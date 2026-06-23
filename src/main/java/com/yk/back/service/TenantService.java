package com.yk.back.service;

import com.yk.back.dto.request.TenantRequest;
import com.yk.back.dto.response.SubscriptionResponse;
import com.yk.back.dto.response.TenantResponse;
import com.yk.back.entity.PasswordResetToken;
import com.yk.back.entity.Plan;
import com.yk.back.entity.Subscription;
import com.yk.back.entity.Tenant;
import com.yk.back.entity.TenantUser;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.PasswordResetTokenRepository;
import com.yk.back.repository.PlanRepository;
import com.yk.back.repository.SubscriptionRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
import com.yk.back.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    private static final String FREE_PLAN_NAME = "FREE";
    private static final long INVITE_TOKEN_VALIDITY_HOURS = 24;
    private static final long TRIAL_DURATION_DAYS = 7;

    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final MerchantRepository merchantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<TenantResponse> listAll() {
        return toResponses(tenantRepository.findAllByArchivedAtIsNull());
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listArchived() {
        return toResponses(tenantRepository.findAllByArchivedAtIsNotNull());
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

        if (request.contactEmail() != null && !request.contactEmail().isBlank()) {
            provisionTenantAdmin(tenant, request.contactEmail());
        }

        provisionSubscription(tenant, request.planId());

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

    @Transactional
    public TenantResponse archive(UUID id) {
        Tenant tenant = findOrThrow(id);
        if (tenant.getArchivedAt() != null) {
            throw new BusinessException("Ce tenant est déjà archivé", HttpStatus.CONFLICT);
        }
        tenant.setArchivedAt(OffsetDateTime.now());
        tenant.setActive(false);
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponse restore(UUID id) {
        Tenant tenant = findOrThrow(id);
        if (tenant.getArchivedAt() == null) {
            throw new BusinessException("Ce tenant n'est pas archivé", HttpStatus.CONFLICT);
        }
        tenant.setArchivedAt(null);
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional
    public void delete(UUID id) {
        Tenant tenant = findOrThrow(id);
        tenantRepository.delete(tenant);
    }

    private void provisionSubscription(Tenant tenant, UUID planId) {
        Plan plan = planId != null
                ? planRepository.findById(planId)
                        .orElseThrow(() -> new ResourceNotFoundException("Plan", planId.toString()))
                : planRepository.findByName(FREE_PLAN_NAME)
                        .orElseThrow(() -> new BusinessException(
                                "Plan FREE introuvable, contactez le support", HttpStatus.CONFLICT));

        boolean isFreePlan = plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) == 0;

        Subscription.Status status = isFreePlan ? Subscription.Status.ACTIVE : Subscription.Status.TRIAL;
        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .plan(plan)
                .status(status)
                .startsAt(OffsetDateTime.now())
                .trialEndsAt(isFreePlan ? null : OffsetDateTime.now().plusDays(TRIAL_DURATION_DAYS))
                .build();
        subscriptionRepository.save(subscription);
    }

    private void provisionTenantAdmin(Tenant tenant, String email) {
        TenantUser admin = TenantUser.builder()
                .tenant(tenant)
                .fullName(tenant.getName() + " — Admin")
                .email(email)
                .password(passwordEncoder.encode(randomToken()))
                .role(ROLE_TENANT_ADMIN)
                .build();
        tenantUserRepository.save(admin);

        String token = randomToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusHours(INVITE_TOKEN_VALIDITY_HOURS))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        mailService.sendTenantAdminInvite(email, tenant.getName(), token);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private List<TenantResponse> toResponses(List<Tenant> tenants) {
        return tenants.stream()
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
