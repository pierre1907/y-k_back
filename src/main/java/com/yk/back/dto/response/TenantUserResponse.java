package com.yk.back.dto.response;

import com.yk.back.entity.TenantUser;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantUserResponse(
        UUID id,
        UUID tenantId,
        String fullName,
        String email,
        String role,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static TenantUserResponse from(TenantUser u) {
        return new TenantUserResponse(
                u.getId(),
                u.getTenant().getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.getCreatedAt()
        );
    }
}
