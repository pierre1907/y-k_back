package com.yk.back.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        UUID productId,
        @NotBlank String name,
        String sku,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {}
