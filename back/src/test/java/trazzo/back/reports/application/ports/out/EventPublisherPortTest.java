package trazzo.back.reports.application.ports.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EventPublisherPortTest {

    @Test
    void shouldBeInterface() {
        assertTrue(EventPublisherPort.class.isInterface());
    }

    @Test
    void shouldDefinePublishEventMethod() {
        try {
            EventPublisherPort.class.getDeclaredMethod("publishEvent", Object.class);
        } catch (NoSuchMethodException e) {
            fail("EventPublisherPort should define publishEvent(Object) method");
        }
    }
}
