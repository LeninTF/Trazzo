package trazzo.back.reports.application.dto.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class GetMonthlyClosureCommandTest {

    @Test
    void shouldCreateCommandSuccessfully() {
        UUID id = UUID.randomUUID();
        GetMonthlyClosureCommand command = new GetMonthlyClosureCommand(id);

        assertEquals(id, command.id());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        GetMonthlyClosureCommand cmd1 = new GetMonthlyClosureCommand(id);
        GetMonthlyClosureCommand cmd2 = new GetMonthlyClosureCommand(id);

        assertEquals(cmd1, cmd2);
        assertEquals(cmd1.hashCode(), cmd2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        GetMonthlyClosureCommand cmd1 = new GetMonthlyClosureCommand(UUID.randomUUID());
        GetMonthlyClosureCommand cmd2 = new GetMonthlyClosureCommand(UUID.randomUUID());

        assertNotEquals(cmd1, cmd2);
    }

    @Test
    void shouldReturnToString() {
        UUID id = UUID.randomUUID();
        GetMonthlyClosureCommand command = new GetMonthlyClosureCommand(id);

        assertNotNull(command.toString());
        assertTrue(command.toString().contains(id.toString()));
    }
}
