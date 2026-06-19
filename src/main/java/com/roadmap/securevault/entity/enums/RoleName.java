package com.roadmap.securevault.entity.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public enum RoleName {
    ROLE_OWNER,
    ROLE_ADMIN,
    ROLE_USER;

    public static RoleName getHighestRole (Collection<RoleName> roleNames) {
        return Arrays.stream(RoleName.values())
                .filter(r -> roleNames.contains(r))
                .min(RoleName::compareTo)
                .orElse(ROLE_USER);
    }

    public static int getRoleHiearchyPosition (RoleName roleName) {
        return roleName.ordinal();
    }

    public static Set<RoleName> getRolesBelow(RoleName roleName) {
        return Arrays.stream(RoleName.values())
                .filter(r -> isRoleBelow(roleName, r))
                .collect(Collectors.toSet());
    }

    public static RoleName tryParseRole(String authority) {
        try {
            return RoleName.valueOf(authority);
        } catch (IllegalArgumentException e) {
            return null; // not one of our roles (e.g. offline_access, uma_authorization)
        }
    }

    private static boolean isRoleBelow (RoleName roleName, RoleName otherRole) {
        return otherRole.ordinal() > roleName.ordinal();
    }
}