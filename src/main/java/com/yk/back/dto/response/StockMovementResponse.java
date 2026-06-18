package com.yk.back.dto.response;

import com.yk.back.entity.StockMovement;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID productId,
        String productName,
        String type,
        int quantity,
        String note,
        UUID orderId,
        UUID createdBy,
        OffsetDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getName(),
                m.getType().name(),
                m.getQuantity(),
                m.getNote(),
                m.getOrderId(),
                m.getCreatedBy(),
                m.getCreatedAt()
        );
    }
}
