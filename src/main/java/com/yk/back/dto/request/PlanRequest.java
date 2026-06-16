package com.yk.back.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotBlank String billingCycle,
        Integer maxMerchants,
        Integer maxUsersPerMerchant
) {}
