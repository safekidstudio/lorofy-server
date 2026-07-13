package com.lorofy.server.features.leaderboard.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardItemResponse {
    private int rank;
    private UUID profileId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private int points;
}
