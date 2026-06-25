package trazzo.back.incidents.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IncidentPermissionTest {

    /* == CREATION TESTS == */

    @Test
    void createWithValidFields() {
        var startDate = LocalDate.now();
        var endDate = startDate.plusDays(3);
        var before = LocalDateTime.now();
        var permission = IncidentPermission.create("inc-1", startDate, endDate, 3);
        var after = LocalDateTime.now();

        assertNull(permission.getId());
        assertEquals("inc-1", permission.getIncidentId());
        assertEquals(startDate, permission.getStartDate());
        assertEquals(endDate, permission.getEndDate());
        assertEquals(3, permission.getDaysGranted());
        assertNotNull(permission.getCreatedAt());
        assertNotNull(permission.getUpdatedAt());
        assertFalse(permission.getCreatedAt().isBefore(before));
        assertFalse(permission.getCreatedAt().isAfter(after));
        assertFalse(permission.getUpdatedAt().isBefore(before));
        assertFalse(permission.getUpdatedAt().isAfter(after));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankIncidentIdThrowsException(String incidentId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.create(
                        incidentId, LocalDate.now(), LocalDate.now().plusDays(1), 1
                )
        );
    }

    @Test
    void createWithNullStartDateThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.create("inc-1", null, LocalDate.now().plusDays(1), 1)
        );
    }

    @Test
    void createWithNullEndDateThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.create("inc-1", LocalDate.now(), null, 1)
        );
    }

    @Test
    void createWithEndDateBeforeStartDateThrowsException() {
        var startDate = LocalDate.now();
        var endDate = startDate.minusDays(1);
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.create("inc-1", startDate, endDate, 1)
        );
    }

    @Test
    void createWithSameStartAndEndDateIsValid() {
        var date = LocalDate.now();
        var permission = IncidentPermission.create("inc-1", date, date, 1);

        assertEquals(date, permission.getStartDate());
        assertEquals(date, permission.getEndDate());
    }

    @Test
    void createWithZeroDaysGrantedThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.create(
                        "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 0
                )
        );
    }

    @Test
    void createWithNegativeDaysGrantedThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.create(
                        "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), -1
                )
        );
    }

    /* == RESTORATION TESTS == */

    @Test
    void restoreWithAllFields() {
        var startDate = LocalDate.now();
        var endDate = startDate.plusDays(2);
        var now = LocalDateTime.now();

        var permission = IncidentPermission.restore(
                "perm-1", "inc-1", startDate, endDate, 2, now, now
        );

        assertEquals("perm-1", permission.getId());
        assertEquals("inc-1", permission.getIncidentId());
        assertEquals(startDate, permission.getStartDate());
        assertEquals(endDate, permission.getEndDate());
        assertEquals(2, permission.getDaysGranted());
        assertEquals(now, permission.getCreatedAt());
        assertEquals(now, permission.getUpdatedAt());
    }

    @Test
    void restoreWithBlankIdNormalizesToNull() {
        var now = LocalDateTime.now();
        var permission = IncidentPermission.restore(
                " ", "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now
        );
        assertNull(permission.getId());
    }

    @Test
    void restoreWithBlankIncidentIdThrowsException() {
        var now = LocalDateTime.now();
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentPermission.restore(
                        "id-1", " ", LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now
                )
        );
    }

    /* == RESCHEDULE TESTS == */

    @Test
    void rescheduleSuccessfully() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(3), 3
        );
        var newStart = LocalDate.now().plusDays(5);
        var newEnd = newStart.plusDays(10);

        permission.reschedule(newStart, newEnd, 10);

        assertEquals(newStart, permission.getStartDate());
        assertEquals(newEnd, permission.getEndDate());
        assertEquals(10, permission.getDaysGranted());
    }

    @Test
    void rescheduleWithNullStartDateThrowsException() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(3), 3
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> permission.reschedule(null, LocalDate.now().plusDays(1), 1)
        );
    }

    @Test
    void rescheduleWithNullEndDateThrowsException() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(3), 3
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> permission.reschedule(LocalDate.now(), null, 1)
        );
    }

    @Test
    void rescheduleWithEndDateBeforeStartDateThrowsException() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(3), 3
        );
        var newStart = LocalDate.now().plusDays(5);
        assertThrows(
                IllegalArgumentException.class,
                () -> permission.reschedule(newStart, newStart.minusDays(1), 1)
        );
    }

    @Test
    void rescheduleWithZeroDaysGrantedThrowsException() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(3), 3
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> permission.reschedule(LocalDate.now(), LocalDate.now().plusDays(1), 0)
        );
    }

    @Test
    void rescheduleUpdatesUpdatedAt() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(3), 3
        );
        var originalUpdatedAt = permission.getUpdatedAt();
        permission.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        permission.reschedule(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 2);

        assertTrue(permission.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == BELONGS TO TESTS == */

    @Test
    void belongsToReturnsTrueForMatchingIncidentId() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1
        );

        assertTrue(permission.belongsTo("inc-1"));
    }

    @Test
    void belongsToReturnsFalseForDifferentIncidentId() {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1
        );

        assertFalse(permission.belongsTo("other-inc"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void belongsToWithBlankIncidentIdThrowsException(String incidentId) {
        var permission = IncidentPermission.create(
                "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> permission.belongsTo(incidentId)
        );
    }

    /* == VALIDATION TESTS == */

    @Test
    void createTrimsWhitespaceFromIncidentId() {
        var permission = IncidentPermission.create(
                "  inc-1  ", LocalDate.now(), LocalDate.now().plusDays(1), 1
        );
        assertEquals("inc-1", permission.getIncidentId());
    }

    @Test
    void restoreTrimsWhitespaceFromId() {
        var now = LocalDateTime.now();
        var permission = IncidentPermission.restore(
                "  perm-1  ", "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now
        );
        assertEquals("perm-1", permission.getId());
    }

    @Test
    void restoreTrimsWhitespaceFromIncidentId() {
        var now = LocalDateTime.now();
        var permission = IncidentPermission.restore(
                "perm-1", "  inc-1  ", LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now
        );
        assertEquals("inc-1", permission.getIncidentId());
    }

}
