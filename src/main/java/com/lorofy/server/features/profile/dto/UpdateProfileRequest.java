package com.lorofy.server.features.profile.dto;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 3, max = 100, message = "Display name must be between 3 and 100 characters")
    private String displayName;
    @Size(min = 3, max = 100, message = "Timezone must be between 3 and 100 characters")
    private String timezone;
    private UUID avatarAssetId;
}
