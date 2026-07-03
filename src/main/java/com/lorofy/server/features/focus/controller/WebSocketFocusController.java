package com.lorofy.server.features.focus.controller;

import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.features.focus.dto.BuddyFocusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketFocusController {

    private final SimpMessagingTemplate messagingTemplate;

    // API nhận hành động phòng (JOIN, START, PAUSE, RESUME, LEAVE) từ Client và
    // phát lại cho cả phòng
    @MessageMapping("/focus/room/{roomId}/action")
    public void handleRoomAction(
            @DestinationVariable UUID roomId,
            @Payload BuddyFocusMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        // Lấy thông tin User đã được bảo mật giải mã sẵn trong WebSocket Session
        UserPrincipal user = (UserPrincipal) ((UsernamePasswordAuthenticationToken) headerAccessor.getUser())
                .getPrincipal();

        // Ép dữ liệu phía Server (tránh Client gửi sai lệch userId hoặc userEmail)
        message.setUserId(user.getId());
        message.setUserEmail(user.getEmail());
        message.setRoomId(roomId);

        // Kênh phát tin broadcast (Ví dụ: /topic/focus/room/123)
        String destination = "/topic/focus/room/" + roomId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("BuddyFocus Room [{}]: User {} performed Action [{}]", roomId, user.getEmail(), message.getAction());
    }

    // API nhận dữ liệu đồng bộ thời gian (SYNC) liên tục từ client điều phối và
    // phát lại
    @MessageMapping("/focus/room/{roomId}/sync")
    public void handleRoomSync(
            @DestinationVariable UUID roomId,
            @Payload BuddyFocusMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        UserPrincipal user = (UserPrincipal) ((UsernamePasswordAuthenticationToken) headerAccessor.getUser())
                .getPrincipal();

        message.setUserId(user.getId());
        message.setUserEmail(user.getEmail());
        message.setRoomId(roomId);

        String destination = "/topic/focus/room/" + roomId;
        messagingTemplate.convertAndSend(destination, message);
    }
}
