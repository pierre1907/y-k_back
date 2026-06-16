package com.yk.back.dto.response;

import com.yk.back.entity.MerchantUser;
import com.yk.back.entity.TenantUser;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String type,
        UUID tenantId,
        String tenantName,
        UUID merchantId,
        String merchantName,
        String fullName,
        String email,
        String role,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static AdminUserResponse fromTenantUser(TenantUser u) {
        return new AdminUserResponse(
                u.getId(), "TENANT_USER",
                u.getTenant().getId(), u.getTenant().getName(),
                null, null,
                u.getFullName(), u.getEmail(), u.getRole(),
                u.isActive(), u.getCreatedAt()
        );
    }

    public static AdminUserResponse fromMerchantUser(MerchantUser u) {
        return new AdminUserResponse(
                u.getId(), "MERCHANT_USER",
                u.getTenant().getId(), u.getTenant().getName(),
                u.getMerchant().getId(), u.getMerchant().getName(),
                u.getFullName(), u.getEmail(), u.getRole(),
                u.isActive(), u.getCreatedAt()
        );
    }
}
