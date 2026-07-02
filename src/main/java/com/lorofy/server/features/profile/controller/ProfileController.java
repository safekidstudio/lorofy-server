package com.lorofy.server.features.profile.controller;

import com.lorofy.server.core.security.UserPrincipal;
import com.lorofy.server.features.profile.dto.OnboardProfileRequest;
import com.lorofy.server.features.profile.dto.ProfileResponse;
import com.lorofy.server.features.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping("/onboard")
    public ResponseEntity<ProfileResponse> onboardProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody OnboardProfileRequest request) {
        ProfileResponse response = profileService.onboardProfile(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }
}
