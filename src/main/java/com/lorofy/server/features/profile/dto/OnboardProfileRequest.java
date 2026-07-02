package com.lorofy.server.features.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class OnboardProfileRequest {

    @NotBlank(message = "Display name is required")
    @Size(min = 3, max = 100, message = "Display name must be between 3 and 100 characters")
    private String displayName;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    private String countryCode;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    private UUID avatarAssetId;
}
