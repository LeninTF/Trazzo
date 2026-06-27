package trazzo.back.corehr.domain.model.schedule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidScheduleException;
import java.time.LocalTime;

class UserScheduleTest {

    @Test
    void shouldCreateUserSchedule() {
        var us = UserSchedule.create(1L, 1L, "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));
        assertNull(us.getId());
        assertEquals(1L, us.getTenantUserId());
        assertEquals(1L, us.getScheduleId());
        assertEquals("Desc", us.getDescription());
    }

    @Test
    void shouldRestoreUserSchedule() {
        var now = java.time.LocalDateTime.now();
        var us = UserSchedule.restore(1L, 1L, 1L, "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        assertEquals(1L, us.getId());
        assertEquals("Desc", us.getDescription());
    }

    @Test
    void shouldReschedule() {
        var us = UserSchedule.create(1L, 1L, null, LocalTime.of(8, 0), LocalTime.of(17, 0));
        us.reschedule(LocalTime.of(9, 0), LocalTime.of(18, 0));
        assertEquals(LocalTime.of(9, 0), us.getEntryTime());
        assertEquals(LocalTime.of(18, 0), us.getDepartureTime());
    }

    @Test
    void shouldUpdateDescription() {
        var us = UserSchedule.create(1L, 1L, "Old", LocalTime.of(8, 0), LocalTime.of(17, 0));
        us.updateDescription("New");
        assertEquals("New", us.getDescription());
    }

    @Test
    void shouldThrowWhenTenantUserIdIsNull() {
        assertThrows(InvalidScheduleException.class, () ->
            UserSchedule.create(null, 1L, null, LocalTime.of(8, 0), LocalTime.of(17, 0))
        );
    }

    @Test
    void shouldThrowWhenEntryTimeIsNull() {
        assertThrows(InvalidScheduleException.class, () ->
            UserSchedule.create(1L, 1L, null, null, LocalTime.of(17, 0))
        );
    }

    @Test
    void shouldThrowWhenDepartureBeforeEntry() {
        assertThrows(InvalidScheduleException.class, () ->
            UserSchedule.create(1L, 1L, null, LocalTime.of(17, 0), LocalTime.of(8, 0))
        );
    }
}
