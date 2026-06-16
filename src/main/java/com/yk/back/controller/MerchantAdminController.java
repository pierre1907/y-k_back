package com.yk.back.controller;

import com.yk.back.dto.request.MerchantRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.MerchantResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.MerchantService;
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
@RequestMapping("/admin/merchants")
@RequiredArgsConstructor
@Tag(name = "Admin — Merchants", description = "Gestion des merchants (plateforme admin)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class MerchantAdminController {

    private final MerchantService merchantService;

    @Operation(summary = "Lister tous les merchants")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> list(
            @RequestParam(required = false) UUID tenantId
    ) {
        List<MerchantResponse> result = tenantId != null
                ? merchantService.listByTenant(tenantId)
                : merchantService.listAll();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Détail d'un merchant")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(merchantService.getById(id)));
    }

    @Operation(summary = "Créer un merchant")
    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> create(
            @Valid @RequestBody MerchantRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        MerchantResponse created = merchantService.create(request, admin.email(), admin.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Merchant créé", created));
    }

    @Operation(summary = "Mettre à jour un merchant")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Merchant mis à jour", merchantService.update(id, request)));
    }

    @Operation(summary = "Activer un merchant")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MerchantResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Merchant activé", merchantService.activate(id)));
    }

    @Operation(summary = "Désactiver un merchant")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<MerchantResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Merchant désactivé", merchantService.deactivate(id)));
    }
}
