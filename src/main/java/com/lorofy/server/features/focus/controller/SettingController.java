package com.lorofy.server.features.focus.controller;

import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.core.infrastructure.security.PublicEndpoint;
import com.lorofy.server.features.focus.dto.UpdateSettingRequest;
import com.lorofy.server.features.focus.service.SettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Tag(name = "Setting", description = "System Settings Management APIs")
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    @PublicEndpoint
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllSettings() {
        Map<String, String> settings = settingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings, "Get All Settings Success"));
    }

    @GetMapping("/{key}")
    @PublicEndpoint
    public ResponseEntity<ApiResponse<String>> getSetting(
            @PathVariable String key,
            @RequestParam(defaultValue = "") String defaultValue) {
        String value = settingService.getSetting(key, defaultValue);
        return ResponseEntity.ok(ApiResponse.success(value, "Get Setting Success"));
    }

    @PutMapping("/{key}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request) {
        settingService.updateSetting(key, request.getValue());
        return ResponseEntity.ok(ApiResponse.success(null, "Update Setting Success"));
    }
}
