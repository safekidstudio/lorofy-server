package com.lorofy.server.features.leaderboard.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLeaderboardHelper {

    private final StringRedisTemplate redisTemplate;

    public static final String KEY_PREFIX = "leaderboard:";

    public String getAllTimeKey(String countryCode) {
        if (countryCode != null && !countryCode.isBlank()) {
            return KEY_PREFIX + "all:country:" + countryCode.trim().toLowerCase();
        }
        return KEY_PREFIX + "all";
    }

    public String getTodayKey(LocalDate date, String countryCode) {
        String base = KEY_PREFIX + "today:" + date.toString();
        if (countryCode != null && !countryCode.isBlank()) {
            return base + ":country:" + countryCode.trim().toLowerCase();
        }
        return base;
    }

    public String getWeekKey(LocalDate date, String countryCode) {
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        int weekBasedYear = date.get(WeekFields.ISO.weekBasedYear());
        String base = KEY_PREFIX + "week:" + weekBasedYear + "-W" + String.format("%02d", week);
        if (countryCode != null && !countryCode.isBlank()) {
            return base + ":country:" + countryCode.trim().toLowerCase();
        }
        return base;
    }

    public String getMonthKey(LocalDate date, String countryCode) {
        String monthStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String base = KEY_PREFIX + "month:" + monthStr;
        if (countryCode != null && !countryCode.isBlank()) {
            return base + ":country:" + countryCode.trim().toLowerCase();
        }
        return base;
    }

    public List<String> getActiveKeys(LocalDate date, String countryCode) {
        List<String> keys = new ArrayList<>();
        keys.add(getAllTimeKey(null));
        keys.add(getTodayKey(date, null));
        keys.add(getWeekKey(date, null));
        keys.add(getMonthKey(date, null));

        if (countryCode != null && !countryCode.isBlank()) {
            keys.add(getAllTimeKey(countryCode));
            keys.add(getTodayKey(date, countryCode));
            keys.add(getWeekKey(date, countryCode));
            keys.add(getMonthKey(date, countryCode));
        }
        return keys;
    }

    public void incrementScoreIfKeyExists(String key, UUID profileId, double points) {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForZSet().incrementScore(key, profileId.toString(), points);
            }
        } catch (Exception e) {
            log.error("Failed to increment score in Redis for key: {}", key, e);
        }
    }
}
