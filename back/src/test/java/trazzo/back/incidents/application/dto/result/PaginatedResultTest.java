package trazzo.back.incidents.application.dto.result;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class PaginatedResultTest {

    @Test
    void createWithAllFields() {
        var content = List.of("a", "b");
        var result = new PaginatedResult<>(content, 0, 10, 2, 1);

        assertEquals(2, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void createWithEmptyContent() {
        var result = new PaginatedResult<>(List.of(), 1, 20, 0, 0);

        assertTrue(result.content().isEmpty());
        assertEquals(1, result.page());
        assertEquals(0, result.totalElements());
    }
}
