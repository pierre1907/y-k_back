package com.yk.back.service;

import com.yk.back.config.AppProperties;
import com.yk.back.dto.request.LoginRequest;
import com.yk.back.dto.request.RefreshTokenRequest;
import com.yk.back.dto.response.LoginResponse;
import com.yk.back.entity.MerchantUser;
import com.yk.back.entity.PlatformAdmin;
import com.yk.back.entity.Tenant;
import com.yk.back.entity.TenantUser;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.exception.UnauthorizedException;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.PlatformAdminRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
import com.yk.back.security.JwtService;
import com.yk.back.service.mail.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final TenantUserRepository tenantUserRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final TenantRepository tenantRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties appProperties;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);

        // 1. Platform admin — pas de tenantSlug, email reconnu comme platform admin
        if (request.tenantSlug() == null || request.tenantSlug().isBlank()) {
            Optional<PlatformAdmin> adminOpt = platformAdminRepository.findByEmail(request.email());
            if (adminOpt.isPresent()) {
                PlatformAdmin admin = adminOpt.get();
                validateCredentials(request.password(), admin.getPassword(), admin.isActive());

                String access  = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), null, null, ROLE_PLATFORM_ADMIN);
                String refresh = jwtService.generateRefreshToken(admin.getId(), admin.getEmail(), null, null, ROLE_PLATFORM_ADMIN);

                mailService.sendLoginAlert(admin.getEmail(), admin.getFullName(), ip);

                return LoginResponse.of(access, refresh, expiresInSeconds(),
                        admin.getId(), admin.getEmail(), null, null, ROLE_PLATFORM_ADMIN);
            }
        }

        // 2. Tenant user ou Merchant user
        Tenant tenant = resolveTenant(request);

        Optional<TenantUser> tenantUserOpt = tenantUserRepository
                .findByEmailAndTenantId(request.email(), tenant.getId());

        if (tenantUserOpt.isPresent()) {
            TenantUser user = tenantUserOpt.get();
            validateCredentials(request.password(), user.getPassword(), user.isActive());

            String access  = jwtService.generateAccessToken(user.getId(), user.getEmail(), tenant.getId(), null, user.getRole());
            String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail(), tenant.getId(), null, user.getRole());

            mailService.sendLoginAlert(user.getEmail(), user.getEmail(), ip);

            return LoginResponse.of(access, refresh, expiresInSeconds(),
                    user.getId(), user.getEmail(), tenant.getId(), null, user.getRole());
        }

        Optional<MerchantUser> merchantUserOpt = merchantUserRepository
                .findByEmailAndTenantId(request.email(), tenant.getId());

        if (merchantUserOpt.isPresent()) {
            MerchantUser user = merchantUserOpt.get();
            validateCredentials(request.password(), user.getPassword(), user.isActive());

            String access  = jwtService.generateAccessToken(user.getId(), user.getEmail(), tenant.getId(), user.getMerchant().getId(), user.getRole());
            String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail(), tenant.getId(), user.getMerchant().getId(), user.getRole());

            mailService.sendLoginAlert(user.getEmail(), user.getEmail(), ip);

            return LoginResponse.of(access, refresh, expiresInSeconds(),
                    user.getId(), user.getEmail(), tenant.getId(), user.getMerchant().getId(), user.getRole());
        }

        throw new UnauthorizedException("Identifiants incorrects");
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Refresh token invalide ou expiré");
        }

        UUID userId     = jwtService.extractUserId(token);
        String email    = jwtService.extractEmail(token);
        UUID tenantId   = jwtService.extractTenantId(token);
        UUID merchantId = jwtService.extractMerchantId(token);
        String role     = jwtService.extractRole(token);

        String newAccess  = jwtService.generateAccessToken(userId, email, tenantId, merchantId, role);
        String newRefresh = jwtService.generateRefreshToken(userId, email, tenantId, merchantId, role);

        return LoginResponse.of(newAccess, newRefresh, expiresInSeconds(),
                userId, email, tenantId, merchantId, role);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Tenant resolveTenant(LoginRequest request) {
        if (request.tenantSlug() != null && !request.tenantSlug().isBlank()) {
            return tenantRepository.findBySlug(request.tenantSlug())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantSlug()));
        }
        return tenantUserRepository.findByEmail(request.email())
                .map(TenantUser::getTenant)
                .orElseThrow(() -> new UnauthorizedException("Identifiants incorrects"));
    }

    private void validateCredentials(String rawPassword, String encodedPassword, boolean isActive) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UnauthorizedException("Identifiants incorrects");
        }
        if (!isActive) {
            throw new UnauthorizedException("Ce compte est désactivé");
        }
    }

    private long expiresInSeconds() {
        return appProperties.getJwt().getExpirationMs() / 1000;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
