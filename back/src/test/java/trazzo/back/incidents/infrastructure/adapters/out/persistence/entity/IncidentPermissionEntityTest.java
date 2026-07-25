package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

class IncidentPermissionEntityTest {

    @Test
    void createInstance() {
        var now = LocalDateTime.now();
        var entity = new IncidentPermissionEntity(1, 1,
                LocalDate.now(), LocalDate.now().plusDays(1), 3, now, now);

        assertEquals(1, entity.getId());
        assertEquals(1, entity.getIncidentId());
        assertEquals(3, entity.getDaysGranted());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentPermissionEntity();
        entity.setId(1);
        entity.setDaysGranted(5);

        assertEquals(1, entity.getId());
        assertEquals(5, entity.getDaysGranted());
    }
}
