package com.assignment.booking.specification;

import com.assignment.booking.entity.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ResourceSpecification {

    private ResourceSpecification() {
    }

    public static Specification<Resource> withFilters(String type, Boolean available) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null && !type.isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("type")), type.toLowerCase()));
            }

            if (available != null) {
                predicates.add(criteriaBuilder.equal(root.get("available"), available));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
