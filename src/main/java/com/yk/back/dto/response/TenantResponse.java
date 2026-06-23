package com.yk.back.dto.response;

import com.yk.back.entity.Tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String contactEmail,
        boolean isActive,
        OffsetDateTime archivedAt,
        OffsetDateTime createdAt,
        long merchantCount,
        SubscriptionResponse activeSubscription
) {
    public static TenantResponse from(Tenant t, long merchantCount, SubscriptionResponse sub) {
        return new TenantResponse(
                t.getId(),
                t.getName(),
                t.getSlug(),
                t.getContactEmail(),
                t.isActive(),
                t.getArchivedAt(),
                t.getCreatedAt(),
                merchantCount,
                sub
        );
    }

    public static TenantResponse from(Tenant t) {
        return from(t, 0, null);
    }
}
