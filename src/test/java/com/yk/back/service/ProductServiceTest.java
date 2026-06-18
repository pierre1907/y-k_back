package com.yk.back.service;

import com.yk.back.dto.request.ProductRequest;
import com.yk.back.dto.request.StockMovementRequest;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.Product;
import com.yk.back.entity.StockMovement;
import com.yk.back.entity.Tenant;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private MerchantRepository merchantRepository;

    private ProductService productService;

    private UUID tenantId;
    private UUID merchantId;
    private Tenant tenant;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, stockMovementRepository, merchantRepository);
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
                .sku("SKU-1")
                .unitPrice(new BigDecimal("10.00"))
                .isActive(true)
                .build();
    }

    // ── listAll / getById ──────────────────────────────────────────────────

    @Test
    void listByTenant_returnsProductsWithCurrentStock() {
        Product p = buildProduct(UUID.randomUUID());
        when(productRepository.findAllByTenantIdOrderByNameAsc(tenantId)).thenReturn(List.of(p));
        when(stockMovementRepository.currentStock(p.getId())).thenReturn(5);

        List<com.yk.back.dto.response.ProductResponse> result = productService.listByTenant(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currentStock()).isEqualTo(5);
    }

    @Test
    void getById_unknownProduct_throwsForbidden() {
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id, tenantId))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_withDuplicateSku_throwsConflict() {
        ProductRequest request = new ProductRequest(merchantId, "Produit X", null, "SKU-1", null, BigDecimal.TEN);
        when(productRepository.existsBySkuAndTenantId("SKU-1", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request, tenantId))
                .isInstanceOf(BusinessException.class);

        verify(merchantRepository, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void create_withUnknownMerchant_throwsNotFound() {
        ProductRequest request = new ProductRequest(merchantId, "Produit X", null, "SKU-1", null, BigDecimal.TEN);
        when(productRepository.existsBySkuAndTenantId("SKU-1", tenantId)).thenReturn(false);
        when(merchantRepository.findByIdAndTenantId(merchantId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request, tenantId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesProductWithZeroStock() {
        ProductRequest request = new ProductRequest(merchantId, "Produit X", "desc", "SKU-1", "cat",
                new BigDecimal("12.50"));
        when(productRepository.existsBySkuAndTenantId("SKU-1", tenantId)).thenReturn(false);
        when(merchantRepository.findByIdAndTenantId(merchantId, tenantId)).thenReturn(Optional.of(merchant));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var response = productService.create(request, tenantId);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Produit X");
        assertThat(captor.getValue().getMerchant()).isEqualTo(merchant);
        assertThat(response.currentStock()).isZero();
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void update_withSkuAlreadyUsedByAnother_throwsConflict() {
        Product existing = buildProduct(UUID.randomUUID());
        ProductRequest request = new ProductRequest(merchantId, "Produit Y", null, "SKU-2", null, BigDecimal.TEN);
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndTenantId("SKU-2", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(existing.getId(), request, tenantId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void update_keepingSameSku_doesNotCheckConflict() {
        Product existing = buildProduct(UUID.randomUUID());
        ProductRequest request = new ProductRequest(merchantId, "Produit Y", null, "SKU-1", null, BigDecimal.TEN);
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);
        when(stockMovementRepository.currentStock(existing.getId())).thenReturn(0);

        productService.update(existing.getId(), request, tenantId);

        verify(productRepository, never()).existsBySkuAndTenantId(any(), any());
    }

    // ── toggleActive ────────────────────────────────────────────────────────

    @Test
    void toggleActive_flipsFlag() {
        Product existing = buildProduct(UUID.randomUUID());
        existing.setActive(true);
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));

        productService.toggleActive(existing.getId(), tenantId);

        assertThat(existing.isActive()).isFalse();
        verify(productRepository).save(existing);
    }

    // ── stock movements ────────────────────────────────────────────────────

    @Test
    void addMovement_outWithSufficientStock_succeeds() {
        Product existing = buildProduct(UUID.randomUUID());
        UUID createdBy = UUID.randomUUID();
        StockMovementRequest request = new StockMovementRequest("OUT", 3, "vente");
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(stockMovementRepository.currentStock(existing.getId())).thenReturn(5);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> {
            StockMovement m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(java.time.OffsetDateTime.now());
            return m;
        });

        var response = productService.addMovement(existing.getId(), request, tenantId, createdBy);

        assertThat(response.type()).isEqualTo("OUT");
        assertThat(response.quantity()).isEqualTo(3);
    }

    @Test
    void addMovement_outWithInsufficientStock_throwsConflict() {
        Product existing = buildProduct(UUID.randomUUID());
        StockMovementRequest request = new StockMovementRequest("OUT", 10, null);
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(stockMovementRepository.currentStock(existing.getId())).thenReturn(5);

        assertThatThrownBy(() -> productService.addMovement(existing.getId(), request, tenantId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void addMovement_withInvalidType_throwsBadRequest() {
        Product existing = buildProduct(UUID.randomUUID());
        StockMovementRequest request = new StockMovementRequest("INVALID", 1, null);
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.addMovement(existing.getId(), request, tenantId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addMovement_in_doesNotCheckCurrentStock() {
        Product existing = buildProduct(UUID.randomUUID());
        StockMovementRequest request = new StockMovementRequest("IN", 20, "réception");
        when(productRepository.findByIdAndTenantId(existing.getId(), tenantId)).thenReturn(Optional.of(existing));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> {
            StockMovement m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(java.time.OffsetDateTime.now());
            return m;
        });

        productService.addMovement(existing.getId(), request, tenantId, UUID.randomUUID());

        verify(stockMovementRepository, never()).currentStock(any());
    }

    @Test
    void listMovements_unknownProduct_throwsForbidden() {
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.listMovements(id, tenantId))
                .isInstanceOf(ForbiddenException.class);
    }
}
