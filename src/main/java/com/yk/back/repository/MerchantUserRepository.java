package com.yk.back.repository;

import com.yk.back.entity.MerchantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantUserRepository extends JpaRepository<MerchantUser, UUID> {
    Optional<MerchantUser> findByEmailAndMerchantId(String email, UUID merchantId);
    Optional<MerchantUser> findByEmailAndTenantId(String email, UUID tenantId);
    Optional<MerchantUser> findByEmail(String email);
    boolean existsByEmailAndMerchantId(String email, UUID merchantId);
    List<MerchantUser> findAllByTenantId(UUID tenantId);
    List<MerchantUser> findAllByMerchantIdAndTenantId(UUID merchantId, UUID tenantId);
    Optional<MerchantUser> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantId(UUID tenantId);
    long countByMerchantId(UUID merchantId);
}
