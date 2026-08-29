package com.assignment.booking.controller;

import com.assignment.booking.dto.request.LoginRequest;
import com.assignment.booking.dto.request.ReservationRequest;
import com.assignment.booking.dto.request.ResourceRequest;
import com.assignment.booking.dto.response.*;
import com.assignment.booking.enums.ReservationStatus;
import com.assignment.booking.service.AuthService;
import com.assignment.booking.service.ReservationService;
import com.assignment.booking.service.ResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private ReservationService reservationService;

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Admin@123");

        LoginResponse loginResponse = LoginResponse.builder()
                .token("jwt-token")
                .type("Bearer")
                .id(1L)
                .username("admin")
                .roles(List.of("ROLE_ADMIN", "ROLE_USER"))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_InvalidCredentials_ReturnsError() throws Exception {
        LoginRequest request = new LoginRequest("wronguser", "wrongpass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_EmptyUsername_ReturnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "Admin@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createResource_Admin_Success() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(50))
                .build();

        ResourceResponse response = ResourceResponse.builder()
                .id(1L)
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(50))
                .build();

        when(resourceService.createResource(any(ResourceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Room"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createResource_User_Forbidden() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(50))
                .build();

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResource_PublicAccess_Success() throws Exception {
        ResourceResponse response = ResourceResponse.builder()
                .id(1L)
                .name("Conference Room")
                .type("ROOM")
                .build();

        when(resourceService.getResourceById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Conference Room"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteResource_Admin_Success() throws Exception {
        mockMvc.perform(delete("/api/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteResource_User_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/resources/1"))
                .andExpect(status().isForbidden());
    }
}
