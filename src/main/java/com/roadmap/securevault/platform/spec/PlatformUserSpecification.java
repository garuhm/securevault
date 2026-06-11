package com.roadmap.securevault.platform.spec;

import com.roadmap.securevault.common.spec.EntitySpecification;
import com.roadmap.securevault.platform.dto.PlatformUserFilter;
import com.roadmap.securevault.platform.entity.PlatformUser;
import org.springframework.data.jpa.domain.Specification;

public class PlatformUserSpecification {

    public static Specification<PlatformUser> fromFilter(PlatformUserFilter filter) {
        return Specification
                .<PlatformUser>where(EntitySpecification.equal("role", filter.role()))
                .and(EntitySpecification.like("username", filter.username()))
                .and(EntitySpecification.like("email", filter.email()));
    }
}