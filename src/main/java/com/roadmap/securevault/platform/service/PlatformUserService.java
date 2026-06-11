package com.roadmap.securevault.platform.service;

import com.roadmap.securevault.common.service.BaseUserService;
import com.roadmap.securevault.platform.dto.PlatformUserFilter;
import com.roadmap.securevault.platform.dto.PlatformUserResponse;
import com.roadmap.securevault.platform.dto.PlatformUserUpdateRequest;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.mapper.PlatformUserMapper;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import com.roadmap.securevault.platform.spec.PlatformUserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PlatformUserService extends BaseUserService<PlatformUser, PlatformUserRepository, PlatformUserResponse, PlatformUserUpdateRequest> {

    public PlatformUserService(PlatformUserRepository userRepository, PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
    }

    @Override
    protected PlatformUserResponse toResponse(PlatformUser user) {
        return PlatformUserMapper.toResponse(user);
    }

    @Override
    protected boolean isOwnerRole(PlatformUser user) {
        return user.getRole() == PlatformRole.PLATFORM_OWNER;
    }

    public Page<PlatformUserResponse> getUsers(PlatformUserFilter filter, Pageable pageable) {
        return getUsers(PlatformUserSpecification.fromFilter(filter), pageable);
    }
}