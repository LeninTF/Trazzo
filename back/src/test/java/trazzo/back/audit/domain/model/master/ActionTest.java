package trazzo.back.audit.domain.model.master;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ActionTest {

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, Action.values().length);
    }

    @Test
    void shouldContainCreate() {
        assertEquals(Action.CREATE, Action.valueOf("CREATE"));
    }

    @Test
    void shouldContainUpdate() {
        assertEquals(Action.UPDATE, Action.valueOf("UPDATE"));
    }

    @Test
    void shouldContainDelete() {
        assertEquals(Action.DELETE, Action.valueOf("DELETE"));
    }

    @Test
    void shouldContainError() {
        assertEquals(Action.ERROR, Action.valueOf("ERROR"));
    }

}
