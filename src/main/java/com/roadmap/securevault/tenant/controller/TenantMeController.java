package com.roadmap.securevault.tenant.controller;

import com.roadmap.securevault.common.controller.BaseMeController;
import com.roadmap.securevault.multitenancy.TenantContext;
import com.roadmap.securevault.tenant.dto.tenant.TenantMeResponse;
import com.roadmap.securevault.tenant.entity.TenantUser;
import com.roadmap.securevault.tenant.mapper.TenantUserMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/t/{companyCode}")
public class TenantMeController extends BaseMeController<TenantUser, TenantMeResponse> {

    @Override
    protected TenantMeResponse toMeResponse(TenantUser user) {
        // need companyCode from TenantContext or request attribute
        String companyCode = TenantContext.getCompanyCode();
        return TenantUserMapper.toMeResponse(user, companyCode);
    }
}