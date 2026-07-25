package trazzo.back.incidents.domain.event;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDate;
import java.time.LocalDateTime;

class IncidentDomainEventTest {

    @Test
    void incidentCreatedEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentCreatedEvent(1, 1, 1, now);

        assertEquals(1, event.incidentId());
        assertEquals(1, event.tenantUserId());
        assertEquals(1, event.incidentTypeId());
        assertEquals(now, event.occurredAt());
    }

    @Test
    void incidentStateChangedEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentStateChangedEvent(1, 1,
                IncidentState.PENDIENTE, IncidentState.APROBADO, null, now);

        assertEquals(1, event.incidentId());
        assertEquals(1, event.tenantUserId());
        assertEquals(IncidentState.PENDIENTE, event.previousState());
        assertEquals(IncidentState.APROBADO, event.newState());
        assertNull(event.rejectionReason());
    }

    @Test
    void incidentStateChangedEventWithRejection() {
        var now = LocalDateTime.now();
        var event = new IncidentStateChangedEvent(1, 1,
                IncidentState.PENDIENTE, IncidentState.DENEGADO, "motivo", now);

        assertEquals("motivo", event.rejectionReason());
    }

    @Test
    void incidentJustificationRequestedEvent() {
        var now = LocalDateTime.now();
        var start = LocalDate.now();
        var end = start.plusDays(3);
        var event = new IncidentJustificationRequestedEvent(1, 1, start, end, now);

        assertEquals(1, event.incidentId());
        assertEquals(start, event.startDate());
        assertEquals(end, event.endDate());
    }

    @Test
    void incidentEvidenceRegisteredEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentEvidenceRegisteredEvent(1, 1, "doc.pdf", "http://url", now);

        assertEquals(1, event.incidentId());
        assertEquals(1, event.evidenceId());
        assertEquals("doc.pdf", event.fileName());
        assertEquals("http://url", event.fileKey());
    }

    @Test
    void incidentEvidenceDeletedEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentEvidenceDeletedEvent(1, 1, now);

        assertEquals(1, event.incidentId());
        assertEquals(1, event.evidenceId());
    }

    @Test
    void allEventsImplementDomainEvent() {
        var now = LocalDateTime.now();
        assertInstanceOf(IncidentDomainEvent.class, new IncidentCreatedEvent(1, 1, 1, now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentStateChangedEvent(1, 1, IncidentState.PENDIENTE, IncidentState.APROBADO, null, now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentJustificationRequestedEvent(1, 1, LocalDate.now(), LocalDate.now(), now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentEvidenceRegisteredEvent(1, 1, "f", "k", now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentEvidenceDeletedEvent(1, 1, now));
    }
}
