package com.lorofy.server.features.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import com.lorofy.server.features.media.entity.MediaAsset;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

}
