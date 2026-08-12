package com.lorofy.server.features.leaderboard.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.lorofy.server.core.infrastructure.storage.MediaAssetResolver;
import com.lorofy.server.core.response.PageResponse;
import com.lorofy.server.features.focus.repository.FocusSessionRepository;
import com.lorofy.server.features.leaderboard.dto.LeaderboardItemResponse;
import com.lorofy.server.features.leaderboard.dto.LeaderboardResponse;
import com.lorofy.server.features.leaderboard.enums.LeaderboardTimeframe;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {
    private final ProfileRepository profileRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final MediaAssetResolver mediaAssetResolver;
    private final StringRedisTemplate redisTemplate;
    private final RedisLeaderboardHelper redisLeaderboardHelper;

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(UUID userId, LeaderboardTimeframe timeframe,
            String countryCode,
            Pageable pageable) {

        String cleanCountry = StringUtils.hasText(countryCode) ? countryCode.trim().toLowerCase() : null;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 1. Construct Redis key
        String key = switch (timeframe) {
            case ALL -> redisLeaderboardHelper.getAllTimeKey(cleanCountry);
            case TODAY -> redisLeaderboardHelper.getTodayKey(today, cleanCountry);
            case WEEK -> redisLeaderboardHelper.getWeekKey(today, cleanCountry);
            case MONTH -> redisLeaderboardHelper.getMonthKey(today, cleanCountry);
        };

        // 2. Lazy load from DB if key not exists
        try {
            lazyLoadLeaderboard(key, timeframe, cleanCountry);
        } catch (Exception e) {
            log.error("Failed to lazy load leaderboard to Redis for key: {}", key, e);
        }

        // 3. Query from Redis
        PageResponse<LeaderboardItemResponse> leaderboardPage = null;
        boolean redisSuccess = false;

        try {
            long start = pageable.getOffset();
            long end = start + pageable.getPageSize() - 1;

            Set<ZSetOperations.TypedTuple<String>> range = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
            Long totalElements = redisTemplate.opsForZSet().zCard(key);

            if (range != null && totalElements != null && totalElements > 0) {
                List<ZSetOperations.TypedTuple<String>> rangeList = new ArrayList<>(range);
                List<UUID> profileIds = rangeList.stream()
                        .map(tuple -> UUID.fromString(tuple.getValue()))
                        .toList();

                List<Double> scores = rangeList.stream()
                        .map(ZSetOperations.TypedTuple::getScore)
                        .toList();

                if (!profileIds.isEmpty()) {
                    List<Profile> profiles = profileRepository.findAllById(profileIds);
                    Map<UUID, Profile> profileMap = profiles.stream()
                            .collect(Collectors.toMap(Profile::getId, p -> p));

                    int startRank = (int) start + 1;
                    List<LeaderboardItemResponse> list = new ArrayList<>();
                    for (int i = 0; i < profileIds.size(); i++) {
                        UUID id = profileIds.get(i);
                        Profile p = profileMap.get(id);
                        if (p != null) {
                            String avatarUrl = mediaAssetResolver.resolveUrl(p.getAvatarAsset());
                            list.add(LeaderboardItemResponse.builder()
                                    .rank(startRank + i)
                                    .profileId(p.getId())
                                    .username(p.getUsername())
                                    .displayName(p.getDisplayName())
                                    .avatarUrl(avatarUrl)
                                    .points(scores.get(i).intValue())
                                    .build());
                        }
                    }

                    Page<LeaderboardItemResponse> page = new PageImpl<>(list, pageable, totalElements);
                    leaderboardPage = PageResponse.from(page);
                    redisSuccess = true;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch leaderboard from Redis, falling back to Database", e);
        }

        // 4. Fallback to Database query if Redis query failed or is empty
        if (!redisSuccess) {
            leaderboardPage = getLeaderboardFromDb(timeframe, cleanCountry, pageable);
        }

        // 5. Calculate rank of the current user
        LeaderboardItemResponse currentUserRank = null;
        if (userId != null) {
            Profile myProfile = profileRepository.findByUserId(userId).orElse(null);
            if (myProfile != null) {
                int myRank = -1;
                int myPoints = -1;
                boolean rankFromRedisSuccess = false;

                if (redisSuccess) {
                    try {
                        Long rankIdx = redisTemplate.opsForZSet().reverseRank(key, myProfile.getId().toString());
                        Double score = redisTemplate.opsForZSet().score(key, myProfile.getId().toString());
                        if (rankIdx != null && score != null) {
                            myRank = rankIdx.intValue() + 1;
                            myPoints = score.intValue();
                            rankFromRedisSuccess = true;
                        }
                    } catch (Exception e) {
                        log.error("Failed to query user rank from Redis, falling back to Database", e);
                    }
                }

                if (!rankFromRedisSuccess) {
                    // Fallback to database queries for rank & score
                    if (timeframe == LeaderboardTimeframe.ALL) {
                        myPoints = myProfile.getRankPoints();
                        if (cleanCountry != null) {
                            myRank = profileRepository.findRankAllTimeByCountry(myPoints, cleanCountry);
                        } else {
                            myRank = profileRepository.findRankAllTime(myPoints);
                        }
                    } else {
                        LocalDate utdToday = LocalDate.now(ZoneOffset.UTC);
                        OffsetDateTime startUtc;
                        OffsetDateTime endUtc;

                        switch (timeframe) {
                            case TODAY -> {
                                startUtc = utdToday.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                                endUtc = utdToday.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                            }
                            case WEEK -> {
                                LocalDate monday = utdToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                                startUtc = monday.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                                endUtc = monday.plusDays(7).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                            }
                            case MONTH -> {
                                LocalDate firstDay = utdToday.withDayOfMonth(1);
                                startUtc = firstDay.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                                endUtc = firstDay.plusMonths(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                            }
                            default -> throw new IllegalArgumentException("Unsupported timeframe");
                        }

                        long score = focusSessionRepository.sumEarnedPointsByTimeframe(myProfile.getId(), startUtc, endUtc);
                        myPoints = (int) score;
                        myRank = focusSessionRepository.findRankByTimeframe(score, startUtc, endUtc, cleanCountry);
                    }
                }

                String avatarUrl = mediaAssetResolver.resolveUrl(myProfile.getAvatarAsset());
                currentUserRank = LeaderboardItemResponse.builder()
                        .rank(myRank)
                        .profileId(myProfile.getId())
                        .username(myProfile.getUsername())
                        .displayName(myProfile.getDisplayName())
                        .avatarUrl(avatarUrl)
                        .points(myPoints)
                        .build();
            }
        }

        return LeaderboardResponse.builder()
                .leaderboard(leaderboardPage)
                .currentUserRank(currentUserRank)
                .build();
    }

    private PageResponse<LeaderboardItemResponse> getLeaderboardFromDb(LeaderboardTimeframe timeframe,
            String cleanCountry, Pageable pageable) {

        if (timeframe.equals(LeaderboardTimeframe.ALL)) {
            Page<Profile> profilePage;
            if (cleanCountry != null) {
                profilePage = profileRepository.findAllByCountryCodeOrderByRankPointsDesc(cleanCountry, pageable);
            } else {
                profilePage = profileRepository.findAllByOrderByRankPointsDesc(pageable);
            }

            int startRank = (int) pageable.getOffset() + 1;
            List<LeaderboardItemResponse> list = IntStream.range(0, profilePage.getContent().size())
                    .mapToObj(i -> {
                        Profile p = profilePage.getContent().get(i);
                        String avatarUrl = mediaAssetResolver.resolveUrl(p.getAvatarAsset());
                        return LeaderboardItemResponse.builder()
                                .rank(startRank + i)
                                .profileId(p.getId())
                                .username(p.getUsername())
                                .displayName(p.getDisplayName())
                                .avatarUrl(avatarUrl)
                                .points(p.getRankPoints())
                                .build();
                    }).collect(Collectors.toList());

            Page<LeaderboardItemResponse> response = new PageImpl<>(list, pageable, profilePage.getTotalElements());
            return PageResponse.from(response);
        } else {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            OffsetDateTime start;
            OffsetDateTime end;

            switch (timeframe) {
                case TODAY -> {
                    start = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    end = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                }
                case WEEK -> {
                    LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    start = monday.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    end = monday.plusDays(7).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                }
                case MONTH -> {
                    LocalDate firstDay = today.withDayOfMonth(1);
                    start = firstDay.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    end = firstDay.plusMonths(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                }
                default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
            }

            Page<Object[]> queryPage = focusSessionRepository.findLeaderboardData(start, end, cleanCountry, pageable);

            int startRank = (int) pageable.getOffset() + 1;
            List<LeaderboardItemResponse> list = IntStream.range(0, queryPage.getContent().size())
                    .mapToObj(i -> {
                        Object[] row = queryPage.getContent().get(i);
                        Profile p = (Profile) row[0];
                        Long sumPoints = (Long) row[1];
                        String avatarUrl = mediaAssetResolver.resolveUrl(p.getAvatarAsset());

                        return LeaderboardItemResponse.builder()
                                .rank(startRank + i)
                                .profileId(p.getId())
                                .username(p.getUsername())
                                .displayName(p.getDisplayName())
                                .avatarUrl(avatarUrl)
                                .points(sumPoints.intValue())
                                .build();
                    }).collect(Collectors.toList());

            Page<LeaderboardItemResponse> response = new PageImpl<>(list, pageable, queryPage.getTotalElements());
            return PageResponse.from(response);
        }
    }

    private void lazyLoadLeaderboard(String key, LeaderboardTimeframe timeframe, String countryCode) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }

        log.info("Lazy-loading leaderboard from database for key: {}", key);

        // Fetch top 1000 users from DB
        Pageable top1000 = PageRequest.of(0, 1000);
        List<LeaderboardItemResponse> list;

        if (timeframe == LeaderboardTimeframe.ALL) {
            Page<Profile> profilePage;
            if (countryCode != null) {
                profilePage = profileRepository.findAllByCountryCodeOrderByRankPointsDesc(countryCode, top1000);
            } else {
                profilePage = profileRepository.findAllByOrderByRankPointsDesc(top1000);
            }
            list = profilePage.getContent().stream()
                    .map(p -> LeaderboardItemResponse.builder()
                            .profileId(p.getId())
                            .points(p.getRankPoints())
                            .build())
                    .toList();
        } else {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            OffsetDateTime start;
            OffsetDateTime end;

            switch (timeframe) {
                case TODAY -> {
                    start = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    end = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                }
                case WEEK -> {
                    LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    start = monday.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    end = monday.plusDays(7).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                }
                case MONTH -> {
                    LocalDate firstDay = today.withDayOfMonth(1);
                    start = firstDay.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                    end = firstDay.plusMonths(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toOffsetDateTime();
                }
                default -> throw new IllegalArgumentException("Unsupported timeframe");
            }

            Page<Object[]> queryPage = focusSessionRepository.findLeaderboardData(start, end, countryCode, top1000);
            list = queryPage.getContent().stream()
                    .map(row -> {
                        Profile p = (Profile) row[0];
                        Long sumPoints = (Long) row[1];
                        return LeaderboardItemResponse.builder()
                                .profileId(p.getId())
                                .points(sumPoints.intValue())
                                .build();
                    })
                    .toList();
        }

        // Populate Redis ZSET
        if (!list.isEmpty()) {
            Set<ZSetOperations.TypedTuple<String>> tuples = list.stream()
                    .map(item -> ZSetOperations.TypedTuple.of(item.getProfileId().toString(), (double) item.getPoints()))
                    .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(key, tuples);
        }

        // Set TTL of 24 hours
        redisTemplate.expire(key, Duration.ofHours(24));
    }
}
