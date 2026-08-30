package com.assignment.booking.exception;

import com.assignment.booking.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleResourceNotFound_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource", 1L);
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("The requested resource was not found", response.getBody().getMessage());
    }

    @Test
    void handleResourceNotFound_SingleArg_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User");
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("The requested resource was not found", response.getBody().getMessage());
    }

    @Test
    void handleBadRequest_Returns400() {
        BadRequestException ex = new BadRequestException("Invalid input");
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    void handleDuplicateResource_Returns409() {
        DuplicateResourceException ex = new DuplicateResourceException("Username already exists");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    void handleUnauthorizedAccess_Returns403() {
        UnauthorizedAccessException ex = new UnauthorizedAccessException("Access denied");
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedAccess(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
    }

    @Test
    void handleReservationConflict_Returns409() {
        ReservationConflictException ex = new ReservationConflictException("Time slot taken");
        ResponseEntity<ErrorResponse> response = handler.handleReservationConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The resource is already booked for the selected time period", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_Returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to access this resource", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentials_Returns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad creds");
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_Returns400() {
        FieldError fieldError = new FieldError("request", "name", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("name"));
        assertTrue(response.getBody().getMessage().contains("must not be blank"));
    }

    @Test
    void handleGenericException_Returns500_WithoutInternalDetails() {
        Exception ex = new RuntimeException("database connection failed");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertFalse(response.getBody().getMessage().contains("database connection failed"));
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
    }

    @Test
    void allHandlers_IncludePathAndTimestamp() {
        BadRequestException ex = new BadRequestException("test");
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals("/api/test", response.getBody().getPath());
        assertTrue(response.getBody().getTimestamp() > 0);
    }
}
