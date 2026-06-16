package com.yk.back.dto.response;

import com.yk.back.entity.MerchantUser;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MerchantUserResponse(
        UUID id,
        UUID tenantId,
        UUID merchantId,
        String merchantName,
        String fullName,
        String email,
        String role,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static MerchantUserResponse from(MerchantUser u) {
        return new MerchantUserResponse(
                u.getId(),
                u.getTenant().getId(),
                u.getMerchant().getId(),
                u.getMerchant().getName(),
                u.getFullName(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.getCreatedAt()
        );
    }
}
