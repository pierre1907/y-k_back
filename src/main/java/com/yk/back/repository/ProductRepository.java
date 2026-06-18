package com.yk.back.repository;

import com.yk.back.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByTenantIdOrderByNameAsc(UUID tenantId);
    List<Product> findAllByMerchantIdAndTenantIdOrderByNameAsc(UUID merchantId, UUID tenantId);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsBySkuAndTenantId(String sku, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
