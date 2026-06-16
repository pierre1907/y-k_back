package com.yk.back.dto.response;

public record TenantDashboardResponse(
        long merchantCount,
        long tenantUserCount,
        long merchantUserCount,
        SubscriptionResponse activeSubscription
) {}
