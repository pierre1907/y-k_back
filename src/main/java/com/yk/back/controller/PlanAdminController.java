package com.yk.back.controller;

import com.yk.back.dto.request.PlanRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.PlanResponse;
import com.yk.back.service.PlanService;
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
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
@Tag(name = "Admin — Plans", description = "Gestion des plans d'abonnement")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlanAdminController {

    private final PlanService planService;

    @Operation(summary = "Lister tous les plans")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(planService.listAll()));
    }

    @Operation(summary = "Détail d'un plan")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(planService.getById(id)));
    }

    @Operation(summary = "Créer un plan")
    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Plan créé", planService.create(request)));
    }

    @Operation(summary = "Mettre à jour un plan")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PlanRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Plan mis à jour", planService.update(id, request)));
    }

    @Operation(summary = "Activer / désactiver un plan")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<PlanResponse>> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(planService.toggleActive(id)));
    }
}
