package com.yk.back.controller;

import com.yk.back.dto.request.TenantUserRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.TenantUserResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.TenantUserService;
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
@RequestMapping("/tenant/users")
@RequiredArgsConstructor
@Tag(name = "Tenant — Utilisateurs", description = "Gestion des utilisateurs du tenant (TENANT_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class TenantUserController {

    private final TenantUserService tenantUserService;

    @Operation(summary = "Lister les utilisateurs du tenant")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantUserResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantUserService.listByTenant(user.tenantId())));
    }

    @Operation(summary = "Détail d'un utilisateur")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantUserResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantUserService.getById(id, user.tenantId())));
    }

    @Operation(summary = "Créer un utilisateur tenant")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantUserResponse>> create(
            @Valid @RequestBody TenantUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Utilisateur créé", tenantUserService.create(request, user.tenantId())));
    }

    @Operation(summary = "Mettre à jour un utilisateur")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantUserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TenantUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur mis à jour",
                tenantUserService.update(id, request, user.tenantId())));
    }

    @Operation(summary = "Activer un utilisateur")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<TenantUserResponse>> activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur activé",
                tenantUserService.activate(id, user.tenantId())));
    }

    @Operation(summary = "Désactiver un utilisateur")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<TenantUserResponse>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur désactivé",
                tenantUserService.deactivate(id, user.tenantId())));
    }
}
