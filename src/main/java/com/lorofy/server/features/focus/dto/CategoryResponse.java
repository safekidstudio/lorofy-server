package com.lorofy.server.features.focus.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String iconName;
    private String colorHex;
    private boolean isSystem;
}
