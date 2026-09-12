package com.lorofy.server.features.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakRepairRequest {
    @Builder.Default
    private boolean useFreezeItem = true;

    @Builder.Default
    private boolean useCoins = false;
}
