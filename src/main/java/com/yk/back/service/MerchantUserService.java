package com.yk.back.service;

import com.yk.back.dto.request.MerchantUserRequest;
import com.yk.back.dto.response.MerchantUserResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.MerchantUser;
import com.yk.back.entity.Plan;
import com.yk.back.entity.Subscription;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.SubscriptionRepository;
import com.yk.back.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantUserService {

    private final MerchantUserRepository merchantUserRepository;
    private final MerchantRepository merchantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public List<MerchantUserResponse> listByMerchant(UUID merchantId, UUID tenantId) {
        return merchantUserRepository.findAllByMerchantIdAndTenantId(merchantId, tenantId).stream()
                .map(MerchantUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MerchantUserResponse> listByTenant(UUID tenantId) {
        return merchantUserRepository.findAllByTenantId(tenantId).stream()
                .map(MerchantUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantUserResponse getById(UUID id, UUID tenantId) {
        return MerchantUserResponse.from(findOrThrow(id, tenantId));
    }

    @Transactional
    public MerchantUserResponse create(MerchantUserRequest request, UUID tenantId) {
        Merchant merchant = merchantRepository.findByIdAndTenantId(request.merchantId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", request.merchantId().toString()));

        Plan plan = getEnforceablePlan(tenantId);
        if (plan.getMaxUsersPerMerchant() != null) {
            long current = merchantUserRepository.countByMerchantId(merchant.getId());
            if (current >= plan.getMaxUsersPerMerchant()) {
                throw new BusinessException(
                        "Quota atteint : votre plan autorise " + plan.getMaxUsersPerMerchant() + " utilisateur(s) maximum par marchand.",
                        HttpStatus.PAYMENT_REQUIRED);
            }
        }

        if (merchantUserRepository.existsByEmailAndMerchantId(request.email(), merchant.getId())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email '" + request.email() + "' existe déjà sur ce merchant",
                    HttpStatus.CONFLICT);
        }

        MerchantUser user = MerchantUser.builder()
                .merchant(merchant)
                .tenant(merchant.getTenant())
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        user = merchantUserRepository.save(user);
        mailService.sendWelcome(user.getEmail(),
                user.getFullName() != null ? user.getFullName() : user.getEmail());
        return MerchantUserResponse.from(user);
    }

    @Transactional
    public MerchantUserResponse update(UUID id, MerchantUserRequest request, UUID tenantId) {
        MerchantUser user = findOrThrow(id, tenantId);

        if (!user.getEmail().equals(request.email())
                && merchantUserRepository.existsByEmailAndMerchantId(request.email(), user.getMerchant().getId())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email '" + request.email() + "' existe déjà",
                    HttpStatus.CONFLICT);
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return MerchantUserResponse.from(merchantUserRepository.save(user));
    }

    @Transactional
    public MerchantUserResponse activate(UUID id, UUID tenantId) {
        MerchantUser user = findOrThrow(id, tenantId);
        user.setActive(true);
        merchantUserRepository.save(user);
        mailService.sendAccountActivated(user.getEmail(),
                user.getFullName() != null ? user.getFullName() : user.getEmail());
        return MerchantUserResponse.from(user);
    }

    @Transactional
    public MerchantUserResponse deactivate(UUID id, UUID tenantId) {
        MerchantUser user = findOrThrow(id, tenantId);
        user.setActive(false);
        merchantUserRepository.save(user);
        mailService.sendAccountDeactivated(user.getEmail(),
                user.getFullName() != null ? user.getFullName() : user.getEmail());
        return MerchantUserResponse.from(user);
    }

    private MerchantUser findOrThrow(UUID id, UUID tenantId) {
        return merchantUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ForbiddenException("Utilisateur introuvable ou accès refusé"));
    }

    private Plan getEnforceablePlan(UUID tenantId) {
        Subscription subscription = subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .filter(s -> s.getStatus() == Subscription.Status.TRIAL || s.getStatus() == Subscription.Status.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "Aucun abonnement actif pour ce tenant. Contactez l'administrateur.", HttpStatus.PAYMENT_REQUIRED));
        return subscription.getPlan();
    }
}
