package com.yk.back.repository;

import com.yk.back.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Order> findAllByMerchantIdAndTenantIdOrderByCreatedAtDesc(UUID merchantId, UUID tenantId);
    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByReferenceAndTenantId(String reference, UUID tenantId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, Order.Status status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.tenant.id = :tenantId AND o.status = 'DELIVERED'")
    BigDecimal sumDeliveredByTenant(UUID tenantId);
}
