package com.roadmap.securevault.platform.filter;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.filter.BaseJwtFilter;
import com.roadmap.securevault.common.service.AccessJwtService;
import com.roadmap.securevault.common.service.CookieService;
import com.roadmap.securevault.platform.service.PlatformUserService;
import org.springframework.stereotype.Component;

@Component
public class PlatformJwtFilter extends BaseJwtFilter {

    public PlatformJwtFilter(AccessJwtService accessJwtService,
                             PlatformUserService userService,
                             CookieService cookieService,
                             CookieProperties cookieProperties) {
        super(accessJwtService, userService, cookieService, cookieProperties);
    }
}