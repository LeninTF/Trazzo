package trazzo.back.corehr.application.dto.result;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalDateTime;

class SyncItemResultTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 0);
        var result = new SyncItemResult(true, "att-1", AttendanceState.PUNTUAL, 0, now, null, 50);
        assertTrue(result.success());
        assertEquals("att-1", result.attendanceId());
        assertEquals(AttendanceState.PUNTUAL, result.state());
        assertEquals(0, result.minutesLate());
        assertEquals(now, result.checkIn());
        assertNull(result.error());
        assertEquals(50, result.offlineEventId());
    }

    @Test
    void successFactoryShouldCreateSuccessResult() {
        var checkIn = LocalDateTime.of(2026, 7, 19, 8, 5);
        var result = SyncItemResult.success("att-2", AttendanceState.TARDANZA, 5, checkIn, 77);
        assertTrue(result.success());
        assertEquals("att-2", result.attendanceId());
        assertEquals(AttendanceState.TARDANZA, result.state());
        assertEquals(5, result.minutesLate());
        assertEquals(checkIn, result.checkIn());
        assertNull(result.error());
        assertEquals(77, result.offlineEventId());
    }

    @Test
    void failureFactoryShouldCreateFailureResult() {
        var result = SyncItemResult.failure("fingerprint mismatch", 33);
        assertFalse(result.success());
        assertNull(result.attendanceId());
        assertNull(result.state());
        assertEquals(0, result.minutesLate());
        assertNull(result.checkIn());
        assertEquals("fingerprint mismatch", result.error());
        assertEquals(33, result.offlineEventId());
    }

    @Test
    void skippedFactoryShouldCreateSkippedResult() {
        var result = SyncItemResult.skipped(88);
        assertFalse(result.success());
        assertNull(result.attendanceId());
        assertNull(result.state());
        assertEquals(0, result.minutesLate());
        assertNull(result.checkIn());
        assertEquals("Duplicate - already processed", result.error());
        assertEquals(88, result.offlineEventId());
    }

    @Test
    void shouldHaveValueEquality() {
        var now = LocalDateTime.of(2026, 7, 19, 8, 0);
        var a = new SyncItemResult(true, "att-1", AttendanceState.PUNTUAL, 0, now, null, 1);
        var b = new SyncItemResult(true, "att-1", AttendanceState.PUNTUAL, 0, now, null, 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var a = SyncItemResult.success("att-1", AttendanceState.PUNTUAL, 0, LocalDateTime.now(), 1);
        var b = SyncItemResult.success("att-2", AttendanceState.PUNTUAL, 0, LocalDateTime.now(), 1);
        assertNotEquals(a, b);
    }
}
