package com.yk.back.controller;

import com.yk.back.dto.request.ProductRequest;
import com.yk.back.dto.request.StockMovementRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.ProductResponse;
import com.yk.back.dto.response.StockMovementResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.ProductService;
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
@Tag(name = "Products & Stock", description = "Catalogue produits et gestion des stocks")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    // ── Tenant-scoped ────────────────────────────────────────────────────────

    @GetMapping("/tenant/products")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Lister tous les produits du tenant")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listByTenant(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(productService.listByTenant(user.tenantId())));
    }

    @PostMapping("/tenant/products")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Créer un produit")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(productService.create(request, user.tenantId())));
    }

    @GetMapping("/tenant/products/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Détail d'un produit")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id, user.tenantId())));
    }

    @PutMapping("/tenant/products/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Modifier un produit")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(productService.update(id, request, user.tenantId())));
    }

    @PatchMapping("/tenant/products/{id}/toggle")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Activer / désactiver un produit")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        productService.toggleActive(id, user.tenantId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Stock movements ──────────────────────────────────────────────────────

    @GetMapping("/tenant/products/{id}/stock")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Historique des mouvements de stock")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> movements(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(productService.listMovements(id, user.tenantId())));
    }

    @PostMapping("/tenant/products/{id}/stock")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
    @Operation(summary = "Enregistrer un mouvement de stock (IN / OUT / ADJUSTMENT)")
    public ResponseEntity<ApiResponse<StockMovementResponse>> addMovement(
            @PathVariable UUID id,
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                productService.addMovement(id, request, user.tenantId(), user.userId())));
    }

    // ── Merchant-scoped ──────────────────────────────────────────────────────

    @GetMapping("/merchant/products")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN','MERCHANT_USER')")
    @Operation(summary = "Lister les produits du merchant connecté")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listByMerchant(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.listByMerchant(user.merchantId(), user.tenantId())));
    }

    @GetMapping("/merchant/products/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN','MERCHANT_USER')")
    @Operation(summary = "Détail d'un produit (merchant)")
    public ResponseEntity<ApiResponse<ProductResponse>> getByIdForMerchant(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id, user.tenantId())));
    }

    @GetMapping("/merchant/products/{id}/stock")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN','MERCHANT_USER')")
    @Operation(summary = "Historique des mouvements de stock (merchant)")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> movementsForMerchant(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.listMovementsForMerchant(id, user.merchantId(), user.tenantId())));
    }
}
