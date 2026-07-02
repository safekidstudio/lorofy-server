package com.lorofy.server.core.storage.cloudinary;

import com.lorofy.server.core.storage.StorageService;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lorofy.server.features.media.entity.MediaAsset;
import com.lorofy.server.features.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;
    private final MediaAssetRepository mediaAssetRepository;

    @Override
    public MediaAsset upload(MultipartFile file, String folder, UUID uploadedBy) throws IOException {
        // Tự động thêm tiền tố thư mục gốc "lorofy/"
        String targetFolder = "lorofy/" + folder;

        // 1. Thực hiện gọi API của Cloudinary để upload file
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", targetFolder,
                "resource_type", "auto" // Tự động phát hiện ảnh/video/raw file
        ));

        // 2. Trích xuất metadata từ Cloudinary để chuẩn hóa lưu vào DB media_assets
        String publicId = (String) uploadResult.get("public_id");
        String format = (String) uploadResult.get("format");
        Long version = ((Number) uploadResult.get("version")).longValue();
        Integer width = (Integer) uploadResult.get("width");
        Integer height = (Integer) uploadResult.get("height");
        Integer bytes = (Integer) uploadResult.get("bytes");
        String resourceType = (String) uploadResult.get("resource_type");

        MediaAsset asset = MediaAsset.builder()
                .provider(getProviderName()) // Lưu trữ "CLOUDINARY"
                .assetKey(publicId)
                .resourceType(resourceType.toUpperCase()) // IMAGE, VIDEO, RAW
                .format(format)
                .version(version)
                .width(width)
                .height(height)
                .bytes(bytes)
                .collection(targetFolder)
                .uploadedBy(uploadedBy)
                .build();

        return mediaAssetRepository.save(asset);
    }

    @Override
    public void delete(String assetKey) throws IOException {
        cloudinary.uploader().destroy(assetKey, ObjectUtils.emptyMap());
    }

    @Override
    public String getProviderName() {
        return "CLOUDINARY";
    }
}
