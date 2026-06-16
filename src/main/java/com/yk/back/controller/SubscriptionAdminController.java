package com.yk.back.controller;

import com.yk.back.dto.request.SubscriptionRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.SubscriptionResponse;
import com.yk.back.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Admin — Abonnements", description = "Gestion des abonnements des tenants")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class SubscriptionAdminController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Lister tous les abonnements")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> list(
            @RequestParam(required = false) UUID tenantId
    ) {
        List<SubscriptionResponse> result = tenantId != null
                ? subscriptionService.listByTenant(tenantId)
                : subscriptionService.listAll();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Détail d'un abonnement")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(subscriptionService.getById(id)));
    }

    @Operation(summary = "Créer un abonnement pour un tenant")
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Abonnement créé", subscriptionService.create(request)));
    }

    @Operation(summary = "Activer un abonnement")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Abonnement activé", subscriptionService.activate(id)));
    }

    @Operation(summary = "Annuler un abonnement")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Abonnement annulé", subscriptionService.cancel(id)));
    }

    @Operation(summary = "Marquer un abonnement comme expiré")
    @PatchMapping("/{id}/expire")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> expire(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Abonnement expiré", subscriptionService.expire(id)));
    }
}
