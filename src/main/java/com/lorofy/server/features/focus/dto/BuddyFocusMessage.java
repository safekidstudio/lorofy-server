package com.lorofy.server.features.focus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyFocusMessage {

    // Loại hành động của thành viên trong phòng
    public enum ActionType {
        JOIN, // Tham gia phòng
        START, // Bắt đầu đếm ngược tập trung
        PAUSE, // Tạm dừng
        RESUME, // Tiếp tục tập trung
        SYNC, // Đồng bộ thời gian còn lại (seconds) giữa các máy
        LEAVE // Rời phòng
    }

    private ActionType action;
    private UUID roomId;
    private UUID userId;
    private String userEmail;
    private String userAvatar;
    private Long remainingSeconds; // Số giây còn lại (chỉ dùng cho SYNC/START)
    private String message; // Ghi chú hoặc tin nhắn chat nhanh trong phòng
}
