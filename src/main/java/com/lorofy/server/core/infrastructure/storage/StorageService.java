package com.lorofy.server.core.infrastructure.storage;

import com.lorofy.server.features.media.entity.MediaAsset;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public interface StorageService {

    /**
     * Upload file lên kho lưu trữ.
     * 
     * @param file       File dữ liệu tải lên
     * @param folder     Thư mục đích (vd: "avatars", "badges")
     * @param uploadedBy ID của User thực hiện upload
     * @return Đối tượng MediaAsset chứa thông tin lưu vào DB
     */
    MediaAsset upload(MultipartFile file, String folder, UUID uploadedBy) throws IOException;

    /**
     * Xóa file khỏi kho lưu trữ bằng assetKey (ví dụ: public_id của Cloudinary).
     */
    void delete(String assetKey) throws IOException;

    /**
     * Trả về tên của nhà cung cấp (CLOUDINARY, S3, R2...)
     */
    String getProviderName();
}
