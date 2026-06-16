package com.yk.back.controller;

import com.yk.back.dto.request.LoginRequest;
import com.yk.back.dto.request.RefreshTokenRequest;
import com.yk.back.dto.response.ApiResponse;
import com.yk.back.dto.response.LoginResponse;
import com.yk.back.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login, refresh token et logout")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Connexion", description = "Retourne access token + refresh token. tenantSlug obligatoire pour les merchant users.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Connexion réussie", response));
    }

    @Operation(summary = "Rafraîchir le token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Déconnexion", description = "Stateless — le client supprime ses tokens côté front.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Stateless JWT : le client supprime ses tokens côté front
        return ResponseEntity.ok(ApiResponse.ok("Déconnexion réussie", null));
    }
}
