package com.lorofy.server.features.focus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FocusStatsResponse {
    private int totalCompletedSessions;
    private int totalFailedSessions;
    private int totalFocusMinutes;
    private int currentStreak;
    private int longestStreak;
    private List<CategoryStat> categoryBreakdown;
    private List<DailyProgress> weeklyProgress;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CategoryStat {
        private String categoryName;
        private String colorHex;
        private int totalMinutes;
        private int sessionCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DailyProgress {
        private String date;
        private int minutes;
    }
}
