package trazzo.back.corehr.domain.model.attendance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;

class DeviceTest {

    @Test
    void shouldCreateDevice() {
        var d = Device.create("DEV-001", "Device 1", "192.168.1.1", 8080, "Office", 1L);
        assertNull(d.getId());
        assertEquals("DEV-001", d.getCode());
        assertTrue(d.isState());
        assertEquals(1L, d.getBranchId());
    }

    @Test
    void shouldRestoreDevice() {
        var d = Device.restore(1L, "DEV-001", "Device 1", "192.168.1.1", 8080, "Office", 1L, true, java.time.LocalDateTime.now());
        assertEquals(1L, d.getId());
        assertEquals("DEV-001", d.getCode());
    }

    @Test
    void shouldActivateAndDeactivate() {
        var d = Device.create("DEV-001", null, null, null, null, null);
        assertTrue(d.isState());
        d.deactivate();
        assertFalse(d.isState());
        d.activate();
        assertTrue(d.isState());
    }

    @Test
    void shouldUpdateLocation() {
        var d = Device.create("DEV-001", null, null, null, null, null);
        d.updateLocation("10.0.0.1", 9090, "Warehouse");
        assertEquals("10.0.0.1", d.getIp());
        assertEquals(9090, d.getPuerto());
        assertEquals("Warehouse", d.getUbicacion());
    }

    @Test
    void shouldThrowWhenCodeIsNull() {
        assertThrows(CoreHrValidationException.class, () -> Device.create(null, null, null, null, null, null));
    }

    @Test
    void shouldThrowWhenCodeIsBlank() {
        assertThrows(CoreHrValidationException.class, () -> Device.create("  ", null, null, null, null, null));
    }
}
