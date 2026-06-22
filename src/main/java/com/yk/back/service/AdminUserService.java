package com.yk.back.service;

import com.yk.back.dto.request.AdminUserRequest;
import com.yk.back.dto.request.AdminUserType;
import com.yk.back.dto.response.AdminUserResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.MerchantUser;
import com.yk.back.entity.Tenant;
import com.yk.back.entity.TenantUser;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
import com.yk.back.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final TenantUserRepository tenantUserRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final TenantRepository tenantRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public List<AdminUserResponse> listAll() {
        List<AdminUserResponse> result = new ArrayList<>();
        tenantUserRepository.findAll().stream()
                .map(AdminUserResponse::fromTenantUser)
                .forEach(result::add);
        merchantUserRepository.findAll().stream()
                .map(AdminUserResponse::fromMerchantUser)
                .forEach(result::add);
        result.sort(Comparator.comparing(AdminUserResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public AdminUserResponse getById(UUID id, AdminUserType type) {
        return switch (type) {
            case TENANT_USER -> AdminUserResponse.fromTenantUser(findTenantUserOrThrow(id));
            case MERCHANT_USER -> AdminUserResponse.fromMerchantUser(findMerchantUserOrThrow(id));
        };
    }

    @Transactional
    public AdminUserResponse create(AdminUserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Le mot de passe est requis", HttpStatus.BAD_REQUEST);
        }

        return switch (request.type()) {
            case TENANT_USER -> createTenantUser(request);
            case MERCHANT_USER -> createMerchantUser(request);
        };
    }

    @Transactional
    public AdminUserResponse update(UUID id, AdminUserRequest request) {
        return switch (request.type()) {
            case TENANT_USER -> updateTenantUser(id, request);
            case MERCHANT_USER -> updateMerchantUser(id, request);
        };
    }

    @Transactional
    public AdminUserResponse activate(UUID id, AdminUserType type) {
        return switch (type) {
            case TENANT_USER -> {
                TenantUser user = findTenantUserOrThrow(id);
                user.setActive(true);
                tenantUserRepository.save(user);
                mailService.sendAccountActivated(user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getEmail());
                yield AdminUserResponse.fromTenantUser(user);
            }
            case MERCHANT_USER -> {
                MerchantUser user = findMerchantUserOrThrow(id);
                user.setActive(true);
                merchantUserRepository.save(user);
                mailService.sendAccountActivated(user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getEmail());
                yield AdminUserResponse.fromMerchantUser(user);
            }
        };
    }

    @Transactional
    public AdminUserResponse deactivate(UUID id, AdminUserType type) {
        return switch (type) {
            case TENANT_USER -> {
                TenantUser user = findTenantUserOrThrow(id);
                user.setActive(false);
                tenantUserRepository.save(user);
                mailService.sendAccountDeactivated(user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getEmail());
                yield AdminUserResponse.fromTenantUser(user);
            }
            case MERCHANT_USER -> {
                MerchantUser user = findMerchantUserOrThrow(id);
                user.setActive(false);
                merchantUserRepository.save(user);
                mailService.sendAccountDeactivated(user.getEmail(),
                        user.getFullName() != null ? user.getFullName() : user.getEmail());
                yield AdminUserResponse.fromMerchantUser(user);
            }
        };
    }

    private AdminUserResponse createTenantUser(AdminUserRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantId().toString()));

        if (tenantUserRepository.existsByEmailAndTenantId(request.email(), tenant.getId())) {
            throw new BusinessException(
                    "Un utilisateur avec l'email '" + request.email() + "' existe déjà sur ce tenant",
                    HttpStatus.CONFLICT);
        }

        TenantUser user = TenantUser.builder()
                .tenant(tenant)
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        user = tenantUserRepository.save(user);
        mailService.sendWelcome(user.getEmail(), user.getFullName() != null ? user.getFullName() : user.getEmail());
        return AdminUserResponse.fromTenantUser(user);
    }

    private AdminUserResponse createMerchantUser(AdminUserRequest request) {
        if (request.merchantId() == null) {
            throw new BusinessException("Le merchant est requis pour un utilisateur merchant", HttpStatus.BAD_REQUEST);
        }

        Merchant merchant = merchantRepository.findByIdAndTenantId(request.merchantId(), request.tenantId())
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
        mailService.sendWelcome(user.getEmail(), user.getFullName() != null ? user.getFullName() : user.getEmail());
        return AdminUserResponse.fromMerchantUser(user);
    }

    private AdminUserResponse updateTenantUser(UUID id, AdminUserRequest request) {
        TenantUser user = findTenantUserOrThrow(id);

        if (!user.getEmail().equals(request.email())
                && tenantUserRepository.existsByEmailAndTenantId(request.email(), user.getTenant().getId())) {
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
        return AdminUserResponse.fromTenantUser(tenantUserRepository.save(user));
    }

    private AdminUserResponse updateMerchantUser(UUID id, AdminUserRequest request) {
        MerchantUser user = findMerchantUserOrThrow(id);

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
        return AdminUserResponse.fromMerchantUser(merchantUserRepository.save(user));
    }

    private TenantUser findTenantUserOrThrow(UUID id) {
        return tenantUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", id.toString()));
    }

    private MerchantUser findMerchantUserOrThrow(UUID id) {
        return merchantUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MerchantUser", id.toString()));
    }
}
