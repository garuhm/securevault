package com.roadmap.securevault.tenant.spec;

import com.roadmap.securevault.common.spec.EntitySpecification;
import com.roadmap.securevault.tenant.dto.tenant_user.TenantUserFilter;
import com.roadmap.securevault.tenant.entity.TenantUser;
import org.springframework.data.jpa.domain.Specification;

public class TenantUserSpecification {

    public static Specification<TenantUser> fromFilter(TenantUserFilter filter) {
        return Specification
                .<TenantUser>where(EntitySpecification.equal("role", filter.role()))
                .and(EntitySpecification.like("username", filter.username()))
                .and(EntitySpecification.like("email", filter.email()));
    }
}