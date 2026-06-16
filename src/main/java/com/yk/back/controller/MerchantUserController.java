package com.yk.back.controller;

import com.yk.back.dto.request.MerchantUserRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.MerchantUserResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.MerchantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/merchants/{merchantId}/users")
@RequiredArgsConstructor
@Tag(name = "Tenant — Merchant Users", description = "Utilisateurs rattachés à un merchant")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class MerchantUserController {

    private final MerchantUserService merchantUserService;

    @Operation(summary = "Lister les utilisateurs du merchant")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MerchantUserResponse>>> list(
            @PathVariable UUID merchantId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                merchantUserService.listByMerchant(merchantId, user.tenantId())));
    }

    @Operation(summary = "Créer un utilisateur merchant")
    @PostMapping
    public ResponseEntity<ApiResponse<MerchantUserResponse>> create(
            @PathVariable UUID merchantId,
            @Valid @RequestBody MerchantUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MerchantUserRequest withMerchant = new MerchantUserRequest(
                merchantId, request.email(), request.password(), request.role(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Utilisateur merchant créé",
                        merchantUserService.create(withMerchant, user.tenantId())));
    }

    @Operation(summary = "Mettre à jour un utilisateur merchant")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantUserResponse>> update(
            @PathVariable UUID merchantId,
            @PathVariable UUID id,
            @Valid @RequestBody MerchantUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MerchantUserRequest withMerchant = new MerchantUserRequest(
                merchantId, request.email(), request.password(), request.role(), request.fullName());
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur mis à jour",
                merchantUserService.update(id, withMerchant, user.tenantId())));
    }

    @Operation(summary = "Activer un utilisateur merchant")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MerchantUserResponse>> activate(
            @PathVariable UUID merchantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur activé",
                merchantUserService.activate(id, user.tenantId())));
    }

    @Operation(summary = "Désactiver un utilisateur merchant")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<MerchantUserResponse>> deactivate(
            @PathVariable UUID merchantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur désactivé",
                merchantUserService.deactivate(id, user.tenantId())));
    }
}
