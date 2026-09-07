package com.lorofy.server.features.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthLoginRequest {
    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Token is required")
    private String token;

    private String fullName;
}
