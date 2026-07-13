package com.lorofy.server.features.leaderboard.dto;

import com.lorofy.server.core.response.PageResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardResponse {
    private PageResponse<LeaderboardItemResponse> leaderboard;
    private LeaderboardItemResponse currentUserRank;
}
