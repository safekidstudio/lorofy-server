package com.lorofy.server.features.focus.repository;

import com.lorofy.server.features.focus.entity.BlockedApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedAppRepository extends JpaRepository<BlockedApp, UUID> {
    List<BlockedApp> findAllByProfileId(UUID profileId);
    List<BlockedApp> findAllByProfileIdAndPlatform(UUID profileId, String platform);
}
