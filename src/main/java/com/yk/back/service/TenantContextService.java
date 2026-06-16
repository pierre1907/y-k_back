package com.yk.back.service;

import com.yk.back.dto.response.MerchantResponse;
import com.yk.back.dto.response.SubscriptionResponse;
import com.yk.back.dto.response.TenantDashboardResponse;
import com.yk.back.dto.response.TenantResponse;
import com.yk.back.entity.Tenant;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.SubscriptionRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantContextService {

    private final TenantRepository tenantRepository;
    private final MerchantRepository merchantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final SubscriptionRepository subscriptionRepository;

    public TenantResponse getProfile(UUID tenantId) {
        Tenant tenant = findOrThrow(tenantId);
        long count = merchantRepository.findAllByTenantId(tenantId).size();
        SubscriptionResponse sub = subscriptionRepository
                .findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(SubscriptionResponse::from).orElse(null);
        return TenantResponse.from(tenant, count, sub);
    }

    public List<MerchantResponse> getMerchants(UUID tenantId) {
        return merchantRepository.findAllByTenantId(tenantId).stream()
                .map(MerchantResponse::from)
                .toList();
    }

    public TenantDashboardResponse getDashboard(UUID tenantId) {
        SubscriptionResponse sub = subscriptionRepository
                .findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(SubscriptionResponse::from).orElse(null);
        return new TenantDashboardResponse(
                merchantRepository.findAllByTenantId(tenantId).size(),
                tenantUserRepository.countByTenantId(tenantId),
                merchantUserRepository.countByTenantId(tenantId),
                sub
        );
    }

    private Tenant findOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));
    }
}
