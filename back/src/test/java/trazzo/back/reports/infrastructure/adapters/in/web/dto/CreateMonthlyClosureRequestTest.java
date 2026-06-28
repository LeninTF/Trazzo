package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CreateMonthlyClosureRequestTest {

    @Test
    void shouldCreateRequestSuccessfully() {
        CreateMonthlyClosureRequest request = new CreateMonthlyClosureRequest(6, 2025);
        assertEquals(6, request.month());
        assertEquals(2025, request.year());
    }

    @Test
    void shouldBeEqualForSameValues() {
        CreateMonthlyClosureRequest r1 = new CreateMonthlyClosureRequest(6, 2025);
        CreateMonthlyClosureRequest r2 = new CreateMonthlyClosureRequest(6, 2025);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        CreateMonthlyClosureRequest r1 = new CreateMonthlyClosureRequest(6, 2025);
        CreateMonthlyClosureRequest r2 = new CreateMonthlyClosureRequest(7, 2025);
        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        CreateMonthlyClosureRequest request = new CreateMonthlyClosureRequest(6, 2025);
        assertNotNull(request.toString());
        assertTrue(request.toString().contains("6"));
        assertTrue(request.toString().contains("2025"));
    }
}
