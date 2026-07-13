package com.lorofy.server.features.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorofy.server.core.infrastructure.security.PublicEndpoint;
import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.features.auth.dto.AuthResponse;
import com.lorofy.server.features.auth.dto.LoginRequest;
import com.lorofy.server.features.auth.dto.RefreshTokenRequest;
import com.lorofy.server.features.auth.dto.RegisterRequest;
import com.lorofy.server.features.auth.dto.SendOtpRequest;
import com.lorofy.server.features.auth.dto.VerifyOtpRequest;
import com.lorofy.server.features.auth.service.AuthService;
import com.lorofy.server.features.auth.service.OtpService;
import com.lorofy.server.features.profile.dto.ProfileResponse;
import com.lorofy.server.features.profile.service.ProfileService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication & Profile Management APIs")
public class AuthController {
    private final AuthService authService;
    private final ProfileService profileService;
    private final OtpService otpService;

    @PostMapping("/register/send-otp")
    @PublicEndpoint
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtpForRegistration(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent successfully to " + request.getEmail()));
    }

    @PostMapping("/register/verify-otp")
    @PublicEndpoint
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String signupToken = otpService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success(signupToken, "OTP verified successfully"));
    }

    @PostMapping("/register")
    @PublicEndpoint
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Registered successfully"));
    }

    @PostMapping("/login")
    @PublicEndpoint
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Logged in successfully"));
    }

    @PostMapping("/refresh")
    @PublicEndpoint
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);
            authService.logout(jwt);
            return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid token"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ProfileResponse response = profileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Get profile successfully"));
    }
}
