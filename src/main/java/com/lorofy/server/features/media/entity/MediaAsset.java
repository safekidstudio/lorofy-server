package com.lorofy.server.features.media.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.lorofy.server.features.auth.entity.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String provider = "CLOUDINARY";

    @Column(name = "asset_key", nullable = false, length = 500)
    private String assetKey;

    @Column(name = "resource_type", nullable = false, length = 10)
    private String resourceType = "IMAGE";

    @Column(length = 10)
    private String format;
    private Long version;
    private Integer width;
    private Integer height;
    private Integer bytes;
    private String collection;

    @Column(name = "custom_domain")
    private String customDomain;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "uploaded_by", updatable = false)
    private UUID uploadedBy;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", insertable = false, updatable = false)
    private User user;

}
