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
}
