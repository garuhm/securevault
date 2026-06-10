package com.roadmap.securevault.tenant.repo;

import com.roadmap.securevault.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends
        JpaRepository<Tenant, UUID>,
        JpaSpecificationExecutor<Tenant>
{
    boolean existsByCompanyCode(String companyCode);

    Optional<Tenant> findByCompanyCode(String companyCode);
}
