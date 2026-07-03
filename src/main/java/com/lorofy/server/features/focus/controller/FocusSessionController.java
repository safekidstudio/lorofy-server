package com.lorofy.server.features.focus.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorofy.server.core.infrastructure.security.UserPrincipal;
import com.lorofy.server.core.response.ApiResponse;
import com.lorofy.server.features.focus.dto.EndSessionRequest;
import com.lorofy.server.features.focus.dto.FocusSessionResponse;
import com.lorofy.server.features.focus.dto.StartSessionRequest;
import com.lorofy.server.features.focus.entity.FocusSession;
import com.lorofy.server.features.focus.service.FocusSessionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/focus")
@RequiredArgsConstructor
@Tag(name = "Focus", description = "Focus Session Management APIs")
public class FocusSessionController {

    private final FocusSessionService focusSessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<FocusSessionResponse>> startSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Validated @RequestBody StartSessionRequest request) {
        FocusSessionResponse response = focusSessionService.startSession(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Session started successfully"));
    }

    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<ApiResponse<FocusSessionResponse>> pauseSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID sessionId) {
        FocusSessionResponse response = focusSessionService.pauseSession(currentUser.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Session paused successfully"));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<FocusSessionResponse>> completeSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID sessionId,
            @Validated @RequestBody EndSessionRequest request) {
        FocusSessionResponse response = focusSessionService.completeSession(currentUser.getId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Session completed successfully"));
    }

    @PostMapping("/{sessionId}/fail")
    public ResponseEntity<ApiResponse<FocusSessionResponse>> failSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID sessionId,
            @Valid @RequestBody EndSessionRequest request) {
        FocusSessionResponse response = focusSessionService.failSession(currentUser.getId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Session failed successfully"));
    }
}
