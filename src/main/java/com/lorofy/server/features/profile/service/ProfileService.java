package com.lorofy.server.features.profile.service;

import com.lorofy.server.features.focus.service.FocusSessionService;
import com.lorofy.server.features.profile.dto.CountryResponse;
import com.lorofy.server.features.profile.dto.OnboardProfileRequest;
import com.lorofy.server.features.profile.dto.ProfileResponse;
import com.lorofy.server.features.profile.dto.UpdateProfileRequest;
import com.lorofy.server.features.profile.entity.Country;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.CountryRepository;
import com.lorofy.server.features.profile.repository.ProfileRepository;
import com.lorofy.server.core.infrastructure.storage.MediaAssetResolver;
import com.lorofy.server.features.media.entity.MediaAsset;
import com.lorofy.server.features.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final CountryRepository countryRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetResolver mediaAssetResolver;
    private final FocusSessionService focusSessionService;

    @Transactional
    public ProfileResponse onboardProfile(UUID userId, OnboardProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

        // 1. Check if country exists
        Country country = countryRepository.findById(request.getCountryCode())
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid country code: " + request.getCountryCode()));
        profile.setCountry(country);

        // 2. Link avatar asset if provided
        if (request.getAvatarAssetId() != null) {
            MediaAsset avatar = mediaAssetRepository.findById(request.getAvatarAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("Avatar asset not found"));
            profile.setAvatarAsset(avatar);
        }

        // 3. Update username if provided
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(profile.getUsername())) {
                if (profileRepository.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException("Username '" + newUsername + "' is already in use");
                }
                profile.setUsername(newUsername);
            }
        }

        // 4. Update displayName if provided (fallback to username)
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName().trim());
        } else if (profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
            profile.setDisplayName(profile.getUsername());
        }

        profile.setTimezone(request.getTimezone());
        profile.setOnboarded(true);

        profile = profileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        if (!profile.isOnboarded()) {
            throw new IllegalArgumentException("Please onboard profile first");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(profile.getUsername())) {
                if (profileRepository.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException("Username '" + newUsername + "' is already in use");
                }
                profile.setUsername(newUsername);
            }
        }

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName().trim());
        }

        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }

        if (request.getAvatarAssetId() != null) {
            MediaAsset avatar = mediaAssetRepository.findById(request.getAvatarAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("Avatar asset not found"));
            profile.setAvatarAsset(avatar);
        }

        profile = profileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional
    public ProfileResponse getProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
        focusSessionService.evaluateAndRepairStreak(profile);
        return mapToResponse(profile);
    }

    @org.springframework.cache.annotation.Cacheable(value = "countries", key = "'all'")
    @Transactional(readOnly = true)
    public List<CountryResponse> getCountries() {
        return countryRepository.findAll().stream()
                .map(country -> {
                    MediaAsset flagAsset = country.getFlagAssetId() != null
                            ? mediaAssetRepository.findById(country.getFlagAssetId()).orElse(null)
                            : null;
                    return CountryResponse.builder()
                            .code(country.getCode())
                            .name(country.getName())
                            .flagAssetId(country.getFlagAssetId())
                            .flagUrl(mediaAssetResolver.resolveUrl(flagAsset))
                            .build();
                })
                .toList();
    }

    private ProfileResponse mapToResponse(Profile profile) {
        String avatarUrl = mediaAssetResolver.resolveUrl(profile.getAvatarAsset());
        boolean canRepair = profile.getPreviousStreak() > 0;
        int repairable = profile.getPreviousStreak();
        int repairCost = 100;

        String displayName = profile.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = profile.getUsername();
        }

        return ProfileResponse.builder()
                .id(profile.getId())
                .username(profile.getUsername())
                .displayName(displayName)
                .countryCode(profile.getCountry() != null ? profile.getCountry().getCode() : null)
                .countryName(profile.getCountry() != null ? profile.getCountry().getName() : null)
                .timezone(profile.getTimezone())
                .isOnboarded(profile.isOnboarded())
                .avatarUrl(avatarUrl)
                .defaultBlockMode(profile.getDefaultBlockMode() != null ? profile.getDefaultBlockMode().name() : "MEDIUM")
                .rankPoints(profile.getRankPoints())
                .goldCoins(profile.getGoldCoins())
                .totalFocusMinutes(profile.getTotalFocusMinutes())
                .currentStreak(profile.getCurrentStreak())
                .longestStreak(profile.getLongestStreak())
                .streakFreezeCount(profile.getStreakFreezeCount())
                .canRepairStreak(canRepair)
                .repairableStreak(repairable)
                .repairCostCoins(repairCost)
                .build();
    }
}
