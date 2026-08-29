package com.assignment.booking.repository;

import com.assignment.booking.entity.Reservation;
import com.assignment.booking.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    List<Reservation> findByUserId(Long userId);

    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.status = :status")
    Page<Reservation> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ReservationStatus status, Pageable pageable);

    boolean existsByResourceIdAndStatusNotAndStartTimeBeforeAndEndTimeAfter(
            Long resourceId, ReservationStatus status,
            java.time.LocalDateTime endTime, java.time.LocalDateTime startTime);

    boolean existsByResourceIdAndIdNotAndStatusNotAndStartTimeBeforeAndEndTimeAfter(
            Long resourceId, Long id, ReservationStatus status,
            java.time.LocalDateTime endTime, java.time.LocalDateTime startTime);

    boolean existsByResourceId(Long resourceId);
}
