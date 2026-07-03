package com.lorofy.server.features.focus.controller;

import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.core.infrastructure.security.PublicEndpoint;
import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.features.focus.dto.CategoryResponse;
import com.lorofy.server.features.focus.dto.CreateCategoryRequest;
import com.lorofy.server.features.focus.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Focus Category Management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PublicEndpoint
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UUID userId = currentUser != null ? currentUser.getId() : null;
        List<CategoryResponse> response = categoryService.getCategories(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Get categories success"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Create category success"));
    }
}
