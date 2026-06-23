package com.yk.back.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TenantRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String slug,
        @Email @Size(max = 255) String contactEmail,
        UUID planId
) {}
