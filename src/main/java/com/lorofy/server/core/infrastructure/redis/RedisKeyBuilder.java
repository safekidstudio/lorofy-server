package com.lorofy.server.core.infrastructure.redis;

public final class RedisKeyBuilder {

    // Prefix chính của toàn bộ ứng dụng để tránh đụng độ key với các ứng dụng khác
    // trên cùng server Redis
    private static final String APP_PREFIX = "lorofy:";

    // Định nghĩa các Namespaces
    private static final String AUTH_NS = APP_PREFIX + "auth:";
    private static final String PROFILE_NS = APP_PREFIX + "profile:";
    private static final String LEADERBOARD_NS = APP_PREFIX + "leaderboard:";
    private static final String QUEST_NS = APP_PREFIX + "quest:";

    private RedisKeyBuilder() {
        // Chống khởi tạo instance
    }

    /**
     * Sinh key cho token bị thu hồi (Blacklist)
     * Key format: lorofy:auth:blacklist:{jwt}
     */
    public static String getJwtBlacklistKey(String token) {
        return AUTH_NS + "blacklist:" + token;
    }

    /**
     * Sinh key lưu trữ cache Profile của User
     * Key format: lorofy:profile:{userId}
     */
    public static String getProfileCacheKey(String userId) {
        return PROFILE_NS + userId;
    }

    /**
     * Sinh key lưu trữ cache Leaderboard
     * Key format: lorofy:leaderboard:{type} (ví dụ: global, friends, weekly)
     */
    public static String getLeaderboardKey(String type) {
        return LEADERBOARD_NS + type;
    }

    /**
     * Sinh key cho tiến trình nhiệm vụ (Quests) của user
     * Key format: lorofy:quest:{profileId}
     */
    public static String getQuestKey(String profileId) {
        return QUEST_NS + profileId;
    }
}
