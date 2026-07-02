package com.lorofy.server.features.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorofy.server.core.security.JwtTokenProvider;
import com.lorofy.server.core.security.UserPrincipal;
import com.lorofy.server.features.auth.dto.AuthResponse;
import com.lorofy.server.features.auth.dto.LoginRequest;
import com.lorofy.server.features.auth.dto.RegisterRequest;
import com.lorofy.server.features.auth.entity.User;
import com.lorofy.server.features.auth.enums.Role;
import com.lorofy.server.features.auth.repository.UserRepository;
import com.lorofy.server.features.profile.constants.ProfileConstants;
import com.lorofy.server.features.profile.entity.Country;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        if (profileRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken: " + request.getUsername());
        }

        // Create and save new user (Password will be hashed)
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isEnabled(true)
                .build();

        user = userRepository.save(user);

        // Create and save new profile for the user
        Profile profile = Profile.builder()
                .user(user)
                .username(request.getUsername())
                .country(Country.builder().code(ProfileConstants.DEFAULT_COUNTRY_CODE).build())
                .isAnonymous(false)
                .rankPoints(0)
                .goldCoins(0)
                .totalFocusMinutes(0)
                .currentStreak(0)
                .longestStreak(0)
                .build();

        profileRepository.save(profile);
    }

    public AuthResponse login(LoginRequest request) {
        // Call Spring Security verification email and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

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
}
