package com.yk.back.service;

import com.yk.back.dto.request.TenantUserRequest;
import com.yk.back.dto.response.TenantUserResponse;
import com.yk.back.entity.Tenant;
import com.yk.back.entity.TenantUser;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
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
public class TenantUserService {

    private final TenantUserRepository tenantUserRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public List<TenantUserResponse> listByTenant(UUID tenantId) {
        return tenantUserRepository.findAllByTenantId(tenantId).stream()
                .map(TenantUserResponse::from)
                .toList();
    }

    public TenantUserResponse getById(UUID id, UUID tenantId) {
        return TenantUserResponse.from(findOrThrow(id, tenantId));
    }

    @Transactional
    public TenantUserResponse create(TenantUserRequest request, UUID tenantId) {
        if (tenantUserRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
            throw new BusinessException(
                    "Un utilisateur avec l'email '" + request.email() + "' existe déjà sur ce tenant",
                    HttpStatus.CONFLICT);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId.toString()));

        TenantUser user = TenantUser.builder()
                .tenant(tenant)
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        user = tenantUserRepository.save(user);
        mailService.sendWelcome(user.getEmail(), user.getFullName() != null ? user.getFullName() : user.getEmail());
        return TenantUserResponse.from(user);
    }

    @Transactional
    public TenantUserResponse update(UUID id, TenantUserRequest request, UUID tenantId) {
        TenantUser user = findOrThrow(id, tenantId);

        if (!user.getEmail().equals(request.email())
                && tenantUserRepository.existsByEmailAndTenantId(request.email(), tenantId)) {
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
        return TenantUserResponse.from(tenantUserRepository.save(user));
    }

    @Transactional
    public TenantUserResponse activate(UUID id, UUID tenantId) {
        TenantUser user = findOrThrow(id, tenantId);
        user.setActive(true);
        tenantUserRepository.save(user);
        mailService.sendAccountActivated(user.getEmail(),
                user.getFullName() != null ? user.getFullName() : user.getEmail());
        return TenantUserResponse.from(user);
    }

    @Transactional
    public TenantUserResponse deactivate(UUID id, UUID tenantId) {
        TenantUser user = findOrThrow(id, tenantId);
        user.setActive(false);
        tenantUserRepository.save(user);
        mailService.sendAccountDeactivated(user.getEmail(),
                user.getFullName() != null ? user.getFullName() : user.getEmail());
        return TenantUserResponse.from(user);
    }

    private TenantUser findOrThrow(UUID id, UUID tenantId) {
        TenantUser user = tenantUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", id.toString()));
        if (!user.getTenant().getId().equals(tenantId)) {
            throw new ForbiddenException("Accès refusé");
        }
        return user;
    }
}
