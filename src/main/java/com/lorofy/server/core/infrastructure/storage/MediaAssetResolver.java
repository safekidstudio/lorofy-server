package com.lorofy.server.core.infrastructure.storage;

import com.lorofy.server.features.media.entity.MediaAsset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaAssetResolver {

    @Value("${app.cloudinary.cloud-name}")
    private String cloudName;

    public String resolveUrl(MediaAsset asset) {
        if (asset == null) {
            return null;
        }

        if (asset.getCustomDomain() != null && !asset.getCustomDomain().isBlank()) {
            String domain = asset.getCustomDomain().endsWith("/")
                    ? asset.getCustomDomain()
                    : asset.getCustomDomain() + "/";
            return domain + asset.getAssetKey();
        }

        if ("CLOUDINARY".equalsIgnoreCase(asset.getProvider())) {
            return buildCloudinaryUrl(asset);
        }
        return asset.getAssetKey();
    }

    private String buildCloudinaryUrl(MediaAsset asset) {
        String resourceType = asset.getResourceType() != null
                ? asset.getResourceType().toLowerCase()
                : "image";

        StringBuilder urlBuilder = new StringBuilder("https://res.cloudinary.com/")
                .append(cloudName)
                .append("/")
                .append(resourceType)
                .append("/upload/");

        if (asset.getVersion() != null) {
            urlBuilder.append("v").append(asset.getVersion()).append("/");
        }
        urlBuilder.append(asset.getAssetKey());
        if (asset.getFormat() != null && !asset.getFormat().isBlank()) {
            urlBuilder.append(".").append(asset.getFormat());
        }

        return urlBuilder.toString();
    }
}
