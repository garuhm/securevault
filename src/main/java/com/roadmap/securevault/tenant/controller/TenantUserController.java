package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserFilter;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserResponse;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserUpdateRequest;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.service.TenantUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/t/{companyCode}/users")
@RequiredArgsConstructor
public class TenantUserController {

    private final TenantUserService tenantUserService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'TENANT_ADMIN', 'TENANT_MEMBER')")
    public ResponseEntity<Page<TenantUserResponse>> getUsers(
            @ParameterObject TenantUserFilter filter,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(tenantUserService.getUsers(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'TENANT_ADMIN', 'TENANT_MEMBER')")
    public ResponseEntity<TenantUserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantUserService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityEvaluator.isSelf(authentication, #id) " +
                   "or hasAnyAuthority('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<TenantUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody TenantUserUpdateRequest request) {
        return ResponseEntity.ok(tenantUserService.updateUser(id, request));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('TENANT_OWNER')")
    public ResponseEntity<Void> updateRole(
            @PathVariable UUID id,
            @RequestParam TenantRole role) {

        tenantUserService.updateRole(id, role);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityEvaluator.isAbove(authentication, #id, T(com.roadmap.securevault.tenant.entity.enums.TenantRole))")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        tenantUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}