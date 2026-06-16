package com.yk.back.service;

import com.yk.back.dto.request.PlanRequest;
import com.yk.back.dto.response.PlanResponse;
import com.yk.back.entity.Plan;
import com.yk.back.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.yk.back.exception.ResourceNotFoundException;
import com.yk.back.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanResponse> listAll() {
        return planRepository.findAll().stream().map(PlanResponse::from).toList();
    }

    public List<PlanResponse> listActive() {
        return planRepository.findAllByIsActiveTrue().stream().map(PlanResponse::from).toList();
    }

    public PlanResponse getById(UUID id) {
        return PlanResponse.from(findOrThrow(id));
    }

    @Transactional
    public PlanResponse create(PlanRequest request) {
        if (planRepository.existsByName(request.name())) {
            throw new BusinessException("Un plan nommé '" + request.name() + "' existe déjà", HttpStatus.CONFLICT);
        }
        Plan plan = Plan.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .billingCycle(request.billingCycle())
                .maxMerchants(request.maxMerchants())
                .maxUsersPerMerchant(request.maxUsersPerMerchant())
                .build();
        return PlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        Plan plan = findOrThrow(id);
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setBillingCycle(request.billingCycle());
        plan.setMaxMerchants(request.maxMerchants());
        plan.setMaxUsersPerMerchant(request.maxUsersPerMerchant());
        return PlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse toggleActive(UUID id) {
        Plan plan = findOrThrow(id);
        plan.setActive(!plan.isActive());
        return PlanResponse.from(planRepository.save(plan));
    }

    private Plan findOrThrow(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id.toString()));
    }
}
