package com.lorofy.server.features.profile.service;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final CountryRepository countryRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetResolver mediaAssetResolver;

    @Transactional
    public ProfileResponse onboardProfile(UUID userId, OnboardProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ người dùng không tồn tại"));

        // 1. Kiểm tra quốc gia có tồn tại trong danh mục không
        Country country = countryRepository.findById(request.getCountryCode())
                .orElseThrow(
                        () -> new IllegalArgumentException("Mã quốc gia không hợp lệ: " + request.getCountryCode()));
        profile.setCountry(country);

        // 2. Nếu client gửi lên avatar ID, liên kết nó với Profile
        if (request.getAvatarAssetId() != null) {
            MediaAsset avatar = mediaAssetRepository.findById(request.getAvatarAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("Ảnh đại diện không tồn tại"));
            profile.setAvatarAsset(avatar);
        }

        // 3. Cập nhật các thông tin onboarding khác
        profile.setDisplayName(request.getDisplayName());
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

        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
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

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ người dùng không tồn tại"));
        return mapToResponse(profile);
    }

    private ProfileResponse mapToResponse(Profile profile) {
        String avatarUrl = mediaAssetResolver.resolveUrl(profile.getAvatarAsset());

        return ProfileResponse.builder()
                .id(profile.getId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .countryCode(profile.getCountry().getCode())
                .countryName(profile.getCountry().getName())
                .timezone(profile.getTimezone())
                .isOnboarded(profile.isOnboarded())
                .avatarUrl(avatarUrl)
                .build();
    }
}
