package com.yk.back.service;

import com.yk.back.dto.request.OrderItemRequest;
import com.yk.back.dto.request.OrderRequest;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.Order;
import com.yk.back.entity.OrderItem;
import com.yk.back.entity.Product;
import com.yk.back.entity.StockMovement;
import com.yk.back.entity.Tenant;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.OrderRepository;
import com.yk.back.repository.ProductRepository;
import com.yk.back.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;

    private OrderService orderService;

    private UUID tenantId;
    private UUID merchantId;
    private Tenant tenant;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, merchantRepository, productRepository, stockMovementRepository);
        tenantId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        tenant = Tenant.builder().id(tenantId).name("Tenant A").build();
        merchant = Merchant.builder().id(merchantId).tenant(tenant).name("Merchant A").build();
    }

    private Product buildProduct(UUID id) {
        return Product.builder()
                .id(id)
                .tenant(tenant)
                .merchant(merchant)
                .name("Produit X")
                .unitPrice(new BigDecimal("10.00"))
                .isActive(true)
                .build();
    }

    private OrderRequest requestWithItems(List<OrderItemRequest> items) {
        return new OrderRequest(merchantId, "Client A", null, null, null, null, items);
    }

    private void stubOrderSave() {
        when(merchantRepository.findByIdAndTenantId(merchantId, tenantId)).thenReturn(Optional.of(merchant));
        when(orderRepository.existsByReferenceAndTenantId(any(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
    }

    // ── create — free-text items (no product link) ───────────────────────────

    @Test
    void create_withoutProductLink_doesNotTouchStock() {
        stubOrderSave();
        OrderRequest request = requestWithItems(List.of(
                new OrderItemRequest(null, "Article libre", "SKU-X", 2, new BigDecimal("5.00"))));

        var response = orderService.create(request, tenantId, UUID.randomUUID());

        assertThat(response.totalAmount()).isEqualTo(new BigDecimal("10.00"));
        verify(stockMovementRepository, never()).save(any());
        verify(productRepository, never()).findByIdAndMerchantIdAndTenantId(any(), any(), any());
    }

    // ── create — product-linked items ─────────────────────────────────────────

    @Test
    void create_withProductLinkAndSufficientStock_deductsStockWithOrderId() {
        stubOrderSave();
        Product product = buildProduct(UUID.randomUUID());
        UUID createdBy = UUID.randomUUID();
        OrderRequest request = requestWithItems(List.of(
                new OrderItemRequest(product.getId(), "Produit X", null, 3, new BigDecimal("10.00"))));

        when(productRepository.findByIdAndMerchantIdAndTenantId(product.getId(), merchantId, tenantId))
                .thenReturn(Optional.of(product));
        when(stockMovementRepository.currentStock(product.getId())).thenReturn(10);

        var response = orderService.create(request, tenantId, createdBy);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertThat(movement.getType()).isEqualTo(StockMovement.Type.OUT);
        assertThat(movement.getQuantity()).isEqualTo(3);
        assertThat(movement.getOrderId()).isEqualTo(response.id());
        assertThat(movement.getCreatedBy()).isEqualTo(createdBy);
    }

    @Test
    void create_withProductLinkAndInsufficientStock_throwsConflict_andDoesNotSaveOrder() {
        Product product = buildProduct(UUID.randomUUID());
        when(merchantRepository.findByIdAndTenantId(merchantId, tenantId)).thenReturn(Optional.of(merchant));
        when(orderRepository.existsByReferenceAndTenantId(any(), any())).thenReturn(false);
        OrderRequest request = requestWithItems(List.of(
                new OrderItemRequest(product.getId(), "Produit X", null, 5, new BigDecimal("10.00"))));

        when(productRepository.findByIdAndMerchantIdAndTenantId(product.getId(), merchantId, tenantId))
                .thenReturn(Optional.of(product));
        when(stockMovementRepository.currentStock(product.getId())).thenReturn(2);

        assertThatThrownBy(() -> orderService.create(request, tenantId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        verify(orderRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void create_withMultipleItemsSameProduct_aggregatesQuantityForStockCheck() {
        when(merchantRepository.findByIdAndTenantId(merchantId, tenantId)).thenReturn(Optional.of(merchant));
        Product product = buildProduct(UUID.randomUUID());
        OrderRequest request = requestWithItems(List.of(
                new OrderItemRequest(product.getId(), "Produit X", null, 3, new BigDecimal("10.00")),
                new OrderItemRequest(product.getId(), "Produit X", null, 3, new BigDecimal("10.00"))));

        when(productRepository.findByIdAndMerchantIdAndTenantId(product.getId(), merchantId, tenantId))
                .thenReturn(Optional.of(product));
        when(stockMovementRepository.currentStock(product.getId())).thenReturn(5);

        assertThatThrownBy(() -> orderService.create(request, tenantId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_withUnknownProduct_throwsNotFound() {
        UUID unknownProductId = UUID.randomUUID();
        when(merchantRepository.findByIdAndTenantId(merchantId, tenantId)).thenReturn(Optional.of(merchant));
        OrderRequest request = requestWithItems(List.of(
                new OrderItemRequest(unknownProductId, "Produit X", null, 1, new BigDecimal("10.00"))));

        when(productRepository.findByIdAndMerchantIdAndTenantId(unknownProductId, merchantId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request, tenantId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    // ── cancel ──────────────────────────────────────────────────────────────

    @Test
    void cancel_withProductLinkedItems_restoresStockWithOrderId() {
        Product product = buildProduct(UUID.randomUUID());
        UUID orderId = UUID.randomUUID();
        UUID cancelledBy = UUID.randomUUID();
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .product(product)
                .name("Produit X")
                .quantity(4)
                .unitPrice(new BigDecimal("10.00"))
                .build();
        Order order = Order.builder()
                .id(orderId)
                .tenant(tenant)
                .merchant(merchant)
                .reference("ORD-1")
                .status(Order.Status.PENDING)
                .totalAmount(new BigDecimal("40.00"))
                .items(List.of(item))
                .build();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancel(orderId, tenantId, cancelledBy);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement movement = captor.getValue();
        assertThat(movement.getType()).isEqualTo(StockMovement.Type.IN);
        assertThat(movement.getQuantity()).isEqualTo(4);
        assertThat(movement.getOrderId()).isEqualTo(orderId);
        assertThat(movement.getCreatedBy()).isEqualTo(cancelledBy);
        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);
    }

    @Test
    void cancel_delivered_throwsConflict_andDoesNotTouchStock() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .tenant(tenant)
                .merchant(merchant)
                .reference("ORD-1")
                .status(Order.Status.DELIVERED)
                .totalAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(orderId, tenantId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        verify(stockMovementRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_alreadyCancelled_isIdempotent_doesNotDuplicateStockMovement() {
        UUID orderId = UUID.randomUUID();
        Product product = buildProduct(UUID.randomUUID());
        OrderItem item = OrderItem.builder().id(UUID.randomUUID()).product(product).quantity(2)
                .unitPrice(BigDecimal.TEN).build();
        Order order = Order.builder()
                .id(orderId)
                .tenant(tenant)
                .merchant(merchant)
                .reference("ORD-1")
                .status(Order.Status.CANCELLED)
                .totalAmount(BigDecimal.ZERO)
                .items(List.of(item))
                .build();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        orderService.cancel(orderId, tenantId, UUID.randomUUID());

        verify(stockMovementRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }
}
