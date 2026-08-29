package com.assignment.booking.integration;

import com.assignment.booking.dto.request.ResourceRequest;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResourceCrudIntegrationTest {

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

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.save(Role.builder()
                .name(RoleName.ROLE_USER).description("User").build());
        Role adminRole = roleRepository.save(Role.builder()
                .name(RoleName.ROLE_ADMIN).description("Admin").build());

        User admin = userRepository.save(User.builder()
                .username("admin_crud")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@crud.com")
                .fullName("Admin")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole, adminRole)))
                .build());

        User user = userRepository.save(User.builder()
                .username("user_crud")
                .password(passwordEncoder.encode("User@123"))
                .email("user@crud.com")
                .fullName("User")
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

    private ResourceRequest createRequest(String name, String type, BigDecimal price) {
        return ResourceRequest.builder()
                .name(name)
                .type(type)
                .pricePerUnit(price)
                .description("Test description")
                .location("Test location")
                .capacity("10")
                .build();
    }

    @Test
    void createResource_Admin_Returns201() throws Exception {
        ResourceRequest request = createRequest("Conference Room A", "ROOM", BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Conference Room A"))
                .andExpect(jsonPath("$.data.type").value("ROOM"))
                .andExpect(jsonPath("$.data.pricePerUnit").value(50));
    }

    @Test
    void createResource_User_Returns403() throws Exception {
        ResourceRequest request = createRequest("Conference Room A", "ROOM", BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_MissingName_Returns400() throws Exception {
        String body = "{\"type\":\"ROOM\",\"pricePerUnit\":50}";

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllResources_ReturnsPaginated() throws Exception {
        ResourceRequest req1 = createRequest("Room 1", "ROOM", BigDecimal.valueOf(25));
        ResourceRequest req2 = createRequest("Room 2", "ROOM", BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)));
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)));

        mockMvc.perform(get("/api/resources")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getResourceById_Found() throws Exception {
        ResourceRequest request = createRequest("Findable Room", "ROOM", BigDecimal.valueOf(75));
        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/resources/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Findable Room"));
    }

    @Test
    void getResourceById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/resources/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateResource_Admin_Success() throws Exception {
        ResourceRequest createReq = createRequest("Original", "ROOM", BigDecimal.valueOf(50));
        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        ResourceRequest updateReq = createRequest("Updated", "VEHICLE", BigDecimal.valueOf(100));
        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"))
                .andExpect(jsonPath("$.data.type").value("VEHICLE"))
                .andExpect(jsonPath("$.data.pricePerUnit").value(100));
    }

    @Test
    void deleteResource_Admin_Success() throws Exception {
        ResourceRequest request = createRequest("To Delete", "ROOM", BigDecimal.valueOf(30));
        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resources/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteResource_User_Returns403() throws Exception {
        ResourceRequest request = createRequest("Protected", "ROOM", BigDecimal.valueOf(60));
        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
}
