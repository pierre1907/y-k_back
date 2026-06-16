package com.yk.back.repository;

import com.yk.back.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    List<Merchant> findAllByTenantId(UUID tenantId);
    Optional<Merchant> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
}
