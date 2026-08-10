package com.lorofy.server.features.profile.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CountryResponse {
    private String code;
    private String name;
    private UUID flagAssetId;
    private String flagUrl;
}
