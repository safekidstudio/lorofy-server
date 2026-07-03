package com.lorofy.server.features.media.service;

import com.lorofy.server.core.infrastructure.storage.StorageService;
import com.lorofy.server.features.media.entity.MediaAsset;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAssetService {

    private final StorageService storageService;
    private final ProfileRepository profileRepository;

    @Transactional
    public MediaAsset uploadAvatar(MultipartFile file, UUID userId) throws IOException {
        // Lấy Profile từ User ID ở tầng nghiệp vụ
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ người dùng không tồn tại"));

        // Gọi StorageService để upload file lên Cloudinary
        return storageService.upload(file, "avatars", profile.getId());
    }
}
