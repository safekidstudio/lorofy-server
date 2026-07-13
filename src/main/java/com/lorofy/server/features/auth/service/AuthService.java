package com.lorofy.server.features.auth.service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorofy.server.core.infrastructure.redis.RedisKeyBuilder;
import com.lorofy.server.core.infrastructure.security.JwtTokenProvider;
import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.features.auth.dto.AuthResponse;
import com.lorofy.server.features.auth.dto.LoginRequest;
import com.lorofy.server.features.auth.dto.RefreshTokenRequest;
import com.lorofy.server.features.auth.dto.RegisterRequest;
import com.lorofy.server.features.auth.entity.User;
import com.lorofy.server.features.auth.enums.Role;
import com.lorofy.server.features.auth.repository.UserRepository;
import com.lorofy.server.features.profile.constants.ProfileConstants;
import com.lorofy.server.features.profile.entity.Country;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OtpService otpService;

    @Transactional
    public void register(RegisterRequest request) {
        String email = otpService.getEmailBySignupToken(request.getSignupToken());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered: " + email);
        }

        // Create and save new user (Password will be hashed)
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isEnabled(true)
                .build();

        user = userRepository.save(user);

        String autoUsername = generateUniqueUsername(email);

        // Create and save new profile for the user
        Profile profile = Profile.builder()
                .user(user)
                .username(autoUsername)
                .timezone("Asia/Ho_Chi_Minh")
                .country(Country.builder().code(ProfileConstants.DEFAULT_COUNTRY_CODE).build())
                .isAnonymous(false)
                .rankPoints(0)
                .goldCoins(0)
                .totalFocusMinutes(0)
                .currentStreak(0)
                .longestStreak(0)
                .build();

        profileRepository.save(profile);

        otpService.clearSignupToken(request.getSignupToken());
    }

    public AuthResponse login(LoginRequest request) {

        if (!userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email is not exist: {}", request.getEmail());
            throw new IllegalArgumentException("Email is not exist");
        }

        Authentication authentication;
        try {
            // Call Spring Security verification email and password
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Incorrect password for email: {}", request.getEmail());
            throw new BadCredentialsException("Incorrect password for email");
        }

        // Get user principal verified
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Generate access token and refresh token
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        // Get profile of user verified
        Profile profile = profileRepository.findByUserId(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        // Return response auth
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .username(profile.getUsername())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        // 1. Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is not valid or expired");
        }
        // 2. Extract user ID from refresh token
        UUID userId = jwtTokenProvider.getUserIdFromJWT(refreshToken);
        // 3. Query user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isEnabled()) {
            throw new IllegalStateException("User is disabled");
        }
        // 4. Create new user principal to generate new token
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                user.getRole().name());
        UserPrincipal userPrincipal = new UserPrincipal(user.getId(), user.getEmail(), "",
                java.util.List.of(authority));
        // 5. Generate new access token and refresh token
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);
        // 6. Get profile information
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .username(profile.getUsername())
                .build();
    }

    public void logout(String jwt) {
        try {
            Date expiryDate = jwtTokenProvider.getExpirationDateFromJWT(jwt);
            long diffInMs = expiryDate.getTime() - System.currentTimeMillis();

            if (diffInMs > 0) {
                String blacklistKey = RedisKeyBuilder.getJwtBlacklistKey(jwt);
                redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofMillis(diffInMs));
                log.info("Token blacklisted in Redis. Remaining TTL: {} ms", diffInMs);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist JWT token on logout", e);
            throw new IllegalArgumentException("Token invalid or expired");
        }
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        if (baseUsername.length() < 3) {
            baseUsername = "user_" + baseUsername;
        }

        String username = baseUsername;
        int count = 1;
        while (profileRepository.existsByUsername(username)) {
            username = baseUsername + "_" + (1000 + (int) (Math.random() * 9000));
            count++;
            if (count > 10) {
                username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return username;
    }

    public void sendOtpForRegistration(String email) {
        // 1. Kiểm tra xem email đã tồn tại trong DB chưa
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered: " + email);
        }

        // 2. Nếu chưa tồn tại, tiến hành gửi OTP
        otpService.generateOtpAndSendEmail(email);
    }

}
