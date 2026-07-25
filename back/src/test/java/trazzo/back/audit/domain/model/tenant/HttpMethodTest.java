package trazzo.back.audit.domain.model.tenant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HttpMethodTest {

    @Test
    void shouldHaveFiveValues() {
        assertEquals(5, HttpMethod.values().length);
    }

    @Test
    void shouldContainGet() {
        assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"));
    }

    @Test
    void shouldContainPut() {
        assertEquals(HttpMethod.PUT, HttpMethod.valueOf("PUT"));
    }

    @Test
    void shouldContainPost() {
        assertEquals(HttpMethod.POST, HttpMethod.valueOf("POST"));
    }

    @Test
    void shouldContainPatch() {
        assertEquals(HttpMethod.PATCH, HttpMethod.valueOf("PATCH"));
    }

    @Test
    void shouldContainDelete() {
        assertEquals(HttpMethod.DELETE, HttpMethod.valueOf("DELETE"));
    }

}
