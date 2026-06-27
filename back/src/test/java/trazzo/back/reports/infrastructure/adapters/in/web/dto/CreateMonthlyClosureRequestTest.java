package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class CreateMonthlyClosureRequestTest {

    @Test
    void shouldCreateRequestSuccessfully() {
        UUID userId = UUID.randomUUID();
        CreateMonthlyClosureRequest request = new CreateMonthlyClosureRequest(6, 2025, userId);
        assertEquals(6, request.month());
        assertEquals(2025, request.year());
        assertEquals(userId, request.createdByUserId());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID userId = UUID.randomUUID();
        CreateMonthlyClosureRequest r1 = new CreateMonthlyClosureRequest(6, 2025, userId);
        CreateMonthlyClosureRequest r2 = new CreateMonthlyClosureRequest(6, 2025, userId);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        CreateMonthlyClosureRequest r1 = new CreateMonthlyClosureRequest(6, 2025, UUID.randomUUID());
        CreateMonthlyClosureRequest r2 = new CreateMonthlyClosureRequest(7, 2025, UUID.randomUUID());
        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        CreateMonthlyClosureRequest request = new CreateMonthlyClosureRequest(6, 2025, UUID.randomUUID());
        assertNotNull(request.toString());
        assertTrue(request.toString().contains("6"));
        assertTrue(request.toString().contains("2025"));
    }
}
