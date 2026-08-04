package com.lorofy.server.features.subscription.repository;

import com.lorofy.server.features.subscription.entity.Subscription;
import com.lorofy.server.features.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByProfileId(UUID profileId);
    List<Subscription> findAllByProfileIdAndStatus(UUID profileId, SubscriptionStatus status);
}
