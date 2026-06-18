package com.yk.back.service;

import com.yk.back.dto.request.TenantRequest;
import com.yk.back.dto.response.SubscriptionResponse;
import com.yk.back.dto.response.TenantResponse;
import com.yk.back.entity.Tenant;
import com.yk.back.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.SubscriptionRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final MerchantRepository merchantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public List<TenantResponse> listAll() {
        return tenantRepository.findAll().stream()
                .map(t -> {
                    long count = merchantRepository.findAllByTenantId(t.getId()).size();
                    SubscriptionResponse sub = subscriptionRepository
                            .findFirstByTenantIdOrderByCreatedAtDesc(t.getId())
                            .map(SubscriptionResponse::from)
                            .orElse(null);
                    return TenantResponse.from(t, count, sub);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse getById(UUID id) {
        Tenant tenant = findOrThrow(id);
        long count = merchantRepository.findAllByTenantId(id).size();
        SubscriptionResponse sub = subscriptionRepository
                .findFirstByTenantIdOrderByCreatedAtDesc(id)
                .map(SubscriptionResponse::from)
                .orElse(null);
        return TenantResponse.from(tenant, count, sub);
    }

    @Transactional
    public TenantResponse create(TenantRequest request, String adminEmail, String adminName) {
        String slug = resolveSlug(request.slug(), request.name());

        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessException("Un tenant avec le slug '" + slug + "' existe déjà", HttpStatus.CONFLICT);
        }

        Tenant tenant = Tenant.builder()
                .name(request.name())
                .slug(slug)
                .contactEmail(request.contactEmail())
                .build();

        tenant = tenantRepository.save(tenant);
        mailService.sendTenantCreated(adminEmail, adminName, tenant.getName());

        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse update(UUID id, TenantRequest request) {
        Tenant tenant = findOrThrow(id);

        String slug = resolveSlug(request.slug(), request.name());
        if (!slug.equals(tenant.getSlug()) && tenantRepository.existsBySlug(slug)) {
            throw new BusinessException("Un tenant avec le slug '" + slug + "' existe déjà", HttpStatus.CONFLICT);
        }

        tenant.setName(request.name());
        tenant.setSlug(slug);
        tenant.setContactEmail(request.contactEmail());
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponse activate(UUID id, String adminEmail, String adminName) {
        Tenant tenant = findOrThrow(id);
        tenant.setActive(true);
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse deactivate(UUID id) {
        Tenant tenant = findOrThrow(id);
        tenant.setActive(false);
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    private Tenant findOrThrow(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id.toString()));
    }

    private String resolveSlug(String providedSlug, String name) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return providedSlug.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
