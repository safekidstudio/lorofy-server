package com.lorofy.server.features.leaderboard.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lorofy.server.core.infrastructure.security.PublicEndpoint;
import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.core.response.PageResponse;
import com.lorofy.server.features.leaderboard.dto.LeaderboardItemResponse;
import com.lorofy.server.features.leaderboard.dto.LeaderboardResponse;
import com.lorofy.server.features.leaderboard.enums.LeaderboardTimeframe;
import com.lorofy.server.features.leaderboard.service.LeaderboardService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leaderboards")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Global and Regional Ranking APIs")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @GetMapping
    @PublicEndpoint
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "ALL") LeaderboardTimeframe timeframe,
            @RequestParam(required = false) String countryCode,
            Pageable pageable) {
        UUID userId = currentUser != null ? currentUser.getId() : null;
        LeaderboardResponse response = leaderboardService.getLeaderboard(userId, timeframe,
                countryCode,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Get leaderboard successfully."));
    }
}
