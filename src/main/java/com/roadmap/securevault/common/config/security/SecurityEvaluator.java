package com.roadmap.securevault.common.config.security;

import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.common.entity.UserOwnable;
import com.roadmap.securevault.platform.entity.PlatformUser;
import com.roadmap.securevault.platform.entity.enums.PlatformRole;
import com.roadmap.securevault.platform.repo.PlatformUserRepository;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import com.roadmap.securevault.tenant.repo.TenantUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityEvaluator {

    private final ApplicationContext applicationContext;

    public boolean isSelf(Authentication authentication, UUID targetUserId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        BaseUser currentUser = (BaseUser) authentication.getPrincipal();
        return currentUser.getId().equals(targetUserId);
    }

    public boolean isOwner(Authentication authentication, String entityName, UUID id) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        BaseUser currentUser = (BaseUser) authentication.getPrincipal();

        String repositoryBeanName = entityName + "Repository";
        JpaRepository<? extends UserOwnable<?>, UUID> repository =
                (JpaRepository<? extends UserOwnable<?>, UUID>) applicationContext.getBean(repositoryBeanName);

        return repository.findById(id)
                .map(e -> e.getUser().getId().equals(currentUser.getId()))
                .orElse(false);
    }

    public boolean isAbove(Authentication authentication, UUID targetUserId, Class<?> roleClass) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        GrantedAuthority currentRole = authentication.getAuthorities()
                .stream().findFirst().orElse(null);
        if (currentRole == null) return false;

        if (roleClass == PlatformRole.class) {
            PlatformUser targetUser = applicationContext
                    .getBean(PlatformUserRepository.class)
                    .findById(targetUserId)
                    .orElse(null);
            if (targetUser == null) return false;

            PlatformRole callerRole = PlatformRole.valueOf(currentRole.getAuthority());
            return platformRoleOrdinal(callerRole) < platformRoleOrdinal(targetUser.getRole());
        }

        if (roleClass == TenantRole.class) {
            TenantUser targetUser = applicationContext
                    .getBean(TenantUserRepository.class)
                    .findById(targetUserId)
                    .orElse(null);
            if (targetUser == null) return false;

            TenantRole callerRole = TenantRole.valueOf(currentRole.getAuthority());
            return tenantRoleOrdinal(callerRole) < tenantRoleOrdinal(targetUser.getRole());
        }
        return false;
    }

    private int platformRoleOrdinal(PlatformRole role) {
        return switch (role) {
            case PLATFORM_OWNER -> 0;
            case PLATFORM_ADMIN -> 1;
            case PLATFORM_SUPPORT -> 2;
        };
    }

    private int tenantRoleOrdinal(TenantRole role) {
        return switch (role) {
            case TENANT_OWNER -> 0;
            case TENANT_ADMIN -> 1;
            case TENANT_MEMBER -> 2;
        };
    }
}
