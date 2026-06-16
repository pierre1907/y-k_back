package com.yk.back.dto.response;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        UUID tenantId,
        UUID merchantId,
        String role
) {
    public static LoginResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UUID userId,
            String email,
            UUID tenantId,
            UUID merchantId,
            String role
    ) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn,
                userId, email, tenantId, merchantId, role);
    }
}
