package com.roadmap.securevault.tenant.filter;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.filter.BaseJwtFilter;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.tenant.entity.Tenant;
import com.roadmap.securevault.tenant.service.TenantUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class TenantJwtFilter extends BaseJwtFilter {
    public TenantJwtFilter(AccessJwtService accessJwtService,
                             TenantUserService userService,
                             CookieService cookieService,
                             CookieProperties cookieProperties) {
        super(accessJwtService, userService, cookieService, cookieProperties);
    }

    @Override
    protected boolean additionalValidation(String token,
                                           UserDetails user,
                                           HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        if (tenant == null) return false;

        String tokenTenantId = accessJwtService.extractClaim(token, "tenantId");
        return tenant.getId().toString().equals(tokenTenantId);
    }
}

