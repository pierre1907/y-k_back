package com.yk.back.dto.response;

import com.yk.back.entity.Subscription;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID tenantId,
        PlanResponse plan,
        String status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime trialEndsAt,
        OffsetDateTime createdAt
) {
    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getTenant().getId(),
                PlanResponse.from(s.getPlan()),
                s.getStatus().name(),
                s.getStartsAt(),
                s.getEndsAt(),
                s.getTrialEndsAt(),
                s.getCreatedAt()
        );
    }
}
