package com.lorofy.server.features.profile.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorofy.server.features.profile.entity.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);

    boolean existsByUsername(String username);

    // Get leaderboard all time without country code
    Page<Profile> findAllByOrderByRankPointsDesc(Pageable pageable);

    // Get leaderboard all time with country code
    Page<Profile> findAllByCountryCodeOrderByRankPointsDesc(String countryCode, Pageable pageable);

    // Get rank of profile all time without country code
    @Query("SELECT COUNT(p) + 1 FROM Profile p WHERE p.rankPoints > :points")
    int findRankAllTime(@Param("points") int points);

    // Get rank of profile all time with country code
    @Query("SELECT COUNT(p) + 1 FROM Profile p WHERE p.rankPoints > :points AND p.country.code = :countryCode")
    int findRankAllTimeByCountry(@Param("points") int points, @Param("countryCode") String countryCode);
}