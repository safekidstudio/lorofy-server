package com.lorofy.server.features.focus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSettingRequest {
    @NotBlank(message = "Value is required")
    private String value;
}
