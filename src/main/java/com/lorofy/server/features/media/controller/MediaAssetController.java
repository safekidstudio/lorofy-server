package com.lorofy.server.features.media.controller;

import org.springframework.http.MediaType;

import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.features.media.entity.MediaAsset;
import com.lorofy.server.features.media.service.MediaAssetService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media Management APIs")
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
