package com.yk.back.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        UUID tenantId,
        UUID merchantId,
        String role
) {}
