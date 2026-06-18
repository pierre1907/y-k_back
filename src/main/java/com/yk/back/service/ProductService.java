package com.yk.back.service;

import com.yk.back.dto.request.ProductRequest;
import com.yk.back.dto.request.StockMovementRequest;
import com.yk.back.dto.response.ProductResponse;
import com.yk.back.dto.response.StockMovementResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.Product;
import com.yk.back.entity.StockMovement;
import com.yk.back.exception.BusinessException;
import com.yk.back.exception.ForbiddenException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.ProductRepository;
import com.yk.back.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final MerchantRepository merchantRepository;

    public List<ProductResponse> listByTenant(UUID tenantId) {
        return productRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(p -> ProductResponse.from(p, stockMovementRepository.currentStock(p.getId())))
                .toList();
    }

    public List<ProductResponse> listByMerchant(UUID merchantId, UUID tenantId) {
        return productRepository.findAllByMerchantIdAndTenantIdOrderByNameAsc(merchantId, tenantId).stream()
                .map(p -> ProductResponse.from(p, stockMovementRepository.currentStock(p.getId())))
                .toList();
    }

    public ProductResponse getById(UUID id, UUID tenantId) {
        Product p = findOrThrow(id, tenantId);
        return ProductResponse.from(p, stockMovementRepository.currentStock(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request, UUID tenantId) {
        if (request.sku() != null && productRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
            throw new BusinessException("Ce SKU est déjà utilisé", HttpStatus.CONFLICT);
        }
        Merchant merchant = merchantRepository.findByIdAndTenantId(request.merchantId(), tenantId)
                .orElseThrow(() -> new com.yk.back.exception.ResourceNotFoundException("Merchant", request.merchantId().toString()));

        Product product = Product.builder()
                .tenant(merchant.getTenant())
                .merchant(merchant)
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .category(request.category())
                .unitPrice(request.unitPrice())
                .isActive(true)
                .build();

        return ProductResponse.from(productRepository.save(product), 0);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request, UUID tenantId) {
        Product product = findOrThrow(id, tenantId);
        if (request.sku() != null && !request.sku().equals(product.getSku())
                && productRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
            throw new BusinessException("Ce SKU est déjà utilisé", HttpStatus.CONFLICT);
        }
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setCategory(request.category());
        product.setUnitPrice(request.unitPrice());
        return ProductResponse.from(productRepository.save(product),
                stockMovementRepository.currentStock(id));
    }

    @Transactional
    public void toggleActive(UUID id, UUID tenantId) {
        Product product = findOrThrow(id, tenantId);
        product.setActive(!product.isActive());
        productRepository.save(product);
    }

    @Transactional
    public StockMovementResponse addMovement(UUID productId, StockMovementRequest request,
                                             UUID tenantId, UUID createdBy) {
        Product product = findOrThrow(productId, tenantId);
        StockMovement.Type type = parseType(request.type());

        if (type == StockMovement.Type.OUT) {
            int stock = stockMovementRepository.currentStock(productId);
            if (stock < request.quantity()) {
                throw new BusinessException("Stock insuffisant (disponible: " + stock + ")", HttpStatus.CONFLICT);
            }
        }

        StockMovement movement = StockMovement.builder()
                .tenantId(tenantId)
                .product(product)
                .type(type)
                .quantity(request.quantity())
                .note(request.note())
                .createdBy(createdBy)
                .build();

        return StockMovementResponse.from(stockMovementRepository.save(movement));
    }

    public List<StockMovementResponse> listMovements(UUID productId, UUID tenantId) {
        findOrThrow(productId, tenantId);
        return stockMovementRepository.findAllByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(StockMovementResponse::from).toList();
    }

    public List<StockMovementResponse> listMovementsForMerchant(UUID productId, UUID merchantId, UUID tenantId) {
        productRepository.findByIdAndMerchantIdAndTenantId(productId, merchantId, tenantId)
                .orElseThrow(() -> new ForbiddenException("Produit introuvable ou accès refusé"));
        return stockMovementRepository.findAllByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(StockMovementResponse::from).toList();
    }

    private Product findOrThrow(UUID id, UUID tenantId) {
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ForbiddenException("Produit introuvable ou accès refusé"));
    }

    private StockMovement.Type parseType(String type) {
        try {
            return StockMovement.Type.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Type de mouvement invalide : " + type, HttpStatus.BAD_REQUEST);
        }
    }
}
