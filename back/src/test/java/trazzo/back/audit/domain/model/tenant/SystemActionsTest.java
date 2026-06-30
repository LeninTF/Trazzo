package trazzo.back.audit.domain.model.tenant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SystemActionsTest {

    @Test
    void shouldHaveFiveValues() {
        assertEquals(5, SystemActions.values().length);
    }

    @Test
    void shouldContainLogin() {
        assertEquals(SystemActions.LOGIN, SystemActions.valueOf("LOGIN"));
    }

    @Test
    void shouldContainCreate() {
        assertEquals(SystemActions.CREATE, SystemActions.valueOf("CREATE"));
    }

    @Test
    void shouldContainUpdate() {
        assertEquals(SystemActions.UPDATE, SystemActions.valueOf("UPDATE"));
    }

    @Test
    void shouldContainDelete() {
        assertEquals(SystemActions.DELETE, SystemActions.valueOf("DELETE"));
    }

    @Test
    void shouldContainRead() {
        assertEquals(SystemActions.READ, SystemActions.valueOf("READ"));
    }

}
