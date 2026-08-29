package com.assignment.booking.util;

import com.assignment.booking.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class SortUtil {

    private SortUtil() {
    }

    public static Pageable parsePageable(String[] sort, int page, int size,
                                         String defaultField, String defaultDirection) {
        Sort sortObj = parseSort(sort, defaultField, defaultDirection);
        return PageRequest.of(page, size, sortObj);
    }

    public static Sort parseSort(String[] sort, String defaultField, String defaultDirection) {
        if (sort == null || sort.length == 0) {
            return Sort.by(parseDirection(defaultDirection), defaultField);
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            if (sortParam == null || sortParam.isBlank()) {
                continue;
            }
            String[] parts = sortParam.split(",");
            if (parts.length < 1 || parts.length > 2) {
                throw new BadRequestException(
                        "Invalid sort format: '" + sortParam + "'. Expected 'field' or 'field,asc|desc'.");
            }

            String field = parts[0].trim();
            if (field.isBlank()) {
                throw new BadRequestException("Sort field cannot be blank.");
            }

            Sort.Direction direction = parts.length == 2
                    ? parseDirection(parts[1].trim())
                    : parseDirection(defaultDirection);

            orders.add(new Sort.Order(direction, field));
        }

        if (orders.isEmpty()) {
            return Sort.by(parseDirection(defaultDirection), defaultField);
        }

        return Sort.by(orders);
    }

    private static Sort.Direction parseDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid sort direction: '" + direction + "'. Must be 'asc' or 'desc'.");
        }
    }
}
