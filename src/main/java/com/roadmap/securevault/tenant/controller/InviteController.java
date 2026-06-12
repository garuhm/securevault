package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.tenant.dto.invite.InviteCreateRequest;
import com.roadmap.securevault.tenant.dto.invite.InviteResponse;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/t/{companyCode}/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<InviteResponse> createInvite(
            @Valid @RequestBody InviteCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(inviteService.createInvite(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Page<InviteResponse>> getInvites(
            @RequestParam(required = false, defaultValue = "false") Boolean used,
            @RequestParam(required = false) TenantRole role,
            @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(inviteService.getInvites(used, role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'TENANT_ADMIN') " +
                  "or @securityEvaluator.isOwner(authentication, 'inviteCode', #id)")
    public ResponseEntity<InviteResponse> getInviteById(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(inviteService.getInviteById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_OWNER') " +
                  "or @securityEvaluator.isOwner(authentication, 'inviteCode', #id)")
    public ResponseEntity<Void> revokeInvite(@PathVariable java.util.UUID id) {
        inviteService.revokeInvite(id);
        return ResponseEntity.noContent().build();
    }
}