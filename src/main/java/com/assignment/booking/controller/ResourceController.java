package com.assignment.booking.controller;

import com.assignment.booking.dto.request.ResourceRequest;
import com.assignment.booking.dto.response.ApiResponse;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ResourceResponse;
import com.assignment.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Resource management endpoints")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get all resources", description = "Retrieve all resources with optional filtering")
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> getAllResources(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        PageResponse<ResourceResponse> resources = resourceService.getAllResources(type, available, pageable);
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID", description = "Retrieve a single resource by its ID")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable Long id) {
        ResourceResponse resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", resource));
    }

    @PostMapping
    @Operation(summary = "Create resource", description = "Create a new resource (ADMIN only)")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse resource = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", resource));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update resource", description = "Update an existing resource (ADMIN only)")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse resource = resourceService.updateResource(id, request);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete resource", description = "Delete a resource (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully"));
    }
}
