package com.assignment.booking.specification;

import com.assignment.booking.entity.Reservation;
import com.assignment.booking.enums.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> withFilters(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String username) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (username != null && !username.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("username"), username));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
