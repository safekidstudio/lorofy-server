package com.lorofy.server.features.focus.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lorofy.server.features.focus.constant.SettingKeys;
import com.lorofy.server.features.focus.dto.EndSessionRequest;
import com.lorofy.server.features.focus.dto.FocusSessionResponse;
import com.lorofy.server.features.focus.dto.StartSessionRequest;
import com.lorofy.server.features.focus.entity.Category;
import com.lorofy.server.features.focus.entity.FocusSession;
import com.lorofy.server.features.focus.enums.BlockMode;
import com.lorofy.server.features.focus.enums.SessionStatus;
import com.lorofy.server.features.focus.repository.CategoryRepository;
import com.lorofy.server.features.focus.repository.FocusSessionRepository;
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

    @Transactional
    public FocusSessionResponse startSession(UUID userId, StartSessionRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        }

        FocusSession session = FocusSession.builder()
                .profile(profile)
                .category(category)
                .blockMode(request.getBlockMode())
                .plannedMinutes(request.getPlannedMinutes())
                .status(SessionStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .friendSessionId(request.getFriendSessionId())
                .build();

        session = focusSessionRepository.save(session);

        return mapToResponse(session, 0, 0);
    }

    @Transactional
    public FocusSessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = getVerifiedSession(userId, sessionId);

        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new IllegalStateException("Only pause when session is running");
        }

        session.setPauseCount(session.getPauseCount() + 1);

        session = focusSessionRepository.save(session);

        return mapToResponse(session, 0, 0);

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

        double multiplier = getMultiplierFromSettings(session.getBlockMode());

        int earnedPoints = (int) Math.round(request.getActualMinutes() * basePointsPerMin * multiplier);
        int earnedCoins = (int) Math.round(request.getActualMinutes() * baseCoinsPerMin * multiplier);

        // Update profile
        Profile profile = session.getProfile();
        profile.setRankPoints(profile.getRankPoints() + earnedPoints);
        profile.setGoldCoins(profile.getGoldCoins() + earnedCoins);
        profile.setTotalFocusMinutes(profile.getTotalFocusMinutes() + request.getActualMinutes());

        // Update streak
        updateSreak(profile);
        session = focusSessionRepository.save(session);

        return mapToResponse(session, earnedPoints, earnedCoins);
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

        session = focusSessionRepository.save(session);

        return mapToResponse(session, 0, 0);
    }

    private FocusSession getVerifiedSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Focus session not found"));
        if (!session.getProfile().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to access this focus session");
        }
        return session;
    }

    private void updateSreak(Profile profile) {
        // Get current timezone user
        ZoneId zoneId = ZoneId.of(profile.getTimezone() != null ? profile.getTimezone() : "Asis/Ho_Chi_Minh");
        // Get current date in user timezone
        LocalDate today = LocalDate.now(zoneId);
        // Get last completed session
        Optional<FocusSession> lastSessionOpt = focusSessionRepository.findLastCompletedSession(profile.getId(),
                SessionStatus.COMPLETED);

        // If no last session, set current streak and longest streak to 1
        if (lastSessionOpt.isEmpty()) {
            profile.setCurrentStreak(1);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), 1));
            return;
        }

        // Get last session date in user timezone
        LocalDate lastSessionDate = lastSessionOpt.get().getEndedAt().atZoneSameInstant(zoneId).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(lastSessionDate, today);

        // If consecutive -> Increase streak
        if (daysBetween == 1) {
            int newStreak = profile.getCurrentStreak() + 1;
            profile.setCurrentStreak(newStreak);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), newStreak));
        } else if (daysBetween > 1) {
            // Miss streak -> Reset streak to 1
            profile.setCurrentStreak(1);
        }

    }

    private double getMultiplierFromSettings(BlockMode mode) {
        String key = switch (mode) {
            case LIGHT -> SettingKeys.MULTIPLIER_LIGHT;
            case MEDIUM -> SettingKeys.MULTIPLIER_MEDIUM;
            case STRICT -> SettingKeys.MULTIPLIER_STRICT;
        };
        return settingService.getDoubleSetting(key, 1.0);
    }

    private FocusSessionResponse mapToResponse(FocusSession session, int earnedPoints, int earnedCoins) {
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
                .earnedPoints(earnedPoints)
                .earnedCoins(earnedCoins)
                .build();
    }

}
