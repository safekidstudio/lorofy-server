package com.lorofy.server.features.leaderboard.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private final ProfileRepository profileRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final MediaAssetResolver mediaAssetResolver;

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(UUID userId, LeaderboardTimeframe timeframe,
            String countryCode,
            Pageable pageable) {

        String cleanCountry = StringUtils.hasText(countryCode) ? countryCode.toLowerCase() : null;
        PageResponse<LeaderboardItemResponse> leaderboardPage;

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
                                .rank(startRank + 1)
                                .profileId(p.getId())
                                .username(p.getUsername())
                                .displayName(p.getDisplayName())
                                .avatarUrl(avatarUrl)
                                .points(p.getRankPoints())
                                .build();
                    }).collect(Collectors.toList());

            Page<LeaderboardItemResponse> response = new PageImpl<>(list, pageable, profilePage.getTotalElements());
            leaderboardPage = PageResponse.from(response);
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
                                .rank(startRank + 1)
                                .profileId(p.getId())
                                .username(p.getUsername())
                                .displayName(p.getDisplayName())
                                .avatarUrl(avatarUrl)
                                .points(sumPoints.intValue())
                                .build();
                    }).collect(Collectors.toList());

            Page<LeaderboardItemResponse> response = new PageImpl<>(list, pageable, queryPage.getTotalElements());
            leaderboardPage = PageResponse.from(response);
        }

        // Calculate the rank of the current user
        LeaderboardItemResponse currentUserRank = null;
        if (userId != null) {
            Profile myProfile = profileRepository.findByUserId(userId).orElse(null);
            if (myProfile != null) {
                int myRank;
                int myPoints;

                if (timeframe == LeaderboardTimeframe.ALL) {
                    myPoints = myProfile.getRankPoints();
                    if (cleanCountry != null) {
                        myRank = profileRepository.findRankAllTimeByCountry(myPoints, countryCode);
                    } else {
                        myRank = profileRepository.findRankAllTime(myPoints);
                    }
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

                    long score = focusSessionRepository.sumEarnedPointsByTimeframe(myProfile.getId(), start, end);
                    myPoints = (int) score;
                    myRank = focusSessionRepository.findRankByTimeframe(score, start, end, cleanCountry);

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
}
