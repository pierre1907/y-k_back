package com.yk.back.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotNull UUID merchantId,
        @NotBlank @Size(max = 255) String customerName,
        @Size(max = 50) String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String notes,
        @NotEmpty @Valid List<OrderItemRequest> items
) {}
