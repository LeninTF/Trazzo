package trazzo.back.reports.application.dto.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ListMonthlyClosuresCommandTest {

    @Test
    void shouldCreateCommandWithAllValues() {
        ListMonthlyClosuresCommand command = new ListMonthlyClosuresCommand(2025, 6);

        assertEquals(2025, command.year());
        assertEquals(6, command.month());
    }

    @Test
    void shouldCreateCommandWithNullValues() {
        ListMonthlyClosuresCommand command = new ListMonthlyClosuresCommand(null, null);

        assertNull(command.year());
        assertNull(command.month());
    }

    @Test
    void shouldBeEqualForSameValues() {
        ListMonthlyClosuresCommand cmd1 = new ListMonthlyClosuresCommand(2025, 6);
        ListMonthlyClosuresCommand cmd2 = new ListMonthlyClosuresCommand(2025, 6);

        assertEquals(cmd1, cmd2);
        assertEquals(cmd1.hashCode(), cmd2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        ListMonthlyClosuresCommand cmd1 = new ListMonthlyClosuresCommand(2025, 6);
        ListMonthlyClosuresCommand cmd2 = new ListMonthlyClosuresCommand(2024, 5);

        assertNotEquals(cmd1, cmd2);
    }

    @Test
    void shouldReturnToString() {
        ListMonthlyClosuresCommand command = new ListMonthlyClosuresCommand(2025, 6);

        assertNotNull(command.toString());
        assertTrue(command.toString().contains("2025"));
        assertTrue(command.toString().contains("6"));
    }
}
