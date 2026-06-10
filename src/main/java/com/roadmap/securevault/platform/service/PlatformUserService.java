package com.roadmap.securevault.platform.service;

import com.roadmap.securevault.common.service.BaseUserService;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import org.springframework.stereotype.Service;

@Service
public class PlatformUserService extends BaseUserService<PlatformUser, PlatformUserRepository> {

    public PlatformUserService(PlatformUserRepository userRepository) {
        super(userRepository);
    }
}