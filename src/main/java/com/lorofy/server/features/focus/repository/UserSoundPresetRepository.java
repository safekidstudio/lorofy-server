package com.lorofy.server.features.focus.repository;

import com.lorofy.server.features.focus.entity.UserSoundPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserSoundPresetRepository extends JpaRepository<UserSoundPreset, UUID> {
    List<UserSoundPreset> findAllByProfileId(UUID profileId);
}
