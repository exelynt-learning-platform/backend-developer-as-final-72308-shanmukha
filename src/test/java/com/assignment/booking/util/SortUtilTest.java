package com.assignment.booking.util;

import com.assignment.booking.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class SortUtilTest {

    @Test
    void parseSort_TwoElements_ReturnsCorrectSort() {
        Sort sort = SortUtil.parseSort(new String[]{"price,asc"}, "id", "asc");
        assertNotNull(sort);
        assertEquals(1, sort.stream().count());
    }

    @Test
    void parseSort_TwoElementsDesc_ReturnsCorrectSort() {
        Sort sort = SortUtil.parseSort(new String[]{"createdAt,desc"}, "id", "asc");
        assertNotNull(sort);
        assertEquals(1, sort.stream().count());
    }

    @Test
    void parseSort_OneElement_UsesDefaultDirection() {
        Sort sort = SortUtil.parseSort(new String[]{"price"}, "id", "desc");
        assertNotNull(sort);
        assertEquals(1, sort.stream().count());
    }

    @Test
    void parseSort_Null_UsesDefaults() {
        Sort sort = SortUtil.parseSort(null, "id", "asc");
        assertNotNull(sort);
        assertEquals(1, sort.stream().count());
    }

    @Test
    void parseSort_EmptyArray_UsesDefaults() {
        Sort sort = SortUtil.parseSort(new String[]{}, "id", "asc");
        assertNotNull(sort);
        assertEquals(1, sort.stream().count());
    }

    @Test
    void parseSort_InvalidDirection_ThrowsException() {
        assertThrows(BadRequestException.class,
                () -> SortUtil.parseSort(new String[]{"price,invalid"}, "id", "asc"));
    }

    @Test
    void parseSort_MultipleSortFields_ReturnsMultipleOrders() {
        Sort sort = SortUtil.parseSort(new String[]{"price,asc", "createdAt,desc"}, "id", "asc");
        assertNotNull(sort);
        assertEquals(2, sort.stream().count());
    }

    @Test
    void parseSort_InvalidFormat_ThrowsException() {
        assertThrows(BadRequestException.class,
                () -> SortUtil.parseSort(new String[]{"price,asc,extra"}, "id", "asc"));
    }

    @Test
    void parseSort_BlankField_ThrowsException() {
        assertThrows(BadRequestException.class,
                () -> SortUtil.parseSort(new String[]{",asc"}, "id", "asc"));
    }

    @Test
    void parsePageable_ValidSort_ReturnsPageable() {
        Pageable pageable = SortUtil.parsePageable(new String[]{"price,asc"}, 0, 10, "id", "asc");
        assertNotNull(pageable);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void parsePageable_NullSort_UsesDefaults() {
        Pageable pageable = SortUtil.parsePageable(null, 0, 10, "id", "desc");
        assertNotNull(pageable);
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }
}
