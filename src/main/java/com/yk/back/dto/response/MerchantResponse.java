package com.yk.back.dto.response;

import com.yk.back.entity.Merchant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        UUID tenantId,
        String tenantName,
        String name,
        String slug,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static MerchantResponse from(Merchant m) {
        return new MerchantResponse(
                m.getId(),
                m.getTenant().getId(),
                m.getTenant().getName(),
                m.getName(),
                m.getSlug(),
                m.isActive(),
                m.getCreatedAt()
        );
    }
}
