package com.yk.back.dto.response;

import com.yk.back.entity.Plan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String billingCycle,
        Integer maxMerchants,
        Integer maxUsersPerMerchant,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getBillingCycle(),
                plan.getMaxMerchants(),
                plan.getMaxUsersPerMerchant(),
                plan.isActive(),
                plan.getCreatedAt()
        );
    }
}
