package com.assignment.booking.integration;

import com.assignment.booking.dto.request.ReservationRequest;
import com.assignment.booking.dto.request.ResourceRequest;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.ReservationStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationCrudIntegrationTest {

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

    private String adminToken;
    private String userToken;
    private String otherUserToken;
    private Long resourceId;

    @BeforeEach
    void setUp() throws Exception {
        Role userRole = roleRepository.save(Role.builder()
                .name(RoleName.ROLE_USER).description("User").build());
        Role adminRole = roleRepository.save(Role.builder()
                .name(RoleName.ROLE_ADMIN).description("Admin").build());

        User admin = userRepository.save(User.builder()
                .username("admin_res")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@res.com")
                .fullName("Admin")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole, adminRole)))
                .build());

        User user = userRepository.save(User.builder()
                .username("user_res")
                .password(passwordEncoder.encode("User@123"))
                .email("user@res.com")
                .fullName("User")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        User otherUser = userRepository.save(User.builder()
                .username("other_res")
                .password(passwordEncoder.encode("Other@123"))
                .email("other@res.com")
                .fullName("Other User")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        adminToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(toUserDetails(admin), null,
                        admin.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                                .collect(Collectors.toList())));
        userToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(toUserDetails(user), null,
                        user.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                                .collect(Collectors.toList())));
        otherUserToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(toUserDetails(otherUser), null,
                        otherUser.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                                .collect(Collectors.toList())));

        ResourceRequest resourceRequest = ResourceRequest.builder()
                .name("Test Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(50))
                .description("desc")
                .location("loc")
                .capacity("10")
                .build();

        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andReturn().getResponse().getContentAsString();

        resourceId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
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

    private ReservationRequest createReservationRequest(Long resourceId, BigDecimal price) {
        return ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(price)
                .build();
    }

    @Test
    void createReservation_AuthenticatedUser_Returns201() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceId").value(resourceId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createReservation_Unauthenticated_Returns401() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReservation_InvalidTimeRange_Returns400() throws Exception {
        ReservationRequest request = ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_Conflict_Returns409() throws Exception {
        ReservationRequest request1 = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        ReservationRequest request2 = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void getReservations_UserOwnOnly() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        ReservationRequest otherRequest = ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(LocalDateTime.now().plusDays(4))
                .endTime(LocalDateTime.now().plusDays(4).plusHours(2))
                .price(BigDecimal.valueOf(120))
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherRequest)));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    void getReservations_AdminSeesAll() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createReservationRequest(resourceId, BigDecimal.valueOf(100)))));

        ReservationRequest otherRequest = ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(3).plusHours(2))
                .price(BigDecimal.valueOf(120))
                .build();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherRequest)));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void getReservationById_Own_Success() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        String createResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/reservations/" + id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void getReservationById_OtherUser_Returns403() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        String createResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/reservations/" + id)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReservation_Own_Success() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        String createResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/reservations/" + id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservations/" + id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReservation_Own_Success() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        String createResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        ReservationRequest updateRequest = ReservationRequest.builder()
                .resourceId(resourceId)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .price(BigDecimal.valueOf(200))
                .status(ReservationStatus.CONFIRMED)
                .build();

        mockMvc.perform(put("/api/reservations/" + id)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.price").value(200));
    }

    @Test
    void reservationEndpoints_FilterByStatus() throws Exception {
        ReservationRequest request = createReservationRequest(resourceId, BigDecimal.valueOf(100));
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
