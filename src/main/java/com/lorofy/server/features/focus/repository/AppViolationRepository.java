package com.lorofy.server.features.focus.repository;

import com.lorofy.server.features.focus.entity.AppViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppViolationRepository extends JpaRepository<AppViolation, UUID> {
    List<AppViolation> findAllByFocusSessionId(UUID focusSessionId);
}
