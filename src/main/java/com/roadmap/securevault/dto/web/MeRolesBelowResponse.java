package com.roadmap.securevault.dto.web;

import com.roadmap.securevault.entity.enums.RoleName;

import java.util.Set;

public record MeRolesBelowResponse(
        Set<RoleName> rolesBelow
) {
}