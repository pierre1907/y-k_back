package com.yk.back.repository;

import com.yk.back.entity.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmailAndTenantId(String email, UUID tenantId);
    Optional<TenantUser> findByEmail(String email);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
