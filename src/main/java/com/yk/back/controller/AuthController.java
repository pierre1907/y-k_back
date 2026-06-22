package com.yk.back.controller;

import com.yk.back.dto.request.ForgotPasswordRequest;
import com.yk.back.dto.request.LoginRequest;
import com.yk.back.dto.request.RefreshTokenRequest;
import com.yk.back.dto.request.ResetPasswordRequest;
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

    @Operation(summary = "Connexion", description = "Retourne access + refresh token à plat (standard JWT). tenantSlug obligatoire pour les merchant/tenant users.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @Operation(summary = "Rafraîchir le token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Déconnexion", description = "Stateless — le client supprime ses tokens côté front.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Déconnexion réussie", null));
    }

    @Operation(summary = "Mot de passe oublié", description = "Envoie un lien de réinitialisation par email si le compte existe. Réponse générique dans tous les cas.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.ok(
                "Si un compte existe avec cette adresse, un email de réinitialisation a été envoyé.", null));
    }

    @Operation(summary = "Réinitialiser le mot de passe", description = "Consomme le token reçu par email et définit le nouveau mot de passe.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe réinitialisé avec succès.", null));
    }
}
