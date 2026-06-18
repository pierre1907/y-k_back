package com.yk.back.repository;

import com.yk.back.entity.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    @EntityGraph(attributePaths = "plan")
    Optional<Subscription> findFirstByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Subscription> findAllByTenantId(UUID tenantId);
    List<Subscription> findAllByStatus(Subscription.Status status);
    long countByStatus(Subscription.Status status);
}
