package com.yk.back.controller;

import com.yk.back.dto.request.OrderRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.OrderResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.OrderService;
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
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestion des commandes")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    // ── Tenant-scoped endpoints ──────────────────────────────────────────────

    @GetMapping("/tenant/orders")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Lister toutes les commandes du tenant")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listByTenant(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.listByTenant(user.tenantId())));
    }

    @PostMapping("/tenant/orders")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Créer une commande")
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        OrderResponse created = orderService.create(request, user.tenantId(), user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/tenant/orders/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Détail d'une commande")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getById(id, user.tenantId())));
    }

    @PatchMapping("/tenant/orders/{id}/status")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Mettre à jour le statut d'une commande")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateStatus(id, status, user.tenantId())));
    }

    @PatchMapping("/tenant/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Annuler une commande")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancel(id, user.tenantId(), user.userId())));
    }

    // ── Merchant-scoped endpoints ────────────────────────────────────────────

    @GetMapping("/merchant/orders")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN','MERCHANT_USER')")
    @Operation(summary = "Lister les commandes du merchant connecté")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listByMerchant(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.listByMerchant(user.merchantId(), user.tenantId())));
    }

    @GetMapping("/merchant/orders/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN','MERCHANT_USER')")
    @Operation(summary = "Détail d'une commande (merchant)")
    public ResponseEntity<ApiResponse<OrderResponse>> getByIdForMerchant(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getById(id, user.tenantId())));
    }
}
