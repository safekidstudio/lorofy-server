package com.lorofy.server.features.profile.entity;

import com.lorofy.server.features.auth.entity.User;
import com.lorofy.server.features.media.entity.MediaAsset;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 50)
    private String displayName; // Tên hiển thị (khác username)

    @Column(length = 20)
    private String timezone; // "Asia/Ho_Chi_Minh"

    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_asset_id")
    private MediaAsset avatarAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_code", referencedColumnName = "code", nullable = false)
    private Country country;

    @Column(name = "current_rank_id")
    private UUID currentRankId;

    @Column(name = "is_anonymous")
    private boolean isAnonymous = false;

    @Column(name = "rank_points")
    private int rankPoints = 0;

    @Column(name = "gold_coins")
    private int goldCoins = 0;

    @Column(name = "total_focus_minutes")
    private int totalFocusMinutes = 0;

    @Column(name = "current_streak")
    private int currentStreak = 0;

    @Column(name = "longest_streak")
    private int longestStreak = 0;

    @Column(name = "streak_freeze_count")
    private int streakFreezeCount = 0;

    @Column(name = "last_streak_freeze_used")
    private OffsetDateTime lastStreakFreezeUsed;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
