package com.lorofy.server.features.focus.dto;

import com.lorofy.server.features.focus.enums.BlockMode;
import com.lorofy.server.features.focus.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class FocusSessionResponse {
    private UUID id;
    private UUID profileId;
    private UUID categoryId;
    private String categoryName;
    private BlockMode blockMode;
    private int plannedMinutes;
    private int actualMinutes;
    private SessionStatus status;
    private int pauseCount;
    private String failureReason;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private UUID friendSessionId;

    // Gamification rewards
    private int earnedPoints;
    private int earnedCoins;
}
