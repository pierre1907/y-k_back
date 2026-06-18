package com.yk.back.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockMovementRequest(
        @NotNull String type,       // IN | OUT | ADJUSTMENT
        @NotNull @Min(1) Integer quantity,
        String note
) {}
