package com.yk.back.service;

import com.yk.back.dto.response.DashboardStatsResponse;
import com.yk.back.entity.Subscription;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.SubscriptionRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TenantRepository tenantRepository;
    private final MerchantRepository merchantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final SubscriptionRepository subscriptionRepository;

    public DashboardStatsResponse stats() {
        return new DashboardStatsResponse(
                tenantRepository.count(),
                tenantRepository.countByIsActiveTrue(),
                merchantRepository.count(),
                tenantUserRepository.count(),
                merchantUserRepository.count(),
                subscriptionRepository.countByStatus(Subscription.Status.ACTIVE),
                subscriptionRepository.countByStatus(Subscription.Status.TRIAL)
        );
    }
}
