package com.lorofy.server.features.profile.controller;

import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.features.profile.dto.OnboardProfileRequest;
import com.lorofy.server.features.profile.dto.ProfileResponse;
import com.lorofy.server.features.profile.dto.UpdateProfileRequest;
import com.lorofy.server.features.profile.service.ProfileService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profile Management APIs")
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping("/onboard")
    public ResponseEntity<ApiResponse<ProfileResponse>> onboardProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody OnboardProfileRequest request) {
        ProfileResponse response = profileService.onboardProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile onboarded successfully"));
    }

    @PatchMapping("/update")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = profileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }
}
