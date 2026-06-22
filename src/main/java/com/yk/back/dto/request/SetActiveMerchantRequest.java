package com.yk.back.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetActiveMerchantRequest(
        @NotNull UUID merchantId
) {}
