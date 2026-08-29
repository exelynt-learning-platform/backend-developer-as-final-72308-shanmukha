package com.assignment.booking.controller;

import com.assignment.booking.dto.request.ReservationRequest;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ReservationResponse;
import com.assignment.booking.enums.ReservationStatus;
import com.assignment.booking.exception.BadRequestException;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.exception.UnauthorizedAccessException;
import com.assignment.booking.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ReservationResponse reservationResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();

        reservationResponse = ReservationResponse.builder()
                .id(1L)
                .resourceId(1L)
                .resourceName("Conference Room")
                .userId(1L)
                .username("testuser")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(100))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getReservations_Success() throws Exception {
        PageResponse<ReservationResponse> pageResponse = PageResponse.<ReservationResponse>builder()
                .content(List.of(reservationResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(reservationService.getReservations(any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/reservations")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("testuser"));
    }

    @Test
    void getReservationById_Success() throws Exception {
        when(reservationService.getReservationById(1L)).thenReturn(reservationResponse);

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getReservationById_NotFound_Returns404() throws Exception {
        when(reservationService.getReservationById(999L))
                .thenThrow(new ResourceNotFoundException("Reservation", 999L));

        mockMvc.perform(get("/api/reservations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReservationById_Unauthorized_Returns403() throws Exception {
        when(reservationService.getReservationById(1L))
                .thenThrow(new UnauthorizedAccessException("You can only access your own reservations"));

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReservation_Success() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(100))
                .build();

        when(reservationService.createReservation(any(ReservationRequest.class))).thenReturn(reservationResponse);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void createReservation_InvalidTimeRange_Returns400() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1).plusHours(3))
                .endTime(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(100))
                .build();

        when(reservationService.createReservation(any(ReservationRequest.class)))
                .thenThrow(new BadRequestException("Start time must be before end time"));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReservation_Success() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .price(BigDecimal.valueOf(150))
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

        when(reservationService.updateReservation(any(Long.class), any(ReservationRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/reservations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void deleteReservation_Success() throws Exception {
        doNothing().when(reservationService).deleteReservation(1L);

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createReservation_MissingFields_Returns400() throws Exception {
        String requestJson = "{\"resourceId\":null}";

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_NegativePrice_Returns400() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(-10))
                .build();

        when(reservationService.createReservation(any(ReservationRequest.class)))
                .thenThrow(new BadRequestException("Price must be greater than 0"));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
