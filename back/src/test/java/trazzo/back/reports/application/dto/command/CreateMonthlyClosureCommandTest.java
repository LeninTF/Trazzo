package trazzo.back.reports.application.dto.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class CreateMonthlyClosureCommandTest {

    @Test
    void shouldCreateCommandSuccessfully() {
        UUID userId = UUID.randomUUID();
        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, userId);

        assertEquals(6, command.month());
        assertEquals(2025, command.year());
        assertEquals(userId, command.createdByUserId());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID userId = UUID.randomUUID();
        CreateMonthlyClosureCommand cmd1 = new CreateMonthlyClosureCommand(6, 2025, userId);
        CreateMonthlyClosureCommand cmd2 = new CreateMonthlyClosureCommand(6, 2025, userId);

        assertEquals(cmd1, cmd2);
        assertEquals(cmd1.hashCode(), cmd2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        CreateMonthlyClosureCommand cmd1 = new CreateMonthlyClosureCommand(6, 2025, UUID.randomUUID());
        CreateMonthlyClosureCommand cmd2 = new CreateMonthlyClosureCommand(7, 2025, UUID.randomUUID());

        assertNotEquals(cmd1, cmd2);
    }

    @Test
    void shouldReturnToString() {
        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, UUID.randomUUID());

        assertNotNull(command.toString());
        assertTrue(command.toString().contains("6"));
        assertTrue(command.toString().contains("2025"));
    }
}
