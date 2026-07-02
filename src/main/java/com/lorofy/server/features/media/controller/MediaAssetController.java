package com.lorofy.server.features.media.controller;

import org.springframework.http.MediaType;
import com.lorofy.server.core.security.UserPrincipal;
import com.lorofy.server.features.media.entity.MediaAsset;
import com.lorofy.server.features.media.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaAssetController {

    private final MediaAssetService mediaAssetService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAsset> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) throws IOException {

        MediaAsset asset = mediaAssetService.uploadAvatar(file, currentUser.getId());
        return ResponseEntity.ok(asset);
    }
}
