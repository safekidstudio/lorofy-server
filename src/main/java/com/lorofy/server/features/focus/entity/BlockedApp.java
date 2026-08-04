package com.lorofy.server.features.focus.entity;

import com.lorofy.server.features.profile.entity.Profile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blocked_apps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedApp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String platform = "IOS";

    @Column(name = "app_identifier", nullable = false, length = 255)
    private String appIdentifier;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(name = "category_group", length = 50)
    private String categoryGroup;

    @Column(name = "is_whitelisted", nullable = false)
    @Builder.Default
    private boolean isWhitelisted = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
