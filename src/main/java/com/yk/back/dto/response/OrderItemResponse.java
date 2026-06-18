package com.yk.back.dto.response;

import com.yk.back.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String name,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem i) {
        return new OrderItemResponse(
                i.getId(), i.getProduct() != null ? i.getProduct().getId() : null, i.getName(), i.getSku(),
                i.getQuantity(), i.getUnitPrice(), i.getTotalPrice()
        );
    }
}
