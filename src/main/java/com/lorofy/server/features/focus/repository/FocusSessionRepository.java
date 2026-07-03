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
                        "AND (:status IS NULL OR f.status = :status) " +
                        "AND (:startDate IS NULL OR f.endedAt >= :startDate) " +
                        "AND (:endDate IS NULL OR f.endedAt <= :endDate) " +
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
}
