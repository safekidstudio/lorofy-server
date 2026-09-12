package com.lorofy.server.features.profile.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String countryCode;
    private String countryName;
    private String timezone;
    private boolean isOnboarded;
    private String avatarUrl;
    private String defaultBlockMode;
    private int rankPoints;
    private int goldCoins;
    private int totalFocusMinutes;
    private int currentStreak;
    private int longestStreak;
    private int streakFreezeCount;
    private boolean canRepairStreak;
    private int repairableStreak;
    private int repairCostCoins;
}
