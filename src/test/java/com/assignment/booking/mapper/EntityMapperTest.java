package com.assignment.booking.mapper;

import com.assignment.booking.dto.response.ReservationResponse;
import com.assignment.booking.dto.response.ResourceResponse;
import com.assignment.booking.dto.response.UserResponse;
import com.assignment.booking.entity.Reservation;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.ReservationStatus;
import com.assignment.booking.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    private EntityMapper entityMapper;

    @BeforeEach
    void setUp() {
        entityMapper = new EntityMapper();
    }

    @Test
    void toUserResponse_MapsAllFields() {
        Role role = Role.builder().id(1L).name(RoleName.ROLE_USER).build();
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .enabled(true)
                .roles(Set.of(role))
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse response = entityMapper.toUserResponse(user);

        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertTrue(response.getEnabled());
        assertEquals(1, response.getRoles().size());
        assertEquals("ROLE_USER", response.getRoles().get(0));
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void toResourceResponse_MapsAllFields() {
        Resource resource = Resource.builder()
                .id(1L)
                .name("Conference Room")
                .description("A room")
                .type("ROOM")
                .available(true)
                .pricePerUnit(BigDecimal.valueOf(50.00))
                .location("Building A")
                .capacity("20 people")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResourceResponse response = entityMapper.toResourceResponse(resource);

        assertEquals(1L, response.getId());
        assertEquals("Conference Room", response.getName());
        assertEquals("A room", response.getDescription());
        assertEquals("ROOM", response.getType());
        assertTrue(response.getAvailable());
        assertEquals(BigDecimal.valueOf(50.00), response.getPricePerUnit());
        assertEquals("Building A", response.getLocation());
        assertEquals("20 people", response.getCapacity());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void toReservationResponse_MapsAllFields() {
        User user = User.builder().id(1L).username("testuser").build();
        Resource resource = Resource.builder().id(1L).name("Room").type("ROOM").build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .resource(resource)
                .user(user)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(BigDecimal.valueOf(100))
                .status(ReservationStatus.PENDING)
                .notes("Test note")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReservationResponse response = entityMapper.toReservationResponse(reservation);

        assertEquals(1L, response.getId());
        assertEquals(1L, response.getResourceId());
        assertEquals("Room", response.getResourceName());
        assertEquals("ROOM", response.getResourceType());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertNotNull(response.getStartTime());
        assertNotNull(response.getEndTime());
        assertEquals(BigDecimal.valueOf(100), response.getPrice());
        assertEquals("PENDING", response.getStatus());
        assertEquals("Test note", response.getNotes());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void toReservationResponse_NullNotes_Handled() {
        User user = User.builder().id(1L).username("user").build();
        Resource resource = Resource.builder().id(1L).name("R").type("T").build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .resource(resource)
                .user(user)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .price(BigDecimal.TEN)
                .status(ReservationStatus.CONFIRMED)
                .build();

        ReservationResponse response = entityMapper.toReservationResponse(reservation);

        assertNull(response.getNotes());
        assertEquals("CONFIRMED", response.getStatus());
    }
}
