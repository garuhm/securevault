package com.roadmap.securevault.tenant.service;

import com.roadmap.securevault.common.config.properties.RedisProperties;
import com.roadmap.securevault.common.exception.CredentialsTakenException;
import com.roadmap.securevault.common.exception.DuplicateInviteException;
import com.roadmap.securevault.common.exception.InvalidInviteTokenException;
import com.roadmap.securevault.common.exception.InvalidStateException;
import com.roadmap.securevault.common.service.NotificationService;
import com.roadmap.securevault.common.spec.EntitySpecification;
import com.roadmap.securevault.tenant.dto.invite.InviteCreateRequest;
import com.roadmap.securevault.tenant.dto.invite.InviteResponse;
import com.roadmap.securevault.tenant.dto.invite.TenantUserRegisterRequest;
import com.roadmap.securevault.tenant.entity.InviteCode;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.mapper.InviteMapper;
import com.roadmap.securevault.tenant.repo.InviteCodeRepository;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteCodeRepository inviteCodeRepository;
    private final InviteTokenService inviteTokenService;
    private final TenantUserRepository tenantUserRepository;
    private final NotificationService notificationService;
    private final RedisProperties redisProperties;

    public Page<InviteResponse> getInvites(Boolean used, TenantRole role, Pageable pageable) {
        Specification<InviteCode> spec = Specification
                .<InviteCode>where(
                        used ? (r, q, cb) -> cb.isNotNull(r.get("usedAt"))
                        : (r, q, cb) -> cb.isNull(r.get("usedAt")))
                .and(EntitySpecification.equal("role", role));

        return inviteCodeRepository.findAll(spec, pageable)
                .map(invite -> InviteMapper.toResponse(invite, null));
    }

    @Transactional
    public InviteResponse getInviteById(UUID id) {
        return inviteCodeRepository.findById(id)
                .map(invite -> InviteMapper.toResponse(invite, null))
                .orElseThrow(() -> new EntityNotFoundException("Invite not found"));
    }

    @Transactional
    public InviteResponse createInvite(InviteCreateRequest request) {
        TenantUser currentUser = (TenantUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        if (tenantUserRepository.existsByEmail(request.email())) {
            throw new CredentialsTakenException("User with this email already exists in this tenant");
        }

        if (inviteCodeRepository.existsByInviteeEmailAndUsedAtIsNull(request.email())) {
            throw new DuplicateInviteException("An active invite for this email already exists");
        }

        InviteCode invite = InviteCode.builder()
                .createdBy(currentUser)
                .role(request.role() != null ? request.role() : TenantRole.TENANT_MEMBER)
                .inviteeEmail(request.email())
                .expiresAt(LocalDateTime.now().plusHours(redisProperties.inviteTokenTtlHours()))
                .build();

        InviteCode saved = inviteCodeRepository.saveAndFlush(invite);

        String token = inviteTokenService.generateToken(saved.getId());
        saved.setCode(token);
        inviteCodeRepository.save(saved);

        notificationService.sendInviteLink(request.email(), token);

        return InviteMapper.toResponse(saved, token);
    }

    @Transactional
    public InviteCode consumeInvite(TenantUserRegisterRequest request) {
        // 1. validate credentials
        if (tenantUserRepository.existsByUsername(request.username())) {
            throw new CredentialsTakenException("Username already exists");
        }
        if (tenantUserRepository.existsByEmail(request.email())) {
            throw new CredentialsTakenException("Email already exists");
        }

        // 2. peek token
        UUID inviteCodeId = inviteTokenService.peekToken(request.inviteCode())
                .orElseThrow(() -> new InvalidInviteTokenException("Invalid or expired invite code"));

        // 3. verify invite record exists and is unused
        InviteCode invite = inviteCodeRepository.findById(inviteCodeId)
                .orElseThrow(() -> new InvalidInviteTokenException("Invite code not found"));

        if (invite.getUsedAt() != null) {
            throw new InvalidInviteTokenException("Invite code has already been used");
        }

        // 4. consume token
        inviteTokenService.consumeToken(request.inviteCode());

        // 5. mark invite as used
        invite.setUsedAt(LocalDateTime.now());
        inviteCodeRepository.save(invite);

        // 6. return invite so auth service can create the user using the role
        return invite; // caller reads invite.getRole()
    }

    @Transactional
    public void revokeInvite(UUID id) {
        InviteCode invite = inviteCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invite not found"));

        if (invite.getUsedAt() != null) {
            throw new InvalidStateException("Cannot revoke an already used invite");
        }

        inviteTokenService.consumeToken(invite.getCode());
        invite.setUsedAt(LocalDateTime.now());
        inviteCodeRepository.save(invite);
    }
}
