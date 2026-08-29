package com.assignment.booking.service;

import com.assignment.booking.dto.request.ReservationRequest;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ReservationResponse;
import com.assignment.booking.entity.Reservation;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.ReservationStatus;
import com.assignment.booking.exception.BadRequestException;
import com.assignment.booking.exception.ReservationConflictException;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.exception.UnauthorizedAccessException;
import com.assignment.booking.mapper.EntityMapper;
import com.assignment.booking.repository.ReservationRepository;
import com.assignment.booking.repository.ResourceRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Resource resource;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        resource = Resource.builder()
                .id(1L)
                .name("Conference Room")
                .type("ROOM")
                .available(true)
                .pricePerUnit(BigDecimal.valueOf(50.00))
                .build();

        reservation = Reservation.builder()
                .id(1L)
                .resource(resource)
                .user(user)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(100))
                .status(ReservationStatus.PENDING)
                .build();

        setupSecurityContext("testuser", false);
    }

    private void setupSecurityContext(String username, boolean isAdmin) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, "password", authorities);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createReservation_Success() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(100))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsByResourceIdAndStatusNotAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), any(), any(), any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(entityMapper.toReservationResponse(any(Reservation.class))).thenReturn(ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .resourceName("Conference Room")
                .userId(1L)
                .username("testuser")
                .price(BigDecimal.valueOf(100))
                .status("PENDING")
                .build());

        ReservationResponse response = reservationService.createReservation(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservation_ResourceNotAvailable_ThrowsException() {
        resource.setAvailable(false);
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(100))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservation_InvalidTimeRange_ThrowsException() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(100))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void getReservationById_UserOwnReservation_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(entityMapper.toReservationResponse(reservation)).thenReturn(ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .userId(1L)
                .username("testuser")
                .build());

        ReservationResponse response = reservationService.getReservationById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getReservationById_UserNotOwner_ThrowsException() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .build();
        reservation.setUser(otherUser);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(UnauthorizedAccessException.class, () -> reservationService.getReservationById(1L));
    }

    @Test
    void getReservationById_AdminCanAccessAll_Success() {
        setupSecurityContext("admin", true);

        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .build();
        reservation.setUser(otherUser);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(entityMapper.toReservationResponse(reservation)).thenReturn(ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .userId(2L)
                .username("otheruser")
                .build());

        ReservationResponse response = reservationService.getReservationById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void deleteReservation_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(1L);

        verify(reservationRepository).deleteById(1L);
    }

    @Test
    void deleteReservation_UserNotOwner_ThrowsException() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .build();
        reservation.setUser(otherUser);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(UnauthorizedAccessException.class, () -> reservationService.deleteReservation(1L));
    }

    @Test
    void getReservations_UserOwnOnly_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        ReservationResponse reservationResponse = ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .resourceName("Conference Room")
                .userId(1L)
                .username("testuser")
                .price(BigDecimal.valueOf(100))
                .status("PENDING")
                .build();

        Page<Reservation> page = new PageImpl<>(List.of(reservation), pageable, 1);

        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(entityMapper.toReservationResponse(reservation)).thenReturn(reservationResponse);

        PageResponse<ReservationResponse> response = reservationService.getReservations(
                null, null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getId());
    }

    @Test
    void getReservations_AdminCanSeeAll_Success() {
        setupSecurityContext("admin", true);

        Pageable pageable = PageRequest.of(0, 10);
        ReservationResponse reservationResponse = ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .userId(1L)
                .username("otheruser")
                .price(BigDecimal.valueOf(100))
                .status("PENDING")
                .build();

        Page<Reservation> page = new PageImpl<>(List.of(reservation), pageable, 1);

        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(entityMapper.toReservationResponse(reservation)).thenReturn(reservationResponse);

        PageResponse<ReservationResponse> response = reservationService.getReservations(
                null, null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void updateReservation_Success() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .price(BigDecimal.valueOf(150))
                .status(ReservationStatus.CONFIRMED)
                .build();

        Reservation updatedReservation = Reservation.builder()
                .id(1L)
                .resource(resource)
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.CONFIRMED)
                .build();

        ReservationResponse updatedResponse = ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .userId(1L)
                .username("testuser")
                .price(BigDecimal.valueOf(150))
                .status("CONFIRMED")
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(updatedReservation);
        when(entityMapper.toReservationResponse(updatedReservation)).thenReturn(updatedResponse);

        ReservationResponse response = reservationService.updateReservation(1L, request);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void updateReservation_UserNotOwner_ThrowsException() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .build();
        reservation.setUser(otherUser);

        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .price(BigDecimal.valueOf(150))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(UnauthorizedAccessException.class, () -> reservationService.updateReservation(1L, request));
    }

    @Test
    void updateReservation_InvalidTimeRange_ThrowsException() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .endTime(LocalDateTime.now().plusDays(2))
                .price(BigDecimal.valueOf(150))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertThrows(BadRequestException.class, () -> reservationService.updateReservation(1L, request));
    }

    @Test
    void updateReservation_NegativePrice_ThrowsException() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .price(BigDecimal.valueOf(-10))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        assertThrows(BadRequestException.class, () -> reservationService.updateReservation(1L, request));
    }

    @Test
    void updateReservation_ConflictWithOtherReservation_ThrowsException() {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .price(BigDecimal.valueOf(150))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsByResourceIdAndIdNotAndStatusNotAndStartTimeBeforeAndEndTimeAfter(
                anyLong(), anyLong(), any(), any(), any())).thenReturn(true);

        assertThrows(ReservationConflictException.class, () -> reservationService.updateReservation(1L, request));
    }
}
