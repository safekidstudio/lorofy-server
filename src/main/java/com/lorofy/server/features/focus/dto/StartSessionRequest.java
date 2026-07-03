package com.lorofy.server.features.focus.dto;

import java.util.UUID;

import com.lorofy.server.features.focus.enums.BlockMode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartSessionRequest {
    private UUID categoryId;

    @NotNull(message = "Block mode is required")
    private BlockMode blockMode;

    @Min(value = 1, message = "Minimum planned minutes is 1")
    @Max(value = 180, message = "Maximum planned minutes is 180")
    private int plannedMinutes;

    private UUID friendSessionId;
}
