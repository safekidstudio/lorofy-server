package com.lorofy.server.features.focus.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorofy.server.features.focus.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("SELECT c FROM Category c WHERE c.profile.id = :profileId OR c.profile IS NULL")
    List<Category> findAllByProfileIdOrSystem(@Param("profileId") UUID profileId);

    List<Category> findAllByProfileIsNull();
}
