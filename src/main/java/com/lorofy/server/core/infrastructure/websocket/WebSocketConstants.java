package com.lorofy.server.core.infrastructure.websocket;

public final class WebSocketConstants {
    private WebSocketConstants() {
    }

    // ===================================================================
    // 1. Kênh Nhận Tin Nhắn (Subscribe Topics/Queues)
    // ===================================================================

    // Kênh phòng học nhóm động (Ví dụ client subscribe: /topic/focus/room/123)
    public static final String TOPIC_BUDDY_FOCUS_ROOM = "/topic/focus/room/";

    // Kênh bảng xếp hạng thời gian thực (Ví dụ: /topic/leaderboard)
    public static final String TOPIC_LEADERBOARD = "/topic/leaderboard";

    // Kênh thông báo cá nhân (Ví dụ client subscribe: /user/queue/notifications)
    public static final String QUEUE_PRIVATE_NOTIFICATIONS = "/queue/notifications";

    // ===================================================================
    // 2. Kênh Gửi Tin Nhắn từ Client (Send Destinations)
    // ===================================================================

    // Định tuyến cho hành động trong phòng (Client gửi lên:
    // /app/focus/room/{roomId}/action)
    public static final String SEND_BUDDY_FOCUS_ACTION = "/focus/room/{roomId}/action";

    // Định tuyến cho đồng bộ thời gian (Client gửi lên:
    // /app/focus/room/{roomId}/sync)
    public static final String SEND_BUDDY_FOCUS_SYNC = "/focus/room/{roomId}/sync";
}
