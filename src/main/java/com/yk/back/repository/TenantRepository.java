package com.yk.back.repository;

import com.yk.back.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);
    long countByIsActiveTrue();
    List<Tenant> findAllByArchivedAtIsNull();
    List<Tenant> findAllByArchivedAtIsNotNull();
}
