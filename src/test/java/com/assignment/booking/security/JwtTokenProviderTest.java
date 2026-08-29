package com.assignment.booking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi0yMDI0"; // base64 encoded 32+ bytes
    private static final long EXPIRATION_MS = 86400000;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        var secretField = org.springframework.util.ReflectionUtils.findField(JwtTokenProvider.class, "jwtSecret");
        org.springframework.util.ReflectionUtils.makeAccessible(secretField);
        org.springframework.util.ReflectionUtils.setField(secretField, tokenProvider, TEST_SECRET);

        var expField = org.springframework.util.ReflectionUtils.findField(JwtTokenProvider.class, "jwtExpirationMs");
        org.springframework.util.ReflectionUtils.makeAccessible(expField);
        org.springframework.util.ReflectionUtils.setField(expField, tokenProvider, EXPIRATION_MS);

        tokenProvider.init();
    }

    @Test
    void generateToken_ReturnsNonNullToken() {
        User userDetails = new User("testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = tokenProvider.generateToken(auth);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUsernameFromToken_ReturnsCorrectUsername() {
        User userDetails = new User("testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = tokenProvider.generateToken(auth);
        String username = tokenProvider.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        User userDetails = new User("testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = tokenProvider.generateToken(auth);

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateToken_NullToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void getExpirationDateFromToken_ReturnsFutureDate() {
        User userDetails = new User("testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = tokenProvider.generateToken(auth);
        Date expiration = tokenProvider.getExpirationDateFromToken(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void init_ShortSecret_ThrowsException() {
        JwtTokenProvider provider = new JwtTokenProvider();
        var secretField = org.springframework.util.ReflectionUtils.findField(JwtTokenProvider.class, "jwtSecret");
        org.springframework.util.ReflectionUtils.makeAccessible(secretField);
        org.springframework.util.ReflectionUtils.setField(secretField, provider, "short");

        assertThrows(IllegalStateException.class, provider::init);
    }
}
