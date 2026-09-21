package com.lorofy.server.features.leaderboard.scheduler;

import java.util.List;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lorofy.server.features.leaderboard.service.RedisLeaderboardHelper;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardSyncScheduler {

    private final ProfileRepository profileRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisLeaderboardHelper redisLeaderboardHelper;

    /**
     * Scheduled job to re-sync all-time rank points from PostgreSQL DB to Redis ZSET.
     * Runs daily at 2:00 AM (0 0 2 * * ?) to fix potential Data Drift or expired Redis keys.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncAllTimeLeaderboard() {
        log.info("Starting daily Leaderboard DB-to-Redis re-sync job...");
        try {
            String allTimeKey = redisLeaderboardHelper.getAllTimeKey(null);

            // Fetch top 1000 profiles from Database
            List<Profile> topProfiles = profileRepository
                    .findAllByRankPointsGreaterThanOrderByRankPointsDesc(0, PageRequest.of(0, 1000))
                    .getContent();

            for (Profile profile : topProfiles) {
                redisTemplate.opsForZSet().add(allTimeKey, profile.getId().toString(), profile.getRankPoints());
            }
            log.info("Successfully re-synced {} top profiles to Redis leaderboard key: {}", topProfiles.size(), allTimeKey);
        } catch (Exception e) {
            log.error("Failed to execute Leaderboard DB-to-Redis sync job", e);
        }
    }
}
