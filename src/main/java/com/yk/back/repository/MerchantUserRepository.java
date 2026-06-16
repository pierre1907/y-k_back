package com.yk.back.repository;

import com.yk.back.entity.MerchantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantUserRepository extends JpaRepository<MerchantUser, UUID> {
    Optional<MerchantUser> findByEmailAndMerchantId(String email, UUID merchantId);
    Optional<MerchantUser> findByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndMerchantId(String email, UUID merchantId);
}
