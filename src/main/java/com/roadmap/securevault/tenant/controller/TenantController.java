package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.annotation.ApiVersion;
import com.roadmap.securevault.common.annotation.NoApiVersion;
import com.roadmap.securevault.tenant.dto.tenant.TenantApprovalResponse;
import com.roadmap.securevault.tenant.dto.tenant.TenantFilter;
import com.roadmap.securevault.tenant.dto.tenant.TenantRegistrationRequest;
import com.roadmap.securevault.tenant.dto.tenant.TenantResponse;
import com.roadmap.securevault.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor

@RestController
@ApiVersion("v1")
@RequestMapping("/platform/tenants")
public class TenantController {
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'PLATFORM_SUPPORT', 'PLATFORM_OWNER')")
    public ResponseEntity<Page<TenantResponse>> getTenants(
            @ParameterObject TenantFilter filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(tenantService.getTenants(filter, pageable));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'PLATFORM_SUPPORT', 'PLATFORM_OWNER')")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantService.getTenantById(tenantId));
    }

    @NoApiVersion
    @PostMapping("/register")
    public ResponseEntity<Void> registerTenant(@RequestBody TenantRegistrationRequest request) {
        tenantService.registerTenant(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'PLATFORM_OWNER')")
    public ResponseEntity<TenantApprovalResponse> approveTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.approveTenant(id));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'PLATFORM_OWNER')")
    public ResponseEntity<Void> suspendTenant(@PathVariable UUID id) {
        tenantService.suspendTenant(id);
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/{id}/unsuspend")
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'PLATFORM_OWNER')")
    public ResponseEntity<Void> unsuspendTenant(@PathVariable UUID id) {
        tenantService.unsuspendTenant(id);
        return ResponseEntity.status(200).build();
    }
}
