package com.yk.back.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotNull UUID merchantId,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 100) String sku,
        @Size(max = 100) String category,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {}
