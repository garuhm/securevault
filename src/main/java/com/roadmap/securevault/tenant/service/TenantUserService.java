package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.service.BaseUserService;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserFilter;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserResponse;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserUpdateRequest;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.mapper.TenantUserMapper;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import com.roadmap.securevault.tenant.spec.TenantUserSpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantUserService extends BaseUserService<TenantUser, TenantUserRepository, TenantUserResponse, TenantUserUpdateRequest> {

    public TenantUserService(TenantUserRepository userRepository, PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
    }

    @Override
    protected TenantUserResponse toResponse(TenantUser user) {
        return TenantUserMapper.toResponse(user);
    }

    @Override
    protected boolean isOwnerRole(TenantUser user) {
        return user.getRole() == TenantRole.TENANT_OWNER;
    }

    public Page<TenantUserResponse> getUsers(TenantUserFilter filter, Pageable pageable) {
        return getUsers(TenantUserSpecification.fromFilter(filter), pageable);
    }

    @Transactional
    public void updateRole(UUID userId, TenantRole role) {
        TenantUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setRole(role);
        userRepository.save(user);
    }
}