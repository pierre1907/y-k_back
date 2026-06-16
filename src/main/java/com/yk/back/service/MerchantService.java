package com.yk.back.service;

import com.yk.back.dto.request.MerchantRequest;
import com.yk.back.dto.response.MerchantResponse;
import com.yk.back.entity.Merchant;
import com.yk.back.entity.Tenant;
import com.yk.back.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.MerchantRepository;
import com.yk.back.repository.TenantRepository;
import com.yk.back.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final TenantRepository tenantRepository;
    private final MailService mailService;

    public List<MerchantResponse> listAll() {
        return merchantRepository.findAll().stream()
                .map(MerchantResponse::from)
                .toList();
    }

    public List<MerchantResponse> listByTenant(UUID tenantId) {
        return merchantRepository.findAllByTenantId(tenantId).stream()
                .map(MerchantResponse::from)
                .toList();
    }

    public MerchantResponse getById(UUID id) {
        return MerchantResponse.from(findOrThrow(id));
    }

    @Transactional
    public MerchantResponse create(MerchantRequest request, String adminEmail, String adminName) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantId().toString()));

        String slug = resolveSlug(request.slug(), request.name());
        if (merchantRepository.existsBySlugAndTenantId(slug, tenant.getId())) {
            throw new BusinessException("Un merchant avec le slug '" + slug + "' existe déjà pour ce tenant", HttpStatus.CONFLICT);
        }

        Merchant merchant = Merchant.builder()
                .tenant(tenant)
                .name(request.name())
                .slug(slug)
                .build();

        merchant = merchantRepository.save(merchant);
        mailService.sendMerchantCreated(adminEmail, adminName, merchant.getName(), tenant.getName());

        return MerchantResponse.from(merchant);
    }

    @Transactional
    public MerchantResponse update(UUID id, MerchantRequest request) {
        Merchant merchant = findOrThrow(id);
        String slug = resolveSlug(request.slug(), request.name());

        if (!slug.equals(merchant.getSlug())
                && merchantRepository.existsBySlugAndTenantId(slug, merchant.getTenant().getId())) {
            throw new BusinessException("Un merchant avec le slug '" + slug + "' existe déjà pour ce tenant", HttpStatus.CONFLICT);
        }

        merchant.setName(request.name());
        merchant.setSlug(slug);
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Transactional
    public MerchantResponse activate(UUID id) {
        Merchant merchant = findOrThrow(id);
        merchant.setActive(true);
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Transactional
    public MerchantResponse deactivate(UUID id) {
        Merchant merchant = findOrThrow(id);
        merchant.setActive(false);
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    private Merchant findOrThrow(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", id.toString()));
    }

    private String resolveSlug(String provided, String name) {
        if (provided != null && !provided.isBlank()) {
            return provided.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
