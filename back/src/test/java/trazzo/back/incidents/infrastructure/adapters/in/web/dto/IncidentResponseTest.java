package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.result.*;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class IncidentResponseTest {

    @Test
    void fromResultMapsAllFields() {
        var now = LocalDateTime.now();
        var typeResult = new IncidentTypeResult("t-1", "Permiso", "Desc", true, now, now);
        var evidenceResult = new IncidentEvidenceResult("ev-1", "inc-1", "doc.pdf",
                "http://url", "pdf", 100, now, now);
        var permissionResult = new IncidentPermissionResult("perm-1", "inc-1",
                LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now);
        var userResult = new IncidentResult.TenantUserBasicInfoResult(1L, "Juan",
                "Perez", "Lopez", "juan@mail.com");

        var result = new IncidentResult("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, typeResult, permissionResult, List.of(evidenceResult),
                userResult, now, now);

        var response = IncidentResponse.from(result);

        assertEquals("inc-1", response.id());
        assertEquals("u-1", response.tenantUserId());
        assertEquals(IncidentState.PENDIENTE, response.state());
        assertEquals("comment", response.comment());
        assertNotNull(response.tipo());
        assertNotNull(response.permiso());
        assertEquals(1, response.evidencias().size());
        assertNotNull(response.tenantUser());
        assertEquals("Juan", response.tenantUser().nombre());
    }

    @Test
    void fromResultWithNullOptionals() {
        var now = LocalDateTime.now();
        var result = new IncidentResult("inc-1", "u-1", "t-1", IncidentState.APROBADO,
                null, "reason", null, null, List.of(), null, now, now);

        var response = IncidentResponse.from(result);

        assertNull(response.tipo());
        assertNull(response.permiso());
        assertTrue(response.evidencias().isEmpty());
        assertNull(response.tenantUser());
        assertEquals("reason", response.rejectionReason());
    }
}
