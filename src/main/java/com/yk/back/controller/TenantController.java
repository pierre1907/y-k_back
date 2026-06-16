package com.yk.back.controller;

import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.MerchantResponse;
import com.yk.back.dto.response.TenantDashboardResponse;
import com.yk.back.dto.response.TenantResponse;
import com.yk.back.security.AuthenticatedUser;
import com.yk.back.service.TenantContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant — Contexte", description = "Profil, dashboard et merchants du tenant connecté")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
public class TenantController {

    private final TenantContextService tenantContextService;

    @Operation(summary = "Profil du tenant courant")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<TenantResponse>> profile(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantContextService.getProfile(user.tenantId())));
    }

    @Operation(summary = "Dashboard KPIs du tenant")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TenantDashboardResponse>> dashboard(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantContextService.getDashboard(user.tenantId())));
    }

    @Operation(summary = "Merchants rattachés au tenant courant")
    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> merchants(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tenantContextService.getMerchants(user.tenantId())));
    }
}
