package com.lorofy.server.features.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryResponse {
    private String code;
    private String name;
    private UUID flagAssetId;
    private String flagUrl;
}
