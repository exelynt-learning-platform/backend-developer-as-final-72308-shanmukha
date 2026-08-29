package com.assignment.booking.specification;

import com.assignment.booking.entity.Reservation;
import com.assignment.booking.enums.ReservationStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<Reservation> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Path<ReservationStatus> statusPath = mock(Path.class);
    private final Path<BigDecimal> pricePath = mock(Path.class);
    private final Path<Object> userPath = mock(Path.class);
    private final Path<String> usernamePath = mock(Path.class);
    private final Predicate predicate = mock(Predicate.class);

    @Test
    void withFilters_AllFilters_ReturnsConjunction() {
        when(root.get("status")).thenReturn((Path) statusPath);
        when(root.get("price")).thenReturn((Path) pricePath);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.get("username")).thenReturn((Path) usernamePath);
        when(cb.equal(statusPath, ReservationStatus.PENDING)).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(pricePath, BigDecimal.TEN)).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(pricePath, BigDecimal.valueOf(100))).thenReturn(predicate);
        when(cb.equal(usernamePath, "testuser")).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Reservation> spec = ReservationSpecification.withFilters(
                ReservationStatus.PENDING, BigDecimal.TEN, BigDecimal.valueOf(100), "testuser");

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(statusPath, ReservationStatus.PENDING);
        verify(cb).greaterThanOrEqualTo(pricePath, BigDecimal.TEN);
        verify(cb).lessThanOrEqualTo(pricePath, BigDecimal.valueOf(100));
        verify(cb).equal(usernamePath, "testuser");
    }

    @Test
    void withFilters_NullFilters_ReturnsEmptyConjunction() {
        when(cb.and()).thenReturn(predicate);

        Specification<Reservation> spec = ReservationSpecification.withFilters(null, null, null, null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).and();
    }

    @Test
    void withFilters_EmptyUsername_IgnoresUsernameFilter() {
        when(cb.and()).thenReturn(predicate);

        Specification<Reservation> spec = ReservationSpecification.withFilters(null, null, null, "");

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root, never()).get("user");
    }

    @Test
    void withFilters_OnlyStatus_FiltersByStatus() {
        when(root.get("status")).thenReturn((Path) statusPath);
        when(cb.equal(statusPath, ReservationStatus.CONFIRMED)).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Reservation> spec = ReservationSpecification.withFilters(
                ReservationStatus.CONFIRMED, null, null, null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(statusPath, ReservationStatus.CONFIRMED);
    }

    @Test
    void withFilters_OnlyMinMaxPrice_FiltersByPrice() {
        when(root.get("price")).thenReturn((Path) pricePath);
        when(cb.greaterThanOrEqualTo(pricePath, BigDecimal.valueOf(50))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(pricePath, BigDecimal.valueOf(200))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Reservation> spec = ReservationSpecification.withFilters(
                null, BigDecimal.valueOf(50), BigDecimal.valueOf(200), null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(pricePath, BigDecimal.valueOf(50));
        verify(cb).lessThanOrEqualTo(pricePath, BigDecimal.valueOf(200));
    }
}
