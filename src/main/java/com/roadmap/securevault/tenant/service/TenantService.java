package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.exception.CredentialsTakenException;
import com.roadmap.securevault.common.exception.InvalidStateException;
import com.roadmap.securevault.common.service.NotificationService;
import com.roadmap.securevault.tenant.dto.tenant.TenantApprovalResponse;
import com.roadmap.securevault.tenant.dto.tenant.TenantFilter;
import com.roadmap.securevault.tenant.dto.tenant.TenantRegistrationRequest;
import com.roadmap.securevault.tenant.dto.tenant.TenantResponse;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import com.roadmap.securevault.tenant.mapper.TenantMapper;
import com.roadmap.securevault.tenant.repo.TenantRepository;
import com.roadmap.securevault.tenant.spec.TenantSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;
    private final BootstrapTokenService bootstrapTokenService;
    private final TenantRefreshJwtService tenantRefreshJwtService;
    private final TenantStatusService tenantStatusService;
    private final TenantSchemaInitializer tenantSchemaInitializer;
    private final NotificationService notificationService;

    public Page<TenantResponse> getTenants(TenantFilter filter, Pageable pageable) {
        return tenantRepository.findAll(TenantSpecification.fromFilter(filter), pageable)
                .map(TenantMapper::toResponse);
    }

    public TenantResponse getTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(TenantMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    }

    public void registerTenant(TenantRegistrationRequest request) {
        if (tenantRepository.existsByCompanyCode(request.companyCode())) {
            throw new CredentialsTakenException("Company code already exists");
        }

        Tenant tenant = Tenant.builder()
                .companyCode(request.companyCode())
                .companyName(request.companyName())
                .ownerEmail(request.ownerEmail())
                .build();
        Tenant saved = tenantRepository.saveAndFlush(tenant);
        saved.setSchemaName("tenant_" + saved.getId().toString().replace("-", ""));

        tenantRepository.save(saved);
    }

    public TenantApprovalResponse approveTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        if (tenant.getStatus() != TenantStatus.PENDING) {
            throw new InvalidStateException("Tenant is not in pending status");
        }

        Tenant approved = tenantStatusService.markAsApproved(tenant);
        tenantSchemaInitializer.migrateSchema(approved.getSchemaName());

        String bootstrapToken = bootstrapTokenService.generateToken(approved.getId());
        notificationService.sendBootstrapLink(approved.getOwnerEmail(), bootstrapToken);

        return new TenantApprovalResponse(
                approved.getCompanyName(),
                approved.getCompanyCode(),
                bootstrapToken
        );
    }

    public void suspendTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            throw new InvalidStateException("Tenant is already suspended");
        }

        tenantStatusService.markAsSuspended(tenant);
        tenantRefreshJwtService.revokeAllTenantTokens(tenant.getSchemaName());
    }

    public void unsuspendTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        if (tenant.getStatus() != TenantStatus.SUSPENDED) {
            throw new InvalidStateException("Tenant is not suspended");
        }

        tenantStatusService.markAsApproved(tenant);
    }
}
