package com.yk.back.controller;

import com.yk.back.dto.request.TenantRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.TenantResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.TenantService;
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
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Admin — Tenants", description = "Gestion des tenants (plateforme admin uniquement)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantAdminController {

    private final TenantService tenantService;

    @Operation(summary = "Lister tous les tenants")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.listAll()));
    }

    @Operation(summary = "Détail d'un tenant")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getById(id)));
    }

    @Operation(summary = "Créer un tenant")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> create(
            @Valid @RequestBody TenantRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        TenantResponse created = tenantService.create(request, admin.email(), admin.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tenant créé", created));
    }

    @Operation(summary = "Mettre à jour un tenant")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tenant mis à jour", tenantService.update(id, request)));
    }

    @Operation(summary = "Activer un tenant")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<TenantResponse>> activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tenant activé", tenantService.activate(id, admin.email(), admin.email())));
    }

    @Operation(summary = "Désactiver un tenant")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<TenantResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tenant désactivé", tenantService.deactivate(id)));
    }
}
