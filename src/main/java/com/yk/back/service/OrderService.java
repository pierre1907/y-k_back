package com.yk.back.service;

import com.yk.back.dto.request.OrderItemRequest;
import com.yk.back.dto.request.OrderRequest;
import com.yk.back.dto.response.OrderResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.Order;
import com.yk.back.entity.OrderItem;
import com.yk.back.entity.Product;
import com.yk.back.entity.StockMovement;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.OrderRepository;
import com.yk.back.repository.ProductRepository;
import com.yk.back.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

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

        Map<UUID, Product> productsById = resolveAndCheckStock(request.items(), merchant.getId(), tenantId);

        List<OrderItem> items = request.items().stream().map(i -> OrderItem.builder()
                .order(order)
                .tenantId(tenantId)
                .product(i.productId() != null ? productsById.get(i.productId()) : null)
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

        Order saved = orderRepository.save(order);

        items.stream()
                .filter(i -> i.getProduct() != null)
                .forEach(i -> stockMovementRepository.save(StockMovement.builder()
                        .tenantId(tenantId)
                        .product(i.getProduct())
                        .type(StockMovement.Type.OUT)
                        .quantity(i.getQuantity())
                        .orderId(saved.getId())
                        .createdBy(createdBy)
                        .note("Commande " + saved.getReference())
                        .build()));

        return OrderResponse.from(saved);
    }

    private Map<UUID, Product> resolveAndCheckStock(List<OrderItemRequest> items, UUID merchantId, UUID tenantId) {
        Map<UUID, Integer> requestedByProduct = new HashMap<>();
        for (OrderItemRequest item : items) {
            if (item.productId() != null) {
                requestedByProduct.merge(item.productId(), item.quantity(), Integer::sum);
            }
        }

        Map<UUID, Product> productsById = new HashMap<>();
        for (var entry : requestedByProduct.entrySet()) {
            Product product = productRepository.findByIdAndMerchantIdAndTenantId(entry.getKey(), merchantId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", entry.getKey().toString()));
            int stock = stockMovementRepository.currentStock(product.getId());
            if (stock < entry.getValue()) {
                throw new BusinessException(
                        "Stock insuffisant pour \"" + product.getName() + "\" (disponible: " + stock + ")",
                        HttpStatus.CONFLICT);
            }
            productsById.put(entry.getKey(), product);
        }
        return productsById;
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, String status, UUID tenantId) {
        Order order = findOrThrow(id, tenantId);
        Order.Status newStatus = parseStatus(status);
        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(UUID id, UUID tenantId, UUID cancelledBy) {
        Order order = findOrThrow(id, tenantId);
        if (order.getStatus() == Order.Status.DELIVERED) {
            throw new BusinessException("Une commande livrée ne peut pas être annulée", HttpStatus.CONFLICT);
        }
        if (order.getStatus() == Order.Status.CANCELLED) {
            return OrderResponse.from(order);
        }
        order.setStatus(Order.Status.CANCELLED);
        Order saved = orderRepository.save(order);

        saved.getItems().stream()
                .filter(i -> i.getProduct() != null)
                .forEach(i -> stockMovementRepository.save(StockMovement.builder()
                        .tenantId(tenantId)
                        .product(i.getProduct())
                        .type(StockMovement.Type.IN)
                        .quantity(i.getQuantity())
                        .orderId(saved.getId())
                        .createdBy(cancelledBy)
                        .note("Annulation commande " + saved.getReference())
                        .build()));

        return OrderResponse.from(saved);
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
