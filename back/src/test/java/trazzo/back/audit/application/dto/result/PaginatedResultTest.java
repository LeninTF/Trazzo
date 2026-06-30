package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginatedResultTest {

    @Test
    void shouldCreateWithEmptyList() {
        var result = new PaginatedResult<>(List.of(), 0, 10, 0, 0);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void shouldCreateWithStringList() {
        var content = List.of("a", "b", "c");
        var result = new PaginatedResult<>(content, 1, 3, 100, 34);
        assertEquals(content, result.content());
        assertEquals(1, result.page());
        assertEquals(3, result.size());
        assertEquals(100, result.totalElements());
        assertEquals(34, result.totalPages());
    }

    @Test
    void shouldTestEquality() {
        var r1 = new PaginatedResult<>(List.of("x"), 0, 1, 1, 1);
        var r2 = new PaginatedResult<>(List.of("x"), 0, 1, 1, 1);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new PaginatedResult<>(List.of("a"), 0, 1, 1, 1);
        assertTrue(result.toString().contains("content=[a]"));
    }
}
