package com.roadmap.securevault.spec;

import com.roadmap.securevault.dto.user.UserFilter;
import com.roadmap.securevault.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
        public static Specification<User> fromFilter(UserFilter filter) {
        return Specification
                .<User>where(EntitySpecification.like("username", filter.username()))
                .and(EntitySpecification.like("email", filter.email()))
                .and(EntitySpecification.memberOfCollection("roles", filter.includeRoles()))
                .and(EntitySpecification.notMemberOfCollection("roles", filter.excludeRoles()));

    }
}
