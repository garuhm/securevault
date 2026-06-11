package com.roadmap.securevault.platform.repo;

import com.roadmap.securevault.common.repo.BaseUserRepository;
import com.roadmap.securevault.platform.entity.PlatformUser;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlatformUserRepository extends
        BaseUserRepository<PlatformUser>,
        JpaSpecificationExecutor<PlatformUser> {
}