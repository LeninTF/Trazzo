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
        var event = new IncidentCreatedEvent("inc-1", "u-1", "t-1", now);

        assertEquals("inc-1", event.incidentId());
        assertEquals("u-1", event.tenantUserId());
        assertEquals("t-1", event.incidentTypeId());
        assertEquals(now, event.occurredAt());
    }

    @Test
    void incidentStateChangedEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentStateChangedEvent("inc-1", "u-1",
                IncidentState.PENDIENTE, IncidentState.APROBADO, null, now);

        assertEquals("inc-1", event.incidentId());
        assertEquals("u-1", event.tenantUserId());
        assertEquals(IncidentState.PENDIENTE, event.previousState());
        assertEquals(IncidentState.APROBADO, event.newState());
        assertNull(event.rejectionReason());
    }

    @Test
    void incidentStateChangedEventWithRejection() {
        var now = LocalDateTime.now();
        var event = new IncidentStateChangedEvent("inc-1", "u-1",
                IncidentState.PENDIENTE, IncidentState.DENEGADO, "motivo", now);

        assertEquals("motivo", event.rejectionReason());
    }

    @Test
    void incidentJustificationRequestedEvent() {
        var now = LocalDateTime.now();
        var start = LocalDate.now();
        var end = start.plusDays(3);
        var event = new IncidentJustificationRequestedEvent("inc-1", "u-1", start, end, now);

        assertEquals("inc-1", event.incidentId());
        assertEquals(start, event.startDate());
        assertEquals(end, event.endDate());
    }

    @Test
    void incidentEvidenceRegisteredEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentEvidenceRegisteredEvent("inc-1", "ev-1", "doc.pdf", "http://url", now);

        assertEquals("inc-1", event.incidentId());
        assertEquals("ev-1", event.evidenceId());
        assertEquals("doc.pdf", event.fileName());
        assertEquals("http://url", event.fileKey());
    }

    @Test
    void incidentEvidenceDeletedEvent() {
        var now = LocalDateTime.now();
        var event = new IncidentEvidenceDeletedEvent("inc-1", "ev-1", now);

        assertEquals("inc-1", event.incidentId());
        assertEquals("ev-1", event.evidenceId());
    }

    @Test
    void allEventsImplementDomainEvent() {
        var now = LocalDateTime.now();
        assertInstanceOf(IncidentDomainEvent.class, new IncidentCreatedEvent("i", "u", "t", now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentStateChangedEvent("i", "u", IncidentState.PENDIENTE, IncidentState.APROBADO, null, now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentJustificationRequestedEvent("i", "u", LocalDate.now(), LocalDate.now(), now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentEvidenceRegisteredEvent("i", "e", "f", "k", now));
        assertInstanceOf(IncidentDomainEvent.class, new IncidentEvidenceDeletedEvent("i", "e", now));
    }
}
