package com.roadmap.securevault.tenant.repo;

import com.roadmap.securevault.common.repo.BaseUserRepository;
import com.roadmap.securevault.tenant.entity.TenantUser;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantUserRepository extends
        BaseUserRepository<TenantUser>,
        JpaSpecificationExecutor<TenantUser> {
}