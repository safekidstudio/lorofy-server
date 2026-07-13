package com.lorofy.server.features.focus.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorofy.server.features.focus.entity.FocusSession;
import com.lorofy.server.features.focus.enums.SessionStatus;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

        // Get last completed session
        @Query("SELECT f FROM FocusSession f WHERE f.profile.id = :profileId AND f.status = :status ORDER BY f.endedAt DESC LIMIT 1")
        Optional<FocusSession> findLastCompletedSession(
                        @Param("profileId") UUID profileId,
                        @Param("status") SessionStatus status);

        // Get histories session with params
        @Query("SELECT f FROM FocusSession f WHERE f.profile.id = :profileId " +
                        "AND (cast(:status as String) IS NULL OR f.status = :status) " +
                        "AND (cast(:startDate as timestamp) IS NULL OR f.endedAt >= :startDate) " +
                        "AND (cast(:endDate as timestamp) IS NULL OR f.endedAt <= :endDate) " +
                        "ORDER BY f.createdAt DESC")
        Page<FocusSession> findFilteredActivities(
                        @Param("profileId") UUID profileId,
                        @Param("status") SessionStatus status,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate,
                        Pageable pageable);

        // Get all sessions of user
        List<FocusSession> findAllByProfileId(UUID profileId);

        // Get all sessions of user in specific date range
        List<FocusSession> findAllByProfileIdAndStatusAndEndedAtBetween(
                        UUID profileId,
                        SessionStatus status,
                        OffsetDateTime start,
                        OffsetDateTime end);

        // Get leaderboard data with params
        @Query("SELECT fs.profile, SUM(fs.earnedPoints) as totalPoints " +
                        "FROM FocusSession fs " +
                        "WHERE fs.status = 'COMPLETED' " +
                        "AND fs.endedAt BETWEEN :start AND :end " +
                        "AND (cast(:countryCode as String) IS NULL OR fs.profile.country.code = :countryCode) " +
                        "GROUP BY fs.profile " +
                        "ORDER BY totalPoints DESC")
        Page<Object[]> findLeaderboardData(
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        @Param("countryCode") String countryCode,
                        Pageable pageable);

        // Get points of profile in specific date range
        @Query("SELECT COALESCE(SUM(fs.earnedPoints), 0) FROM FocusSession fs " +
                        "WHERE fs.profile.id = :profileId AND fs.status = 'COMPLETED' AND fs.endedAt BETWEEN :start AND :end")
        long sumEarnedPointsByTimeframe(
                        @Param("profileId") UUID profileId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end);

        // Get rank of profile in specific date range
        @Query("SELECT COUNT(p) + 1 FROM Profile p WHERE " +
                        "(SELECT COALESCE(SUM(fs.earnedPoints), 0) FROM FocusSession fs WHERE fs.profile.id = p.id AND fs.status = 'COMPLETED' AND fs.endedAt BETWEEN :start AND :end) > :userScore "
                        +
                        "AND (cast(:countryCode as String) IS NULL OR p.country.code = :countryCode)")
        int findRankByTimeframe(
                        @Param("userScore") long userScore,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        @Param("countryCode") String countryCode);

}
