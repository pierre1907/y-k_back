package com.yk.back.repository;

import com.yk.back.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findAllByIsActiveTrue();
    boolean existsByName(String name);
    Optional<Plan> findByName(String name);
}
