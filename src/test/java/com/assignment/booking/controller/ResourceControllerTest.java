package com.assignment.booking.controller;

import com.assignment.booking.dto.request.ResourceRequest;
import com.assignment.booking.dto.response.ApiResponse;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ResourceResponse;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.service.ResourceService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ResourceController resourceController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResourceResponse resourceResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resourceController).build();

        resourceResponse = ResourceResponse.builder()
                .id(1L)
                .name("Conference Room")
                .type("ROOM")
                .available(true)
                .pricePerUnit(BigDecimal.valueOf(50.00))
                .location("Building A")
                .capacity("20 people")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllResources_Success() throws Exception {
        PageResponse<ResourceResponse> pageResponse = PageResponse.<ResourceResponse>builder()
                .content(List.of(resourceResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(resourceService.getAllResources(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/resources")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Conference Room"));
    }

    @Test
    void getResourceById_Success() throws Exception {
        when(resourceService.getResourceById(1L)).thenReturn(resourceResponse);

        mockMvc.perform(get("/api/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Conference Room"));
    }

    @Test
    void getResourceById_NotFound_Returns404() throws Exception {
        when(resourceService.getResourceById(999L))
                .thenThrow(new ResourceNotFoundException("Resource", 999L));

        mockMvc.perform(get("/api/resources/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createResource_Success() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(75.00))
                .build();

        ResourceResponse createdResponse = ResourceResponse.builder()
                .id(2L)
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(75.00))
                .build();

        when(resourceService.createResource(any(ResourceRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Room"));
    }

    @Test
    void updateResource_Success() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("Updated Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(60.00))
                .build();

        ResourceResponse updatedResponse = ResourceResponse.builder()
                .id(1L)
                .name("Updated Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(60.00))
                .build();

        when(resourceService.updateResource(eq(1L), any(ResourceRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/resources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Room"));
    }

    @Test
    void deleteResource_Success() throws Exception {
        doNothing().when(resourceService).deleteResource(1L);

        mockMvc.perform(delete("/api/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createResource_MissingFields_Returns400() throws Exception {
        String requestJson = "{\"name\":\"\"}";

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}
