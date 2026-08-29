package com.assignment.booking.service;

import com.assignment.booking.dto.request.ResourceRequest;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ResourceResponse;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.mapper.EntityMapper;
import com.assignment.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource;
    private ResourceResponse resourceResponse;

    @BeforeEach
    void setUp() {
        resource = Resource.builder()
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

        resourceResponse = ResourceResponse.builder()
                .id(1L)
                .name("Conference Room")
                .type("ROOM")
                .available(true)
                .pricePerUnit(BigDecimal.valueOf(50.00))
                .location("Building A")
                .capacity("20 people")
                .build();
    }

    @Test
    void getAllResources_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(resource), pageable, 1);

        when(resourceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(entityMapper.toResourceResponse(resource)).thenReturn(resourceResponse);

        PageResponse<ResourceResponse> response = resourceService.getAllResources(null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Conference Room", response.getContent().get(0).getName());
        assertEquals(1L, response.getTotalElements());
    }

    @Test
    void getAllResources_EmptyResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(), pageable, 0);

        when(resourceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ResourceResponse> response = resourceService.getAllResources(null, null, pageable);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0L, response.getTotalElements());
    }

    @Test
    void getResourceById_Success() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(entityMapper.toResourceResponse(resource)).thenReturn(resourceResponse);

        ResourceResponse response = resourceService.getResourceById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Conference Room", response.getName());
    }

    @Test
    void getResourceById_NotFound_ThrowsException() {
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResourceById(999L));
    }

    @Test
    void createResource_Success() {
        ResourceRequest request = ResourceRequest.builder()
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(75.00))
                .location("Building B")
                .capacity("10 people")
                .build();

        Resource savedResource = Resource.builder()
                .id(2L)
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(75.00))
                .location("Building B")
                .capacity("10 people")
                .build();

        ResourceResponse savedResponse = ResourceResponse.builder()
                .id(2L)
                .name("New Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(75.00))
                .build();

        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);
        when(entityMapper.toResourceResponse(savedResource)).thenReturn(savedResponse);

        ResourceResponse response = resourceService.createResource(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("New Room", response.getName());
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void updateResource_Success() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Updated Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(60.00))
                .build();

        Resource updatedResource = Resource.builder()
                .id(1L)
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

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(updatedResource);
        when(entityMapper.toResourceResponse(updatedResource)).thenReturn(updatedResponse);

        ResourceResponse response = resourceService.updateResource(1L, request);

        assertNotNull(response);
        assertEquals("Updated Room", response.getName());
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void updateResource_NotFound_ThrowsException() {
        ResourceRequest request = ResourceRequest.builder()
                .name("Updated Room")
                .type("ROOM")
                .pricePerUnit(BigDecimal.valueOf(60.00))
                .build();

        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.updateResource(999L, request));
    }

    @Test
    void deleteResource_Success() {
        when(resourceRepository.existsById(1L)).thenReturn(true);

        resourceService.deleteResource(1L);

        verify(resourceRepository).deleteById(1L);
    }

    @Test
    void deleteResource_NotFound_ThrowsException() {
        when(resourceRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> resourceService.deleteResource(999L));
    }
}
