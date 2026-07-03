package com.lorofy.server.features.focus.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorofy.server.features.focus.entity.FocusSession;
import com.lorofy.server.features.focus.enums.SessionStatus;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    @Query("SELECT f FROM FocusSession f WHERE f.profile.id = :profileId AND f.status = :status ORDER BY f.endedAt DESC LIMIT 1")
    Optional<FocusSession> findLastCompletedSession(
            @Param("profileId") UUID profileId,
            @Param("status") SessionStatus status);
}
