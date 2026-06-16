package com.yk.back.security;

import com.yk.back.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final AppProperties appProperties;

    public String generateAccessToken(UUID userId, String email, UUID tenantId, UUID merchantId, String role) {
        return buildToken(userId, email, tenantId, merchantId, role,
                appProperties.getJwt().getExpirationMs());
    }

    public String generateRefreshToken(UUID userId, String email, UUID tenantId, UUID merchantId, String role) {
        return buildToken(userId, email, tenantId, merchantId, role,
                appProperties.getJwt().getRefreshExpirationMs());
    }

    private String buildToken(UUID userId, String email, UUID tenantId, UUID merchantId, String role, long ttl) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", userId.toString());
        if (tenantId != null) claims.put("tenantId", tenantId.toString());
        if (merchantId != null) claims.put("merchantId", merchantId.toString());
        claims.put("role", role);

        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(secretKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString((String) extractAllClaims(token).get("userId"));
    }

    public UUID extractTenantId(String token) {
        Object val = extractAllClaims(token).get("tenantId");
        return val != null ? UUID.fromString((String) val) : null;
    }

    public UUID extractMerchantId(String token) {
        Object val = extractAllClaims(token).get("merchantId");
        return val != null ? UUID.fromString((String) val) : null;
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    private SecretKey secretKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
