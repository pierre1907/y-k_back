package com.yk.back.dto.response;

import com.yk.back.entity.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID tenantId,
        UUID merchantId,
        String merchantName,
        String reference,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String status,
        String notes,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getTenant().getId(),
                o.getMerchant().getId(),
                o.getMerchant().getName(),
                o.getReference(),
                o.getCustomerName(),
                o.getCustomerPhone(),
                o.getCustomerEmail(),
                o.getDeliveryAddress(),
                o.getStatus().name(),
                o.getNotes(),
                o.getTotalAmount(),
                o.getItems().stream().map(OrderItemResponse::from).toList(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
