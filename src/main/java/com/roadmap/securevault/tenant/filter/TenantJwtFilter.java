package com.roadmap.securevault.tenant.filter;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.filter.BaseJwtFilter;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.tenant.service.TenantUserService;
import org.springframework.stereotype.Component;

@Component
public class TenantJwtFilter extends BaseJwtFilter {
    public TenantJwtFilter(AccessJwtService accessJwtService,
                             TenantUserService userService,
                             CookieService cookieService,
                             CookieProperties cookieProperties) {
        super(accessJwtService, userService, cookieService, cookieProperties);
    }
}
