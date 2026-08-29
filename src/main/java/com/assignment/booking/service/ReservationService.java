package com.assignment.booking.service;

import com.assignment.booking.dto.request.ReservationRequest;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ReservationResponse;
import com.assignment.booking.entity.Reservation;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.ReservationStatus;
import com.assignment.booking.exception.*;
import com.assignment.booking.mapper.EntityMapper;
import com.assignment.booking.repository.ReservationRepository;
import com.assignment.booking.repository.ResourceRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.specification.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.getResourceId()));

        if (!resource.getAvailable()) {
            throw new BadRequestException("Resource is not available for booking");
        }

        if (request.getStartTime().isAfter(request.getEndTime()) ||
                request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        boolean hasConflict = reservationRepository
                .existsByResourceIdAndStatusNotAndStartTimeBeforeAndEndTimeAfter(
                        resource.getId(),
                        ReservationStatus.CANCELLED,
                        request.getEndTime(),
                        request.getStartTime()
                );

        if (hasConflict) {
            throw new ReservationConflictException("Resource is already booked for the selected time period");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(request.getStatus() != null ? request.getStatus() : ReservationStatus.PENDING)
                .notes(request.getNotes())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return entityMapper.toReservationResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getReservations(
            ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
            String sort, Pageable pageable) {

        boolean isAdmin = isAdminUser();
        String username = getCurrentUsername();

        Specification<Reservation> spec = ReservationSpecification.withFilters(
                status, minPrice, maxPrice, isAdmin ? null : username);

        Page<Reservation> page = reservationRepository.findAll(spec, pageable);

        return PageResponse.<ReservationResponse>builder()
                .content(page.getContent().stream()
                        .map(entityMapper::toReservationResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (!isAdminUser() && !reservation.getUser().getUsername().equals(getCurrentUsername())) {
            throw new UnauthorizedAccessException("You can only access your own reservations");
        }

        return entityMapper.toReservationResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (!isAdminUser() && !reservation.getUser().getUsername().equals(getCurrentUsername())) {
            throw new UnauthorizedAccessException("You can only update your own reservations");
        }

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.getResourceId()));

        if (request.getStartTime().isAfter(request.getEndTime()) ||
                request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }
        reservation.setNotes(request.getNotes());

        Reservation updated = reservationRepository.save(reservation);
        return entityMapper.toReservationResponse(updated);
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (!isAdminUser() && !reservation.getUser().getUsername().equals(getCurrentUsername())) {
            throw new UnauthorizedAccessException("You can only delete your own reservations");
        }

        reservationRepository.deleteById(id);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    private boolean isAdminUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
