package com.roadmap.securevault.entity.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static int getRoleHierarchyPosition(RoleName roleName) {
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

    public static boolean isRoleAbove (RoleName roleName, RoleName otherRole) {
        return otherRole.ordinal() < roleName.ordinal();
    }

    public static boolean isRoleAbove (Set<RoleName> roleNames, RoleName otherRole) {
        return roleNames.stream().anyMatch(r -> isRoleAbove(r, otherRole));
    }

    public static Set<RoleName> combineRoles(Set<RoleName> original, Set<RoleName> newRoles) {
        return Stream.concat(original.stream(), newRoles.stream())
                .collect(Collectors.toSet());
    }

    private static boolean isRoleBelow (RoleName roleName, RoleName otherRole) {
        return otherRole.ordinal() > roleName.ordinal();
    }
}