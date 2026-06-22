package com.yk.back.service;

import com.yk.back.config.AppProperties;
import com.yk.back.dto.request.LoginRequest;
import com.yk.back.dto.request.RefreshTokenRequest;
import com.yk.back.dto.response.LoginResponse;
import com.yk.back.entity.MerchantUser;
import com.yk.back.entity.PasswordResetToken;
import com.yk.back.entity.PlatformAdmin;
import com.yk.back.entity.Tenant;
import com.yk.back.entity.TenantUser;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.exception.UnauthorizedException;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.PasswordResetTokenRepository;
import com.yk.back.repository.PlatformAdminRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.repository.TenantUserRepository;
import com.yk.back.security.JwtService;
import com.yk.back.service.mail.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final long RESET_TOKEN_VALIDITY_MINUTES = 60;

    private final TenantUserRepository tenantUserRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final TenantRepository tenantRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

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

    public void forgotPassword(String email) {
        Optional<String> fullName = resolveFullNameByEmail(email);
        if (fullName.isEmpty()) {
            // Don't reveal whether the email is registered.
            log.debug("Password reset requested for unknown email {}", email);
            return;
        }

        String token = generateToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        mailService.sendPasswordReset(email, fullName.get(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Lien de réinitialisation invalide", HttpStatus.BAD_REQUEST));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("Lien de réinitialisation invalide ou expiré", HttpStatus.BAD_REQUEST);
        }

        String email = resetToken.getEmail();
        String encoded = passwordEncoder.encode(newPassword);
        String fullName = applyNewPassword(email, encoded)
                .orElseThrow(() -> new BusinessException("Lien de réinitialisation invalide", HttpStatus.BAD_REQUEST));

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        mailService.sendPasswordChanged(email, fullName);
    }

    private Optional<String> resolveFullNameByEmail(String email) {
        Optional<PlatformAdmin> admin = platformAdminRepository.findByEmail(email);
        if (admin.isPresent()) return Optional.of(admin.get().getFullName());

        Optional<TenantUser> tenantUser = tenantUserRepository.findByEmail(email);
        if (tenantUser.isPresent()) return Optional.of(tenantUser.get().getFullName());

        Optional<MerchantUser> merchantUser = merchantUserRepository.findByEmail(email);
        if (merchantUser.isPresent()) return Optional.of(merchantUser.get().getFullName());

        return Optional.empty();
    }

    private Optional<String> applyNewPassword(String email, String encodedPassword) {
        Optional<PlatformAdmin> admin = platformAdminRepository.findByEmail(email);
        if (admin.isPresent()) {
            PlatformAdmin a = admin.get();
            a.setPassword(encodedPassword);
            platformAdminRepository.save(a);
            return Optional.of(a.getFullName());
        }

        Optional<TenantUser> tenantUser = tenantUserRepository.findByEmail(email);
        if (tenantUser.isPresent()) {
            TenantUser u = tenantUser.get();
            u.setPassword(encodedPassword);
            tenantUserRepository.save(u);
            return Optional.of(u.getFullName());
        }

        Optional<MerchantUser> merchantUser = merchantUserRepository.findByEmail(email);
        if (merchantUser.isPresent()) {
            MerchantUser u = merchantUser.get();
            u.setPassword(encodedPassword);
            merchantUserRepository.save(u);
            return Optional.of(u.getFullName());
        }

        return Optional.empty();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
