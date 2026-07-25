package trazzo.back.corehr.domain.model.attendance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BiometricIdentifyResultTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var result = new BiometricIdentifyResult(true, 42L, 85);
        assertTrue(result.matched());
        assertEquals(42L, result.tenantUserId());
        assertEquals(85, result.confidence());
    }

    @Test
    void noMatchShouldReturnUnmatchedResult() {
        var result = BiometricIdentifyResult.noMatch();
        assertFalse(result.matched());
        assertNull(result.tenantUserId());
        assertEquals(0, result.confidence());
    }

    @Test
    void matchShouldReturnMatchedResult() {
        var result = BiometricIdentifyResult.match(99L, 92);
        assertTrue(result.matched());
        assertEquals(99L, result.tenantUserId());
        assertEquals(92, result.confidence());
    }

    @Test
    void shouldHaveValueEquality() {
        var a = BiometricIdentifyResult.match(10L, 70);
        var b = BiometricIdentifyResult.match(10L, 70);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var a = BiometricIdentifyResult.noMatch();
        var b = BiometricIdentifyResult.match(1L, 50);
        assertNotEquals(a, b);
    }
}
