package com.lorofy.server.features.profile.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponse {
    private UUID id;
    private String type; // "REWARD" or "PENALTY"
    private int points; // e.g. +30 or -20
    private String title; // "Hoàn thành tập trung" or "Bỏ cuộc giữa chừng"
    private String description;
    private String categoryName;
    private String blockMode;
    private OffsetDateTime timestamp;
}
