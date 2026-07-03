package com.lorofy.server.features.focus.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class EndSessionRequest {
    @Min(value = 0, message = "Actual minutes cannot be negative")
    private int actualMinutes;

    private String failureReason;
}
