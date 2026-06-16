package com.yk.back.service;

import com.yk.back.dto.request.SubscriptionRequest;
import com.yk.back.dto.response.SubscriptionResponse;
import com.yk.back.entity.Plan;
import com.yk.back.entity.Subscription;
import com.yk.back.entity.Tenant;
import com.yk.back.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.PlanRepository;
import com.yk.back.repository.SubscriptionRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final MailService mailService;

    public List<SubscriptionResponse> listAll() {
        return subscriptionRepository.findAll().stream().map(SubscriptionResponse::from).toList();
    }

    public List<SubscriptionResponse> listByTenant(UUID tenantId) {
        return subscriptionRepository.findAllByTenantId(tenantId).stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    public SubscriptionResponse getById(UUID id) {
        return SubscriptionResponse.from(findOrThrow(id));
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantId().toString()));

        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId().toString()));

        Subscription.Status status = parseStatus(request.status());

        Subscription sub = Subscription.builder()
                .tenant(tenant)
                .plan(plan)
                .status(status)
                .startsAt(request.startsAt() != null ? request.startsAt() : OffsetDateTime.now())
                .endsAt(request.endsAt())
                .trialEndsAt(request.trialEndsAt())
                .build();

        sub = subscriptionRepository.save(sub);

        if (status == Subscription.Status.ACTIVE && tenant.getContactEmail() != null) {
            mailService.sendSubscriptionActivated(tenant.getContactEmail(), tenant.getName(), plan.getName());
        }

        return SubscriptionResponse.from(sub);
    }

    @Transactional
    public SubscriptionResponse activate(UUID id) {
        Subscription sub = findOrThrow(id);
        sub.setStatus(Subscription.Status.ACTIVE);
        sub = subscriptionRepository.save(sub);

        Tenant tenant = sub.getTenant();
        if (tenant.getContactEmail() != null) {
            mailService.sendSubscriptionActivated(
                    tenant.getContactEmail(), tenant.getName(), sub.getPlan().getName());
        }
        return SubscriptionResponse.from(sub);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID id) {
        Subscription sub = findOrThrow(id);
        sub.setStatus(Subscription.Status.CANCELLED);
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    @Transactional
    public SubscriptionResponse expire(UUID id) {
        Subscription sub = findOrThrow(id);
        sub.setStatus(Subscription.Status.EXPIRED);
        subscriptionRepository.save(sub);

        Tenant tenant = sub.getTenant();
        if (tenant.getContactEmail() != null) {
            mailService.sendSubscriptionExpired(
                    tenant.getContactEmail(), tenant.getName(), sub.getPlan().getName());
        }
        return SubscriptionResponse.from(sub);
    }

    private Subscription findOrThrow(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id.toString()));
    }

    private Subscription.Status parseStatus(String status) {
        try {
            return Subscription.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut invalide : " + status, HttpStatus.BAD_REQUEST);
        }
    }
}
