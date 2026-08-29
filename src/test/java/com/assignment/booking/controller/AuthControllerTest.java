package com.assignment.booking.controller;

import com.assignment.booking.dto.request.LoginRequest;
import com.assignment.booking.dto.request.RegisterRequest;
import com.assignment.booking.dto.response.ApiResponse;
import com.assignment.booking.dto.response.LoginResponse;
import com.assignment.booking.dto.response.UserResponse;
import com.assignment.booking.exception.BadRequestException;
import com.assignment.booking.exception.DuplicateResourceException;
import com.assignment.booking.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Admin@123");
        LoginResponse loginResponse = LoginResponse.builder()
                .token("test-jwt-token")
                .type("Bearer")
                .id(1L)
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_InvalidCredentials_ThrowsException() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_MissingFields_Returns400() throws Exception {
        String requestJson = "{\"username\":\"\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "Pass@123", "new@example.com", "New User");
        UserResponse userResponse = UserResponse.builder()
                .id(2L)
                .username("newuser")
                .email("new@example.com")
                .fullName("New User")
                .enabled(true)
                .roles(List.of("ROLE_USER"))
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void register_DuplicateUsername_ThrowsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest("admin", "Pass@123", null, null);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User with username 'admin' already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_MissingFields_Returns400() throws Exception {
        String requestJson = "{\"username\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}
