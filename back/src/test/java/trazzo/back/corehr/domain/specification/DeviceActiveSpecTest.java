package trazzo.back.corehr.domain.specification;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.attendance.Device;

class DeviceActiveSpecTest {

    private final DeviceActiveSpec spec = new DeviceActiveSpec();

    @Test
    void shouldBeSatisfiedWhenDeviceIsActive() {
        var d = Device.create("DEV-001", null, null, null, null, null);
        assertTrue(spec.isSatisfiedBy(d));
    }

    @Test
    void shouldNotBeSatisfiedWhenDeviceIsInactive() {
        var d = Device.create("DEV-001", null, null, null, null, null);
        d.deactivate();
        assertFalse(spec.isSatisfiedBy(d));
    }

    @Test
    void shouldNotBeSatisfiedWhenDeviceIsNull() {
        assertFalse(spec.isSatisfiedBy(null));
    }
}
