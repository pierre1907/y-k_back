package com.yk.back.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MerchantUserRequest(
        @NotNull UUID merchantId,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotBlank @Size(max = 50) String role,
        @Size(max = 255) String fullName
) {}
