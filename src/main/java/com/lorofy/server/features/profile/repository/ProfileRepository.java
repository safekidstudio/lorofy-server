package com.lorofy.server.features.profile.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorofy.server.features.profile.entity.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);

    boolean existsByUsername(String username);
}