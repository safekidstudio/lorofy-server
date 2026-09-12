package com.lorofy.server.features.profile.controller;

import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.core.response.PageResponse;
import com.lorofy.server.features.focus.dto.FocusSessionResponse;
import com.lorofy.server.features.focus.dto.FocusStatsResponse;
import com.lorofy.server.features.focus.enums.SessionStatus;
import com.lorofy.server.features.focus.service.FocusSessionService;
import com.lorofy.server.features.profile.dto.CountryResponse;
import com.lorofy.server.features.profile.dto.OnboardProfileRequest;
import com.lorofy.server.features.profile.dto.PointHistoryResponse;
import com.lorofy.server.features.profile.dto.ProfileResponse;
import com.lorofy.server.features.profile.dto.StreakRepairRequest;
import com.lorofy.server.features.profile.dto.UpdateProfileRequest;
import com.lorofy.server.features.profile.service.ProfileService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profile Management APIs")
public class ProfileController {

    private final ProfileService profileService;
    private final FocusSessionService focusSessionService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ProfileResponse response = profileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Get profile success"));
    }

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

    @PostMapping("/streak/repair")
    public ResponseEntity<ApiResponse<ProfileResponse>> repairStreak(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody StreakRepairRequest request) {
        ProfileResponse response = focusSessionService.repairStreak(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Streak repaired successfully"));
    }

    @GetMapping("/points/history")
    public ResponseEntity<ApiResponse<PageResponse<PointHistoryResponse>>> getPointHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable) {
        PageResponse<PointHistoryResponse> response = focusSessionService.getPointHistory(
                currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Get point history success"));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<PageResponse<FocusSessionResponse>>> getActivities(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) OffsetDateTime startDate,
            @RequestParam(required = false) OffsetDateTime endDate,
            Pageable pageable) {

        PageResponse<FocusSessionResponse> response = focusSessionService.getHistory(
                currentUser.getId(), status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Get activities success"));
    }

    @GetMapping("/focus-stats")
    public ResponseEntity<ApiResponse<FocusStatsResponse>> getFocusStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        FocusStatsResponse response = focusSessionService.getStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Get focus stats success"));
    }

    @GetMapping("/focus-calendar")
    public ResponseEntity<ApiResponse<List<String>>> getFocusCalendar(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam int year,
            @RequestParam int month) {
        List<String> response = focusSessionService.getCalendarDates(currentUser.getId(), year, month);
        return ResponseEntity.ok(ApiResponse.success(response, "Get focus calendar success"));
    }

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getCountries() {
        List<CountryResponse> response = profileService.getCountries();
        return ResponseEntity.ok(ApiResponse.success(response, "Get countries success"));
    }

}
