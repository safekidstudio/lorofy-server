package com.lorofy.server.features.focus.service;

import com.lorofy.server.features.focus.dto.CategoryResponse;
import com.lorofy.server.features.focus.dto.CreateCategoryRequest;
import com.lorofy.server.features.focus.entity.Category;
import com.lorofy.server.features.focus.repository.CategoryRepository;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID userId) {
        List<Category> categories;

        if (userId != null) {
            Profile profile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
            categories = categoryRepository.findAllByProfileIdOrSystem(profile.getId());
        } else {
            categories = categoryRepository.findAllByProfileIsNull();
        }

        // Map to Response
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(UUID userId, CreateCategoryRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        // Create category
        Category category = Category.builder()
                .profile(profile)
                .name(request.getName())
                .iconName(request.getIconName())
                .colorHex(request.getColorHex())
                .build();
        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .iconName(category.getIconName())
                .colorHex(category.getColorHex())
                .isSystem(category.getProfile() == null) // If profile_id is null, it is a system category
                .build();
    }
}
