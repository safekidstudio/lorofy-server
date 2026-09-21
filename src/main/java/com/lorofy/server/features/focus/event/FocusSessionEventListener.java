package com.lorofy.server.features.focus.event;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.lorofy.server.features.leaderboard.service.LeaderboardSseService;
import com.lorofy.server.features.leaderboard.service.LeaderboardSseService.LeaderboardUpdateEvent;
import com.lorofy.server.features.leaderboard.service.RedisLeaderboardHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FocusSessionEventListener {

    private final RedisLeaderboardHelper redisLeaderboardHelper;
    private final LeaderboardSseService leaderboardSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFocusSessionCompleted(FocusSessionCompletedEvent event) {
        log.info("Handling FocusSessionCompletedEvent AFTER_COMMIT for profileId: {}", event.profileId());

        if (event.earnedPoints() != 0) {
            try {
                LocalDate today = LocalDate.now(ZoneOffset.UTC);
                List<String> activeKeys = redisLeaderboardHelper.getActiveKeys(today, event.countryCode());
                for (String key : activeKeys) {
                    redisLeaderboardHelper.incrementScoreIfKeyExists(key, event.profileId(), event.earnedPoints());
                }
            } catch (Exception e) {
                log.error("Failed to update scores in Redis after transaction commit", e);
            }
        }

        try {
            LeaderboardUpdateEvent updateEvent = new LeaderboardUpdateEvent(
                event.profileId(),
                event.username(),
                event.displayName(),
                event.avatarUrl(),
                event.earnedPoints()
            );
            leaderboardSseService.broadcastUpdate(updateEvent);
        } catch (Exception e) {
            log.error("Failed to broadcast leaderboard update via SSE after transaction commit", e);
        }
    }
}
