package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantStatusService {

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant markAsApproved(Tenant tenant) {
        tenant.setStatus(TenantStatus.APPROVED);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant markAsSuspended(Tenant tenant) {
        tenant.setStatus(TenantStatus.SUSPENDED);
        return tenantRepository.save(tenant);
    }
}
