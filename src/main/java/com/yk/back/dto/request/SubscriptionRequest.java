package com.yk.back.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionRequest(
        @NotNull UUID tenantId,
        @NotNull UUID planId,
        @NotNull String status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime trialEndsAt
) {}
