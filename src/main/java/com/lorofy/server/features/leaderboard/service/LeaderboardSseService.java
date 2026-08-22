package com.lorofy.server.features.leaderboard.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LeaderboardSseService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public record LeaderboardUpdateEvent(
        UUID profileId,
        String username,
        String displayName,
        String avatarUrl,
        int earnedPoints
    ) {}

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("handshake").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcastUpdate(LeaderboardUpdateEvent event) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("leaderboard-update").data(event));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    @Scheduled(fixedRate = 20000) // every 20 seconds
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        log.debug("Sending heartbeat ping to {} SSE emitters", emitters.size());
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Removed {} dead SSE emitters", deadEmitters.size());
        }
    }
}
