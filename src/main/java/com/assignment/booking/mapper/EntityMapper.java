package com.assignment.booking.mapper;

import com.assignment.booking.dto.response.ResourceResponse;
import com.assignment.booking.dto.response.ReservationResponse;
import com.assignment.booking.dto.response.UserResponse;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.Reservation;
import com.assignment.booking.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    public ResourceResponse toResourceResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .available(resource.getAvailable())
                .pricePerUnit(resource.getPricePerUnit())
                .location(resource.getLocation())
                .capacity(resource.getCapacity())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }

    public ReservationResponse toReservationResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .resourceType(reservation.getResource().getType())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus().name())
                .notes(reservation.getNotes())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
