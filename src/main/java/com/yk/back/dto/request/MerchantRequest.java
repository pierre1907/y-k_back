package com.yk.back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MerchantRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String slug
) {}
