package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.service.BaseUserService;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserFilter;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserResponse;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.mapper.TenantUserMapper;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import com.roadmap.securevault.tenant.spec.TenantUserSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public class TenantUserService extends BaseUserService<TenantUser, TenantUserRepository> {
    public TenantUserService(TenantUserRepository userRepository) {
        super(userRepository);
    }

    public Page<TenantUserResponse> getUsers(TenantUserFilter filter, Pageable pageable) {
        return userRepository.findAll(TenantUserSpecification.fromFilter(filter), pageable)
                .map(TenantUserMapper::toResponse);
    }

    public TenantUserResponse getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(TenantUserMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
