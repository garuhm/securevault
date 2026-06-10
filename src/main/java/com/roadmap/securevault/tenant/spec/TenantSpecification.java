package com.roadmap.securevault.tenant.spec;

import com.roadmap.securevault.common.spec.EntitySpecification;
import com.roadmap.securevault.tenant.dto.tenant.TenantFilter;
import com.roadmap.securevault.tenant.entity.Tenant;
import org.springframework.data.jpa.domain.Specification;

public class TenantSpecification {

    public static Specification<Tenant> fromFilter(TenantFilter filter) {
        return Specification
                .<Tenant>where(EntitySpecification.equal("status", filter.status()))
                .and(EntitySpecification.like("companyName", filter.companyName()))
                .and(EntitySpecification.like("companyCode", filter.companyCode()));
    }
}