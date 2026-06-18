package com.yk.back.dto.response;

import com.yk.back.entity.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID tenantId,
        UUID merchantId,
        String merchantName,
        String name,
        String description,
        String sku,
        String category,
        BigDecimal unitPrice,
        boolean isActive,
        Integer currentStock,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductResponse from(Product p, int stock) {
        return new ProductResponse(
                p.getId(),
                p.getTenant().getId(),
                p.getMerchant().getId(),
                p.getMerchant().getName(),
                p.getName(),
                p.getDescription(),
                p.getSku(),
                p.getCategory(),
                p.getUnitPrice(),
                p.isActive(),
                stock,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public static ProductResponse from(Product p) {
        return from(p, 0);
    }
}
