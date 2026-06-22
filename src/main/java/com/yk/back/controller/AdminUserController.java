package com.yk.back.controller;

import com.yk.back.dto.request.AdminUserRequest;
import com.yk.back.dto.request.AdminUserType;
import com.yk.back.dto.response.AdminUserResponse;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.service.AdminUserService;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — Utilisateurs", description = "Gestion globale des utilisateurs (tenant + merchant)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lister tous les utilisateurs de la plateforme")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.listAll()));
    }

    @Operation(summary = "Détail d'un utilisateur")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getById(
            @PathVariable UUID id,
            @RequestParam AdminUserType type
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getById(id, type)));
    }

    @Operation(summary = "Créer un utilisateur (tenant ou merchant)")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> create(
            @Valid @RequestBody AdminUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Utilisateur créé", adminUserService.create(request)));
    }

    @Operation(summary = "Mettre à jour un utilisateur")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur mis à jour", adminUserService.update(id, request)));
    }

    @Operation(summary = "Activer un utilisateur")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> activate(
            @PathVariable UUID id,
            @RequestParam AdminUserType type
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur activé", adminUserService.activate(id, type)));
    }

    @Operation(summary = "Désactiver un utilisateur")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivate(
            @PathVariable UUID id,
            @RequestParam AdminUserType type
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur désactivé", adminUserService.deactivate(id, type)));
    }
}
