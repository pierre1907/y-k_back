package com.yk.back.service;

import com.yk.back.dto.response.AdminUserResponse;
import com.yk.back.repository.MerchantUserRepository;
import com.yk.back.repository.TenantUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final TenantUserRepository tenantUserRepository;
    private final MerchantUserRepository merchantUserRepository;

    public List<AdminUserResponse> listAll() {
        List<AdminUserResponse> result = new ArrayList<>();
        tenantUserRepository.findAll().stream()
                .map(AdminUserResponse::fromTenantUser)
                .forEach(result::add);
        merchantUserRepository.findAll().stream()
                .map(AdminUserResponse::fromMerchantUser)
                .forEach(result::add);
        result.sort(Comparator.comparing(AdminUserResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }
}
