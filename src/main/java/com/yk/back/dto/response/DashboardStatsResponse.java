package com.yk.back.dto.response;

public record DashboardStatsResponse(
        long totalTenants,
        long activeTenants,
        long totalMerchants,
        long totalTenantUsers,
        long totalMerchantUsers,
        long activeSubscriptions,
        long trialSubscriptions
) {}
