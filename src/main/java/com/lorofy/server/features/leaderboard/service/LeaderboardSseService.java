package com.lorofy.server.features.leaderboard.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardSseService implements MessageListener {

    private static final String TOPIC = "lorofy:leaderboard:updates";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public record LeaderboardUpdateEvent(
        UUID profileId,
        String username,
        String displayName,
        String avatarUrl,
        int earnedPoints
    ) {}

    @PostConstruct
    public void init() {
        redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(TOPIC));
        log.info("Subscribed LeaderboardSseService to Redis topic: {}", TOPIC);
    }

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
        try {
            // Publish event to Redis Pub/Sub channel for multi-node distribution
            redisTemplate.convertAndSend(TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to publish LeaderboardUpdateEvent to Redis topic", e);
            // Fallback: send directly to local emitters
            sendToLocalEmitters(event);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            LeaderboardUpdateEvent event = objectMapper.readValue(message.getBody(), LeaderboardUpdateEvent.class);
            sendToLocalEmitters(event);
        } catch (Exception e) {
            log.error("Failed to deserialize Redis Pub/Sub LeaderboardUpdateEvent", e);
        }
    }

    private void sendToLocalEmitters(LeaderboardUpdateEvent event) {
        if (emitters.isEmpty()) {
            return;
        }
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("leaderboard-update").data(event));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
        }
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
