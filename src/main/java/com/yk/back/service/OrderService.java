package com.yk.back.service;

import com.yk.back.dto.request.OrderRequest;
import com.yk.back.dto.response.OrderResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.Order;
import com.yk.back.entity.OrderItem;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;

    public List<OrderResponse> listByTenant(UUID tenantId) {
        return orderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(OrderResponse::from).toList();
    }

    public List<OrderResponse> listByMerchant(UUID merchantId, UUID tenantId) {
        return orderRepository.findAllByMerchantIdAndTenantIdOrderByCreatedAtDesc(merchantId, tenantId).stream()
                .map(OrderResponse::from).toList();
    }

    public OrderResponse getById(UUID id, UUID tenantId) {
        return OrderResponse.from(findOrThrow(id, tenantId));
    }

    @Transactional
    public OrderResponse create(OrderRequest request, UUID tenantId, UUID createdBy) {
        Merchant merchant = merchantRepository.findByIdAndTenantId(request.merchantId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", request.merchantId().toString()));

        String ref = generateReference(tenantId);

        Order order = Order.builder()
                .tenant(merchant.getTenant())
                .merchant(merchant)
                .reference(ref)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .customerEmail(request.customerEmail())
                .deliveryAddress(request.deliveryAddress())
                .status(Order.Status.PENDING)
                .notes(request.notes())
                .totalAmount(BigDecimal.ZERO)
                .createdBy(createdBy)
                .build();

        List<OrderItem> items = request.items().stream().map(i -> OrderItem.builder()
                .order(order)
                .tenantId(tenantId)
                .name(i.name())
                .sku(i.sku())
                .quantity(i.quantity())
                .unitPrice(i.unitPrice())
                .build()
        ).toList();

        order.getItems().addAll(items);
        order.setTotalAmount(items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, String status, UUID tenantId) {
        Order order = findOrThrow(id, tenantId);
        Order.Status newStatus = parseStatus(status);
        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(UUID id, UUID tenantId) {
        Order order = findOrThrow(id, tenantId);
        if (order.getStatus() == Order.Status.DELIVERED) {
            throw new BusinessException("Une commande livrée ne peut pas être annulée", HttpStatus.CONFLICT);
        }
        order.setStatus(Order.Status.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order findOrThrow(UUID id, UUID tenantId) {
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ForbiddenException("Commande introuvable ou accès refusé"));
    }

    private String generateReference(UUID tenantId) {
        String prefix = tenantId.toString().substring(0, 4).toUpperCase();
        String ts = String.valueOf(Instant.now().toEpochMilli()).substring(7);
        String ref = "ORD-" + prefix + "-" + ts;
        if (orderRepository.existsByReferenceAndTenantId(ref, tenantId)) {
            ref = ref + "-" + (int)(Math.random() * 999);
        }
        return ref;
    }

    private Order.Status parseStatus(String status) {
        try {
            return Order.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut invalide : " + status, HttpStatus.BAD_REQUEST);
        }
    }
}
