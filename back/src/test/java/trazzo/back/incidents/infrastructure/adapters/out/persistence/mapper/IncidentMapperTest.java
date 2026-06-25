package trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.*;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEvidenceEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class IncidentMapperTest {

    @Test
    void toEntityMapsIncidentFields() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);
        var permission = IncidentPermission.create("inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1);
        var domain = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, permission, List.of(evidence), now, now);

        var entity = IncidentMapper.toEntity(domain);

        assertEquals("inc-1", entity.getId());
        assertEquals("u-1", entity.getTenantUserId());
        assertEquals(IncidentState.PENDIENTE, entity.getState());
        assertEquals("comment", entity.getComment());
        assertNotNull(entity.getPermission());
    }

    @Test
    void toDomainMapsIncidentFields() {
        var now = LocalDateTime.now();
        var evidenceEntity = new IncidentEvidenceEntity("ev-1", "inc-1", "doc.pdf",
                "http://url", "pdf", 100, false, null, now, now, now);
        var permissionEntity = new IncidentPermissionEntity("perm-1", "inc-1",
                LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now);
        var entity = new IncidentEntity("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(evidenceEntity), permissionEntity);

        var domain = IncidentMapper.toDomain(entity);

        assertEquals("inc-1", domain.getId());
        assertEquals("u-1", domain.getTenantUserId());
        assertEquals(IncidentState.PENDIENTE, domain.getState());
        assertEquals("comment", domain.getComment());
        assertNotNull(domain.getPermission());
        assertEquals(1, domain.getEvidences().size());
    }

    @Test
    void roundTripPreservesIncident() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);
        var domain = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(evidence), now, now);

        var entity = IncidentMapper.toEntity(domain);
        var restored = IncidentMapper.toDomain(entity);

        assertEquals(domain.getId(), restored.getId());
        assertEquals(domain.getTenantUserId(), restored.getTenantUserId());
        assertEquals(domain.getState(), restored.getState());
        assertEquals(domain.getComment(), restored.getComment());
        assertEquals(1, restored.getEvidences().size());
    }

    @Test
    void evidenceRoundTrip() {
        var now = LocalDateTime.now();
        var ev = IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);
        var entity = IncidentMapper.toEntity(ev);
        var restored = IncidentMapper.toDomain(entity);

        assertEquals(ev.getIncidentId(), restored.getIncidentId());
        assertEquals(ev.getFileName(), restored.getFileName());
        assertEquals(ev.getFileUrl(), restored.getFileUrl());
        assertEquals(ev.getFileSize(), restored.getFileSize());
    }

    @Test
    void permissionRoundTrip() {
        var now = LocalDateTime.now();
        var perm = IncidentPermission.create("inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1);
        var entity = IncidentMapper.toEntity(perm);
        var restored = IncidentMapper.toDomain(entity);

        assertEquals(perm.getIncidentId(), restored.getIncidentId());
        assertEquals(perm.getStartDate(), restored.getStartDate());
        assertEquals(perm.getEndDate(), restored.getEndDate());
        assertEquals(perm.getDaysGranted(), restored.getDaysGranted());
    }
}
