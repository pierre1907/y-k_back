package com.yk.back.service;

import com.yk.back.dto.request.MerchantUserRequest;
import com.yk.back.dto.response.MerchantUserResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.MerchantUser;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.MerchantUserRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public List<MerchantUserResponse> listByMerchant(UUID merchantId, UUID tenantId) {
        return merchantUserRepository.findAllByMerchantIdAndTenantId(merchantId, tenantId).stream()
                .map(MerchantUserResponse::from)
                .toList();
    }

    public List<MerchantUserResponse> listByTenant(UUID tenantId) {
        return merchantUserRepository.findAllByTenantId(tenantId).stream()
                .map(MerchantUserResponse::from)
                .toList();
    }

    public MerchantUserResponse getById(UUID id, UUID tenantId) {
        return MerchantUserResponse.from(findOrThrow(id, tenantId));
    }

    @Transactional
    public MerchantUserResponse create(MerchantUserRequest request, UUID tenantId) {
        Merchant merchant = merchantRepository.findByIdAndTenantId(request.merchantId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", request.merchantId().toString()));

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
}
