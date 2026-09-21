package com.lorofy.server.features.profile.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorofy.server.core.infrastructure.security.PublicEndpoint;
import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.features.profile.dto.CountryResponse;
import com.lorofy.server.features.profile.service.ProfileService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
@Tag(name = "Country", description = "Country Reference APIs")
public class CountryController {

    private final ProfileService profileService;

    @PublicEndpoint
    @GetMapping
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getCountries() {
        List<CountryResponse> response = profileService.getCountries();
        return ResponseEntity.ok(ApiResponse.success(response, "Get countries success"));
    }
}
