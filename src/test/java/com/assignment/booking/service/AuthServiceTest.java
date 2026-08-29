package com.assignment.booking.service;

import com.assignment.booking.dto.request.LoginRequest;
import com.assignment.booking.dto.request.RegisterRequest;
import com.assignment.booking.dto.response.LoginResponse;
import com.assignment.booking.dto.response.UserResponse;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.RoleName;
import com.assignment.booking.exception.DuplicateResourceException;
import com.assignment.booking.mapper.EntityMapper;
import com.assignment.booking.repository.RoleRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name(RoleName.ROLE_USER)
                .description("User role")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .fullName("Test User")
                .enabled(true)
                .roles(roles)
                .build();
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("testuser", "password123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");
        when(tokenProvider.getExpirationDateFromToken("jwt-token")).thenReturn(new Date(System.currentTimeMillis() + 86400000));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("password123")
                .email("new@example.com")
                .fullName("New User")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(entityMapper.toUserResponse(any(User.class))).thenReturn(UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .build());

        UserResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }
}
