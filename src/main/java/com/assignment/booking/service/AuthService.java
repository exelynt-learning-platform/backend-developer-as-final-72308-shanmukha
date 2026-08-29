package com.assignment.booking.service;

import com.assignment.booking.dto.request.LoginRequest;
import com.assignment.booking.dto.request.RegisterRequest;
import com.assignment.booking.dto.response.LoginResponse;
import com.assignment.booking.dto.response.UserResponse;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.RoleName;
import com.assignment.booking.exception.BadRequestException;
import com.assignment.booking.exception.DuplicateResourceException;
import com.assignment.booking.mapper.EntityMapper;
import com.assignment.booking.repository.RoleRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EntityMapper entityMapper;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toList())
                .expiresAt(LocalDateTime.ofInstant(
                        tokenProvider.getExpirationDateFromToken(token).toInstant(),
                        ZoneId.systemDefault()))
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new BadRequestException("Default role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .enabled(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return entityMapper.toUserResponse(savedUser);
    }
}
