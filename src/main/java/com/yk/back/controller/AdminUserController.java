package com.yk.back.controller;

import com.yk.back.dto.response.AdminUserResponse;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — Utilisateurs", description = "Vue globale de tous les utilisateurs (tenant + merchant)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lister tous les utilisateurs de la plateforme")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.listAll()));
    }
}
