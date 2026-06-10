package com.roadmap.securevault.common.spec;

import org.springframework.data.jpa.domain.Specification;

public class EntitySpecification {

    public static <T> Specification<T> like(String field, String value) {
        return (root, query, cb) -> value == null ? null
                : cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }

    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, cb) -> value == null ? null
                : cb.equal(root.get(field), value);
    }

    public static <T, V extends Comparable<V>> Specification<T> between(String field, V from, V to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from == null) return cb.lessThanOrEqualTo(root.get(field), to);
            if (to == null) return cb.greaterThanOrEqualTo(root.get(field), from);
            return cb.between(root.get(field), from, to);
        };
    }

    public static <T> Specification<T> joinEqual(String join, String field, Object value) {
        return (root, query, cb) -> value == null ? null
                : cb.equal(root.join(join).get(field), value);
    }
}