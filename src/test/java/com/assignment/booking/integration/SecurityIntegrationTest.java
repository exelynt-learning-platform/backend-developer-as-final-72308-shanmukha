package com.assignment.booking.integration;

import com.assignment.booking.dto.request.LoginRequest;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.RoleName;
import com.assignment.booking.repository.RoleRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.save(Role.builder()
                .name(RoleName.ROLE_USER).description("User").build());
        Role adminRole = roleRepository.save(Role.builder()
                .name(RoleName.ROLE_ADMIN).description("Admin").build());

        User user = userRepository.save(User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("User@123"))
                .email("test@example.com")
                .fullName("Test User")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        User admin = userRepository.save(User.builder()
                .username("testadmin")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@example.com")
                .fullName("Test Admin")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole, adminRole)))
                .build());

        userToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(toUserDetails(user), null,
                        user.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                                .collect(Collectors.toList())));
        adminToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(toUserDetails(admin), null,
                        admin.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                                .collect(Collectors.toList())));
    }

    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                        .collect(Collectors.toList()));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void publicEndpoints_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resources/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoint_Returns403_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_Returns401_WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_AuthenticatedUser_CanAccess() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());
    }

    @Test
    void createResource_UserRole_Returns403() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.assignment.booking.dto.request.ResourceRequest(
                        "Test Room", "desc", "ROOM", new java.math.BigDecimal("50"), null, "loc", "10"));

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_AdminRole_Returns201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.assignment.booking.dto.request.ResourceRequest(
                        "Test Room", "desc", "ROOM", new java.math.BigDecimal("50"), null, "loc", "10"));

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void actuatorHealth_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorBeans_UserRole_Returns403() throws Exception {
        mockMvc.perform(get("/actuator/beans")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actuatorBeans_AdminRole_Returns404_NotExposed() throws Exception {
        mockMvc.perform(get("/actuator/beans")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_ValidCredentials_Returns200WithToken() throws Exception {
        String body = objectMapper.writeValueAsString(
                new LoginRequest("testuser", "User@123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void login_InvalidCredentials_Returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                new LoginRequest("testuser", "wrongpassword"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_ValidRequest_Returns201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.assignment.booking.dto.request.RegisterRequest(
                        "newuser", "NewPass@123", "new@example.com", "New User"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }
}
