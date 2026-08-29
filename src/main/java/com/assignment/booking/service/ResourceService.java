package com.assignment.booking.service;

import com.assignment.booking.dto.request.ResourceRequest;
import com.assignment.booking.dto.response.PageResponse;
import com.assignment.booking.dto.response.ResourceResponse;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.mapper.EntityMapper;
import com.assignment.booking.repository.ResourceRepository;
import com.assignment.booking.specification.ResourceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final EntityMapper entityMapper;

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getAllResources(
            String type, Boolean available, Pageable pageable) {

        Specification<Resource> spec = ResourceSpecification.withFilters(type, available);
        Page<Resource> page = resourceRepository.findAll(spec, pageable);

        return PageResponse.<ResourceResponse>builder()
                .content(page.getContent().stream()
                        .map(entityMapper::toResourceResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        return entityMapper.toResourceResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .pricePerUnit(request.getPricePerUnit())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .build();

        Resource saved = resourceRepository.save(resource);
        return entityMapper.toResourceResponse(saved);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        resource.setPricePerUnit(request.getPricePerUnit());
        resource.setLocation(request.getLocation());
        resource.setCapacity(request.getCapacity());

        Resource updated = resourceRepository.save(resource);
        return entityMapper.toResourceResponse(updated);
    }

    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource", id);
        }
        resourceRepository.deleteById(id);
    }
}
