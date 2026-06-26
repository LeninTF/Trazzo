package trazzo.back.reports.application.dto.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CreateMonthlyClosureCommandTest {

    @Test
    void shouldCreateCommandSuccessfully() {
        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, "user-1");

        assertEquals(6, command.month());
        assertEquals(2025, command.year());
        assertEquals("user-1", command.createdByUserId());
    }

    @Test
    void shouldBeEqualForSameValues() {
        CreateMonthlyClosureCommand cmd1 = new CreateMonthlyClosureCommand(6, 2025, "user-1");
        CreateMonthlyClosureCommand cmd2 = new CreateMonthlyClosureCommand(6, 2025, "user-1");

        assertEquals(cmd1, cmd2);
        assertEquals(cmd1.hashCode(), cmd2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        CreateMonthlyClosureCommand cmd1 = new CreateMonthlyClosureCommand(6, 2025, "user-1");
        CreateMonthlyClosureCommand cmd2 = new CreateMonthlyClosureCommand(7, 2025, "user-2");

        assertNotEquals(cmd1, cmd2);
    }

    @Test
    void shouldReturnToString() {
        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, "user-1");

        assertNotNull(command.toString());
        assertTrue(command.toString().contains("6"));
        assertTrue(command.toString().contains("2025"));
        assertTrue(command.toString().contains("user-1"));
    }
}
