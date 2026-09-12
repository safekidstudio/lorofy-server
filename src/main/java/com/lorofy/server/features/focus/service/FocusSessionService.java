package com.lorofy.server.features.focus.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorofy.server.core.response.PageResponse;
import com.lorofy.server.features.focus.constant.SettingKeys;
import com.lorofy.server.features.focus.dto.EndSessionRequest;
import com.lorofy.server.features.focus.dto.FocusSessionResponse;
import com.lorofy.server.features.focus.dto.FocusStatsResponse;
import com.lorofy.server.features.profile.dto.PointHistoryResponse;
import com.lorofy.server.features.profile.dto.ProfileResponse;
import com.lorofy.server.features.profile.dto.StreakRepairRequest;
import com.lorofy.server.features.focus.dto.StartSessionRequest;
import com.lorofy.server.features.focus.entity.Category;
import com.lorofy.server.features.focus.entity.FocusSession;
import com.lorofy.server.features.focus.enums.BlockMode;
import com.lorofy.server.features.focus.enums.SessionStatus;
import com.lorofy.server.features.focus.repository.CategoryRepository;
import com.lorofy.server.features.focus.repository.FocusSessionRepository;
import java.time.ZoneOffset;
import com.lorofy.server.core.infrastructure.storage.MediaAssetResolver;
import com.lorofy.server.features.leaderboard.service.LeaderboardSseService;
import com.lorofy.server.features.leaderboard.service.LeaderboardSseService.LeaderboardUpdateEvent;
import com.lorofy.server.features.leaderboard.service.RedisLeaderboardHelper;
import com.lorofy.server.features.profile.entity.Profile;
import com.lorofy.server.features.profile.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FocusSessionService {
    private final FocusSessionRepository focusSessionRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;
    private final SettingService settingService;
    private final LeaderboardSseService leaderboardSseService;
    private final MediaAssetResolver mediaAssetResolver;
    private final RedisLeaderboardHelper redisLeaderboardHelper;

    @Transactional
    public FocusSessionResponse startSession(UUID userId, StartSessionRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        }

        double multiplier = getMultiplierFromSettings(request.getBlockMode());

        FocusSession session = FocusSession.builder()
                .profile(profile)
                .category(category)
                .blockMode(request.getBlockMode())
                .plannedMinutes(request.getPlannedMinutes())
                .status(SessionStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .friendSessionId(request.getFriendSessionId())
                .devicePlatform(request.getDevicePlatform() != null ? request.getDevicePlatform() : "IOS")
                .rewardMultiplier(multiplier)
                .build();

        session = focusSessionRepository.save(session);

        return mapToResponse(session);
    }

    @Transactional
    public FocusSessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = getVerifiedSession(userId, sessionId);

        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new IllegalStateException("Only pause when session is running");
        }

        session.setPauseCount(session.getPauseCount() + 1);

        session = focusSessionRepository.save(session);

        return mapToResponse(session);

    }

    @Transactional
    public FocusSessionResponse completeSession(UUID userId, UUID sessionId, EndSessionRequest request) {
        FocusSession session = getVerifiedSession(userId, sessionId);
        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new IllegalStateException("The focus session has already ended");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setActualMinutes(request.getActualMinutes());
        session.setEndedAt(OffsetDateTime.now());

        // Calculate rewards
        int basePointsPerMin = settingService.getIntSetting(SettingKeys.BASE_POINTS_PER_MIN, 1);
        int baseCoinsPerMin = settingService.getIntSetting(SettingKeys.BASE_COINS_PER_MIN, 1);

        double multiplier = session.getRewardMultiplier();

        int earnedPoints = (int) Math.round(request.getActualMinutes() * basePointsPerMin * multiplier);
        int earnedCoins = (int) Math.round(request.getActualMinutes() * baseCoinsPerMin * multiplier);

        session.setEarnedPoints(earnedPoints);
        session.setEarnedCoins(earnedCoins);

        // Update profile
        Profile profile = session.getProfile();
        profile.setRankPoints(profile.getRankPoints() + earnedPoints);
        profile.setGoldCoins(profile.getGoldCoins() + earnedCoins);
        profile.setTotalFocusMinutes(profile.getTotalFocusMinutes() + request.getActualMinutes());

        // Update streak
        updateSreak(profile);
        session = focusSessionRepository.save(session);

        try {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            String countryCode = profile.getCountry() != null ? profile.getCountry().getCode() : null;
            List<String> activeKeys = redisLeaderboardHelper.getActiveKeys(today, countryCode);
            for (String key : activeKeys) {
                redisLeaderboardHelper.incrementScoreIfKeyExists(key, profile.getId(), earnedPoints);
            }
        } catch (Exception e) {
            log.error("Failed to update scores in Redis", e);
        }

        try {
            String avatarUrl = mediaAssetResolver.resolveUrl(profile.getAvatarAsset());
            LeaderboardUpdateEvent event = new LeaderboardUpdateEvent(
                profile.getId(),
                profile.getUsername(),
                profile.getDisplayName(),
                avatarUrl,
                earnedPoints
            );
            leaderboardSseService.broadcastUpdate(event);
        } catch (Exception e) {
            log.error("Failed to broadcast leaderboard update via SSE", e);
        }

        return mapToResponse(session);
    }

    @Transactional
    public FocusSessionResponse failSession(UUID userId, UUID sessionId, EndSessionRequest request) {
        FocusSession session = getVerifiedSession(userId, sessionId);
        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new IllegalArgumentException("The focus session has already ended");
        }

        session.setStatus(SessionStatus.FAILED);
        session.setActualMinutes(request.getActualMinutes());
        session.setEndedAt(OffsetDateTime.now());
        session.setFailureReason(
                request.getFailureReason() != null ? request.getFailureReason() : "User exited session");

        Profile profile = session.getProfile();
        int penaltyPoints = 0;

        long elapsedSeconds = session.getStartedAt() != null
                ? ChronoUnit.SECONDS.between(session.getStartedAt(), session.getEndedAt())
                : request.getActualMinutes() * 60L;

        // Grace period check: apply penalty if elapsed time >= 60s or actualMinutes >= 1
        if (elapsedSeconds >= 60 || request.getActualMinutes() >= 1) {
            String settingKey = session.getBlockMode() == BlockMode.STRICT
                    ? SettingKeys.PENALTY_POINTS_STRICT
                    : SettingKeys.PENALTY_POINTS_MEDIUM;
            int defaultPenalty = session.getBlockMode() == BlockMode.STRICT ? 40 : 20;
            penaltyPoints = settingService.getIntSetting(settingKey, defaultPenalty);
        }

        if (penaltyPoints > 0) {
            session.setEarnedPoints(-penaltyPoints);
            int updatedPoints = Math.max(0, profile.getRankPoints() - penaltyPoints);
            profile.setRankPoints(updatedPoints);
            profileRepository.save(profile);

            try {
                LocalDate today = LocalDate.now(ZoneOffset.UTC);
                String countryCode = profile.getCountry() != null ? profile.getCountry().getCode() : null;
                List<String> activeKeys = redisLeaderboardHelper.getActiveKeys(today, countryCode);
                for (String key : activeKeys) {
                    redisLeaderboardHelper.incrementScoreIfKeyExists(key, profile.getId(), -penaltyPoints);
                }
            } catch (Exception e) {
                log.error("Failed to deduct score in Redis leaderboard", e);
            }
        }

        session = focusSessionRepository.save(session);

        return mapToResponse(session);
    }

    // HISTORY FOCUS SESSION
    @Transactional(readOnly = true)
    public PageResponse<FocusSessionResponse> getHistory(
            UUID userId,
            SessionStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable) {

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        Page<FocusSession> page = focusSessionRepository.findFilteredActivities(
                profile.getId(), status, startDate, endDate, pageable);

        Page<FocusSessionResponse> responsePage = page.map(this::mapToResponse);
        return PageResponse.from(responsePage);
    }

    // FOCUS SESSION STATS
    @Transactional
    public FocusStatsResponse getStats(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        evaluateAndRepairStreak(profile);

        List<FocusSession> allSessions = focusSessionRepository.findAllByProfileId(profile.getId());
        // Lọc danh sách hoàn thành và thất bại
        List<FocusSession> completed = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .collect(Collectors.toList());
        List<FocusSession> failed = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.FAILED)
                .collect(Collectors.toList());
        int totalFocusMins = completed.stream().mapToInt(FocusSession::getActualMinutes).sum();
        // 2.1 Tính toán tỷ lệ phân bổ theo Category
        List<FocusStatsResponse.CategoryStat> categoryStats = completed.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null ? s.getCategory().getId()
                                : UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .values().stream()
                .map(list -> {
                    FocusSession first = list.get(0);
                    String name = first.getCategory() != null ? first.getCategory().getName() : "Others";
                    String color = first.getCategory() != null ? first.getCategory().getColorHex() : "#9E9E9E";
                    int minutes = list.stream().mapToInt(FocusSession::getActualMinutes).sum();
                    return new FocusStatsResponse.CategoryStat(name, color, minutes, list.size());
                })
                .collect(Collectors.toList());
        // 2.2 Tính toán biểu đồ tiến trình 7 ngày gần nhất (Theo múi giờ local của User)
        ZoneId zoneId = ZoneId.of(profile.getTimezone() != null ? profile.getTimezone() : "Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);
        List<LocalDate> last7Days = IntStream.range(0, 7)
                .mapToObj(today::minusDays)
                .sorted()
                .collect(Collectors.toList());
        List<FocusStatsResponse.DailyProgress> weeklyProgress = last7Days.stream()
                .map(date -> {
                    int mins = completed.stream()
                            .filter(s -> s.getEndedAt() != null && s.getEndedAt().atZoneSameInstant(zoneId).toLocalDate().equals(date))
                            .mapToInt(FocusSession::getActualMinutes)
                            .sum();
                    return new FocusStatsResponse.DailyProgress(date.toString(), mins);
                })
                .collect(Collectors.toList());
        return FocusStatsResponse.builder()
                .totalCompletedSessions(completed.size())
                .totalFailedSessions(failed.size())
                .totalFocusMinutes(totalFocusMins)
                .currentStreak(profile.getCurrentStreak())
                .longestStreak(profile.getLongestStreak())
                .categoryBreakdown(categoryStats)
                .weeklyProgress(weeklyProgress)
                .build();
    }

    // POINT HISTORY
    @Transactional(readOnly = true)
    public PageResponse<PointHistoryResponse> getPointHistory(UUID userId, Pageable pageable) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        Page<FocusSession> page = focusSessionRepository.findPointHistory(profile.getId(), pageable);

        Page<PointHistoryResponse> responsePage = page.map(session -> {
            boolean isPositive = session.getEarnedPoints() > 0;
            String type = isPositive ? "REWARD" : "PENALTY";
            String title = isPositive ? "Hoàn thành tập trung" : "Bỏ cuộc giữa chừng";
            String categoryName = session.getCategory() != null ? session.getCategory().getName() : "Khác";
            String description = !isPositive
                    ? (session.getFailureReason() != null ? session.getFailureReason() : "Bấm bỏ cuộc trong phiên tập trung")
                    : ("Tập trung " + session.getActualMinutes() + " phút (" + (session.getBlockMode() != null ? session.getBlockMode().name() : "") + ")");

            return PointHistoryResponse.builder()
                    .id(session.getId())
                    .type(type)
                    .points(session.getEarnedPoints())
                    .title(title)
                    .description(description)
                    .categoryName(categoryName)
                    .blockMode(session.getBlockMode() != null ? session.getBlockMode().name() : null)
                    .timestamp(session.getEndedAt() != null ? session.getEndedAt() : session.getStartedAt())
                    .build();
        });

        return PageResponse.from(responsePage);
    }

    // CALENDAR
    @Transactional(readOnly = true)
    public List<String> getCalendarDates(UUID userId, int year, int month) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        ZoneId zoneId = ZoneId.of(profile.getTimezone() != null ? profile.getTimezone() : "Asia/Ho_Chi_Minh");
        LocalDate startLocalDate = LocalDate.of(year, month, 1);
        LocalDate endLocalDate = startLocalDate.plusMonths(1).minusDays(1);
        OffsetDateTime startUtc = startLocalDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime endUtc = endLocalDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toOffsetDateTime();
        List<FocusSession> sessions = focusSessionRepository.findAllByProfileIdAndStatusAndEndedAtBetween(
                profile.getId(), SessionStatus.COMPLETED, startUtc, endUtc);
        return sessions.stream()
                .map(s -> s.getEndedAt().atZoneSameInstant(zoneId).toLocalDate().toString())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private FocusSession getVerifiedSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Focus session not found"));
        if (!session.getProfile().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to access this focus session");
        }
        return session;
    }

    @Transactional
    public void evaluateAndRepairStreak(Profile profile) {
        ZoneId zoneId = ZoneId.of(profile.getTimezone() != null ? profile.getTimezone() : "Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);

        Optional<FocusSession> lastSessionOpt = focusSessionRepository.findLastCompletedSession(profile.getId(),
                SessionStatus.COMPLETED);

        if (lastSessionOpt.isEmpty()) {
            if (profile.getCurrentStreak() != 0) {
                profile.setCurrentStreak(0);
                profile.setPreviousStreak(0);
                profileRepository.save(profile);
            }
            return;
        }

        LocalDate lastSessionDate = lastSessionOpt.get().getEndedAt().atZoneSameInstant(zoneId).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(lastSessionDate, today);

        if (daysBetween <= 1) {
            // Intact streak
            return;
        }

        // Missed yesterday (daysBetween == 2)
        if (daysBetween == 2) {
            if (profile.getCurrentStreak() > 0) {
                profile.setPreviousStreak(profile.getCurrentStreak());
                profile.setStreakBrokenAt(OffsetDateTime.now());
                profile.setCurrentStreak(0);
                profileRepository.save(profile);
                log.info("Streak broken for profile ID: {}. Can be repaired (previous streak: {})",
                        profile.getId(), profile.getPreviousStreak());
            }
        } else {
            // Missed > 1 day -> repair window expired
            if (profile.getCurrentStreak() != 0 || profile.getPreviousStreak() != 0) {
                profile.setCurrentStreak(0);
                profile.setPreviousStreak(0);
                profile.setStreakBrokenAt(null);
                profileRepository.save(profile);
                log.info("Streak repair window expired for profile ID: {}", profile.getId());
            }
        }
    }

    @Transactional
    public ProfileResponse repairStreak(UUID userId, StreakRepairRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        evaluateAndRepairStreak(profile);

        if (profile.getPreviousStreak() <= 0) {
            throw new IllegalStateException("Không có streak nào có thể khôi phục");
        }

        int costCoins = 100;

        if (request.isUseFreezeItem()) {
            if (profile.getStreakFreezeCount() < 1) {
                throw new IllegalArgumentException("Bạn không có đủ Lá Chắn Đóng Băng trong kho đồ");
            }
            profile.setStreakFreezeCount(profile.getStreakFreezeCount() - 1);
            profile.setLastStreakFreezeUsed(OffsetDateTime.now());
        } else if (request.isUseCoins()) {
            if (profile.getGoldCoins() < costCoins) {
                throw new IllegalArgumentException("Bạn không có đủ Gold Coins (Cần " + costCoins + " xu)");
            }
            profile.setGoldCoins(profile.getGoldCoins() - costCoins);
        } else {
            throw new IllegalArgumentException("Vui lòng chọn phương thức khôi phục (Lá Chắn hoặc Xu)");
        }

        int restoredStreak = profile.getPreviousStreak();
        profile.setCurrentStreak(restoredStreak);
        profile.setLongestStreak(Math.max(profile.getLongestStreak(), restoredStreak));
        profile.setPreviousStreak(0);
        profile.setStreakBrokenAt(null);

        profile = profileRepository.save(profile);

        String avatarUrl = mediaAssetResolver.resolveUrl(profile.getAvatarAsset());
        return ProfileResponse.builder()
                .id(profile.getId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .countryCode(profile.getCountry() != null ? profile.getCountry().getCode() : null)
                .countryName(profile.getCountry() != null ? profile.getCountry().getName() : null)
                .timezone(profile.getTimezone())
                .isOnboarded(profile.isOnboarded())
                .avatarUrl(avatarUrl)
                .defaultBlockMode(profile.getDefaultBlockMode() != null ? profile.getDefaultBlockMode().name() : "MEDIUM")
                .rankPoints(profile.getRankPoints())
                .goldCoins(profile.getGoldCoins())
                .totalFocusMinutes(profile.getTotalFocusMinutes())
                .currentStreak(profile.getCurrentStreak())
                .longestStreak(profile.getLongestStreak())
                .streakFreezeCount(profile.getStreakFreezeCount())
                .canRepairStreak(false)
                .repairableStreak(0)
                .repairCostCoins(100)
                .build();
    }

    private void updateSreak(Profile profile) {
        ZoneId zoneId = ZoneId.of(profile.getTimezone() != null ? profile.getTimezone() : "Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);

        Optional<FocusSession> lastSessionOpt = focusSessionRepository.findLastCompletedSession(profile.getId(),
                SessionStatus.COMPLETED);

        if (lastSessionOpt.isEmpty()) {
            profile.setCurrentStreak(1);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), 1));
            return;
        }

        LocalDate lastSessionDate = lastSessionOpt.get().getEndedAt().atZoneSameInstant(zoneId).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(lastSessionDate, today);

        if (daysBetween == 0) {
            if (profile.getCurrentStreak() == 0) {
                profile.setCurrentStreak(1);
                profile.setLongestStreak(Math.max(profile.getLongestStreak(), 1));
            }
        } else if (daysBetween == 1) {
            int newStreak = profile.getCurrentStreak() + 1;
            profile.setCurrentStreak(newStreak);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), newStreak));
        } else if (daysBetween > 1) {
            profile.setCurrentStreak(1);
            profile.setPreviousStreak(0);
            profile.setStreakBrokenAt(null);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), 1));
        }
    }

    private double getMultiplierFromSettings(BlockMode mode) {
        String key = switch (mode) {
            case MEDIUM -> SettingKeys.MULTIPLIER_MEDIUM;
            case STRICT -> SettingKeys.MULTIPLIER_STRICT;
        };
        return settingService.getDoubleSetting(key, 1.0);
    }

    private FocusSessionResponse mapToResponse(FocusSession session) {
        return FocusSessionResponse.builder()
                .id(session.getId())
                .profileId(session.getProfile().getId())
                .categoryId(session.getCategory() != null ? session.getCategory().getId() : null)
                .categoryName(session.getCategory() != null ? session.getCategory().getName() : "Others")
                .blockMode(session.getBlockMode())
                .plannedMinutes(session.getPlannedMinutes())
                .actualMinutes(session.getActualMinutes())
                .status(session.getStatus())
                .pauseCount(session.getPauseCount())
                .failureReason(session.getFailureReason())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .friendSessionId(session.getFriendSessionId())
                .devicePlatform(session.getDevicePlatform())
                .rewardMultiplier(session.getRewardMultiplier())
                .earnedPoints(session.getEarnedPoints())
                .earnedCoins(session.getEarnedCoins())
                .build();
    }

}
