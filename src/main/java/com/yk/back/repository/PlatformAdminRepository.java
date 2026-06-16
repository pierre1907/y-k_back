package com.yk.back.repository;

import com.yk.back.entity.PlatformAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, UUID> {
    Optional<PlatformAdmin> findByEmail(String email);
    boolean existsByEmail(String email);
}
