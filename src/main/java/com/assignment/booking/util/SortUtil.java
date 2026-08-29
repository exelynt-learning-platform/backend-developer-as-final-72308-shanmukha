package com.assignment.booking.util;

import com.assignment.booking.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortUtil {

    private static final Set<String> VALID_DIRECTIONS = Set.of("asc", "desc");

    private SortUtil() {
    }

    public static Pageable parsePageable(String[] sort, int page, int size,
                                         String defaultField, String defaultDirection) {
        Sort sortObj = parseSort(sort, defaultField, defaultDirection);
        return PageRequest.of(page, size, sortObj);
    }

    public static Sort parseSort(String[] sort, String defaultField, String defaultDirection) {
        if (sort == null || sort.length == 0) {
            return Sort.by(Sort.Direction.fromString(defaultDirection), defaultField);
        }

        if (sort.length == 1) {
            return Sort.by(Sort.Direction.fromString(defaultDirection), sort[0]);
        }

        if (sort.length >= 2) {
            String direction = sort[1].toLowerCase();
            if (!VALID_DIRECTIONS.contains(direction)) {
                throw new BadRequestException(
                        "Invalid sort direction: '" + sort[1] + "'. Must be 'asc' or 'desc'.");
            }
            return Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        }

        return Sort.by(Sort.Direction.fromString(defaultDirection), defaultField);
    }
}
