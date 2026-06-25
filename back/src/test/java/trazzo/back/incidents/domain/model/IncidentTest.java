package trazzo.back.incidents.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IncidentTest {

    /* == CREATION TESTS == */

    @Test
    void createWithValidFields() {
        var before = LocalDateTime.now();
        var incident = Incident.create("user-1", "type-1", "Comentario");
        var after = LocalDateTime.now();

        assertNull(incident.getId());
        assertEquals("user-1", incident.getTenantUserId());
        assertEquals("type-1", incident.getIncidentTypeId());
        assertEquals(IncidentState.PENDIENTE, incident.getState());
        assertEquals("Comentario", incident.getComment());
        assertNull(incident.getRejectionReason());
        assertNull(incident.getType());
        assertNull(incident.getPermission());
        assertTrue(incident.getEvidences().isEmpty());
        assertNotNull(incident.getCreatedAt());
        assertNotNull(incident.getUpdatedAt());
        assertFalse(incident.getCreatedAt().isBefore(before));
        assertFalse(incident.getCreatedAt().isAfter(after));
        assertFalse(incident.getUpdatedAt().isBefore(before));
        assertFalse(incident.getUpdatedAt().isAfter(after));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankTenantUserIdThrowsException(String tenantUserId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Incident.create(tenantUserId, "type-1", "comment")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankIncidentTypeIdThrowsException(String incidentTypeId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> Incident.create("user-1", incidentTypeId, "comment")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankCommentNormalizesToNull(String comment) {
        var incident = Incident.create("user-1", "type-1", comment);
        assertNull(incident.getComment());
    }

    /* == RESTORATION TESTS == */

    @Test
    void restoreWithAllFields() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "application/pdf",
                1024, false, null, now, now
        );
        var type = IncidentType.create("Urgente", "Desc");
        var permission = IncidentPermission.restore(
                "perm-1", "inc-1", LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now
        );

        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.APROBADO,
                "comment", "reason", type, permission, List.of(evidence),
                now, now
        );

        assertEquals("inc-1", incident.getId());
        assertEquals("user-1", incident.getTenantUserId());
        assertEquals("type-1", incident.getIncidentTypeId());
        assertEquals(IncidentState.APROBADO, incident.getState());
        assertEquals("comment", incident.getComment());
        assertEquals("reason", incident.getRejectionReason());
        assertSame(type, incident.getType());
        assertSame(permission, incident.getPermission());
        assertEquals(1, incident.getEvidences().size());
        assertSame(evidence, incident.getEvidences().getFirst());
        assertEquals(now, incident.getCreatedAt());
        assertEquals(now, incident.getUpdatedAt());
    }

    @Test
    void restoreWithBlankIdNormalizesToNull() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                " ", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        assertNull(incident.getId());
    }

    @Test
    void restoreWithBlankTenantUserIdThrowsException() {
        var now = LocalDateTime.now();
        assertThrows(
                IllegalArgumentException.class,
                () -> Incident.restore(
                        "id-1", " ", "type-1", IncidentState.PENDIENTE,
                        null, null, null, null, List.of(), now, now
                )
        );
    }

    @Test
    void restoreWithNullStateThrowsException() {
        var now = LocalDateTime.now();
        assertThrows(
                IllegalArgumentException.class,
                () -> Incident.restore(
                        "id-1", "user-1", "type-1", null,
                        null, null, null, null, List.of(), now, now
                )
        );
    }

    @Test
    void restoreWithMultipleActiveEvidencesThrowsException() {
        var now = LocalDateTime.now();
        var ev1 = IncidentEvidence.restore(
                "ev-1", "inc-1", "a.pdf", "http://a", "pdf", 100, false, null, now, now
        );
        var ev2 = IncidentEvidence.restore(
                "ev-2", "inc-1", "b.pdf", "http://b", "pdf", 200, false, null, now, now
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Incident.restore(
                        "id-1", "user-1", "type-1", IncidentState.PENDIENTE,
                        null, null, null, null, List.of(ev1, ev2), now, now
                )
        );
    }

    @Test
    void restoreWithDeletedEvidencesAllowsMultiple() {
        var now = LocalDateTime.now();
        var ev1 = IncidentEvidence.restore(
                "ev-1", "inc-1", "a.pdf", "http://a", "pdf", 100, true, null, now, now
        );
        var ev2 = IncidentEvidence.restore(
                "ev-2", "inc-1", "b.pdf", "http://b", "pdf", 200, true, null, now, now
        );
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(ev1, ev2), now, now
        );
        assertEquals(2, incident.getEvidences().size());
    }

    /* == COMMENT UPDATE TESTS == */

    @Test
    void updateCommentSuccessfully() {
        var incident = Incident.create("user-1", "type-1", "Original");
        incident.updateComment("Modificado");

        assertEquals("Modificado", incident.getComment());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void updateCommentWithBlankNormalizesToNull(String comment) {
        var incident = Incident.create("user-1", "type-1", "Original");
        incident.updateComment(comment);

        assertNull(incident.getComment());
    }

    @Test
    void updateCommentWhenNotPendingThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        assertThrows(
                IllegalStateException.class,
                () -> incident.updateComment("nuevo")
        );
    }

    @Test
    void updateCommentUpdatesUpdatedAt() {
        var incident = Incident.create("user-1", "type-1", "Original");
        var originalUpdatedAt = incident.getUpdatedAt();
        incident.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        incident.updateComment("Modificado");

        assertTrue(incident.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == ATTACH TYPE TESTS == */

    @Test
    void attachType() {
        var incident = Incident.create("user-1", "type-1", "comment");
        var type = IncidentType.create("Urgente", "Desc");

        assertNull(incident.getType());
        incident.attachType(type);

        assertSame(type, incident.getType());
    }

    /* == ADD EVIDENCE TESTS == */

    @Test
    void addEvidenceToPendingIncident() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        var evidence = IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);

        incident.addEvidence(evidence);

        assertEquals(1, incident.getEvidences().size());
        assertSame(evidence, incident.getEvidences().getFirst());
    }

    @Test
    void addEvidenceWhenEvidenceIsNullThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> incident.addEvidence(null)
        );
    }

    @Test
    void addEvidenceWhenActiveEvidenceExistsThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        var ev1 = IncidentEvidence.create("inc-1", "a.pdf", "http://a", "pdf", 100);
        var ev2 = IncidentEvidence.create("inc-1", "b.pdf", "http://b", "pdf", 200);

        incident.addEvidence(ev1);
        assertThrows(
                IllegalStateException.class,
                () -> incident.addEvidence(ev2)
        );
    }

    @Test
    void addEvidenceWhenEvidenceDoesNotBelongToIncidentThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        var evidence = IncidentEvidence.create("other-incident", "doc.pdf", "http://url", "pdf", 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> incident.addEvidence(evidence)
        );
    }

    @Test
    void addEvidenceWhenNotPendingThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        var evidence = IncidentEvidence.create("user-1", "doc.pdf", "http://url", "pdf", 100);

        assertThrows(
                IllegalStateException.class,
                () -> incident.addEvidence(evidence)
        );
    }

    @Test
    void addEvidenceToIncidentWithoutIdThrowsException() {
        var incident = Incident.create("user-1", "type-1", "comment");
        var evidence = IncidentEvidence.create("different-id", "doc.pdf", "http://url", "pdf", 100);

        assertThrows(
                IllegalStateException.class,
                () -> incident.addEvidence(evidence)
        );
    }

    @Test
    void addEvidenceUpdatesUpdatedAt() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        var evidence = IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);
        var originalUpdatedAt = incident.getUpdatedAt();
        incident.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        incident.addEvidence(evidence);

        assertTrue(incident.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == DELETE EVIDENCE TESTS == */

    @Test
    void deleteEvidenceFromPendingIncident() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, now, now
        );
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now
        );

        assertFalse(evidence.isDeleted());
        incident.deleteEvidence(evidence.getId());

        assertTrue(evidence.isDeleted());
        assertNotNull(evidence.getDeletedAt());
    }

    @Test
    void deleteEvidenceWithBlankIdThrowsException() {
        var incident = Incident.create("user-1", "type-1", "comment");
        assertThrows(
                IllegalArgumentException.class,
                () -> incident.deleteEvidence(" ")
        );
    }

    @Test
    void deleteEvidenceNotFoundThrowsException() {
        var incident = Incident.create("user-1", "type-1", "comment");
        assertThrows(
                IllegalArgumentException.class,
                () -> incident.deleteEvidence("non-existent")
        );
    }

    @Test
    void deleteEvidenceWhenNotPendingThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        assertThrows(
                IllegalStateException.class,
                () -> incident.deleteEvidence("any-id")
        );
    }

    @Test
    void deleteEvidenceUpdatesUpdatedAt() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf",
                100, false, null, now, now
        );
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now
        );
        var originalUpdatedAt = incident.getUpdatedAt();
        incident.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        incident.deleteEvidence(evidence.getId());

        assertTrue(incident.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == APPROVE TESTS == */

    @Test
    void approvePendingIncident() {
        var incident = Incident.create("user-1", "type-1", "comment");

        assertEquals(IncidentState.PENDIENTE, incident.getState());
        incident.approve();

        assertEquals(IncidentState.APROBADO, incident.getState());
        assertTrue(incident.isApproved());
        assertFalse(incident.isPending());
    }

    @Test
    void approveWhenNotPendingThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        assertThrows(
                IllegalStateException.class,
                () -> incident.approve()
        );
    }

    @Test
    void approveUpdatesUpdatedAt() {
        var incident = Incident.create("user-1", "type-1", "comment");
        var originalUpdatedAt = incident.getUpdatedAt();
        incident.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        incident.approve();

        assertTrue(incident.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == APPROVE WITH PERMISSION TESTS == */

    @Test
    void approveWithPermissionCreatesPermissionAndChangesState() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );

        var startDate = LocalDate.now();
        var endDate = startDate.plusDays(5);
        incident.approveWithPermission(startDate, endDate, 5);

        assertEquals(IncidentState.APROBADO, incident.getState());
        assertNotNull(incident.getPermission());
        assertEquals(startDate, incident.getPermission().getStartDate());
        assertEquals(endDate, incident.getPermission().getEndDate());
        assertEquals(5, incident.getPermission().getDaysGranted());
        assertTrue(incident.isApproved());
    }

    @Test
    void approveWithPermissionWhenIdIsNullThrowsException() {
        var incident = Incident.create("user-1", "type-1", "comment");

        assertThrows(
                IllegalStateException.class,
                () -> incident.approveWithPermission(LocalDate.now(), LocalDate.now().plusDays(1), 1)
        );
    }

    @Test
    void approveWithPermissionUpdatesUpdatedAt() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now
        );
        var originalUpdatedAt = incident.getUpdatedAt();
        incident.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        incident.approveWithPermission(LocalDate.now(), LocalDate.now().plusDays(1), 1);

        assertTrue(incident.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == DENY TESTS == */

    @Test
    void denyPendingIncidentWithReason() {
        var incident = Incident.create("user-1", "type-1", "comment");

        assertEquals(IncidentState.PENDIENTE, incident.getState());
        incident.deny("No cumple requisitos");

        assertEquals(IncidentState.DENEGADO, incident.getState());
        assertEquals("No cumple requisitos", incident.getRejectionReason());
        assertTrue(incident.isDenied());
        assertFalse(incident.isPending());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void denyWithBlankRejectionReasonThrowsException(String reason) {
        var incident = Incident.create("user-1", "type-1", "comment");
        assertThrows(
                IllegalArgumentException.class,
                () -> incident.deny(reason)
        );
    }

    @Test
    void denyWhenNotPendingThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        assertThrows(
                IllegalStateException.class,
                () -> incident.deny("reason")
        );
    }

    @Test
    void denyUpdatesUpdatedAt() {
        var incident = Incident.create("user-1", "type-1", "comment");
        var originalUpdatedAt = incident.getUpdatedAt();
        incident.clock = Clock.fixed(
                originalUpdatedAt.plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        incident.deny("Razón");

        assertTrue(incident.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == STATE QUERY TESTS == */

    @Test
    void isPendingReturnsTrueForPendingState() {
        var incident = Incident.create("user-1", "type-1", "comment");
        assertTrue(incident.isPending());
    }

    @Test
    void isPendingReturnsFalseForNonPendingState() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        assertFalse(incident.isPending());
    }

    @Test
    void isApprovedReturnsTrueForApprovedState() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.APROBADO,
                null, null, null, null, List.of(), now, now
        );
        assertTrue(incident.isApproved());
    }

    @Test
    void isApprovedReturnsFalseForNonApprovedState() {
        var incident = Incident.create("user-1", "type-1", "comment");
        assertFalse(incident.isApproved());
    }

    @Test
    void isDeniedReturnsTrueForDeniedState() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(
                "id-1", "user-1", "type-1", IncidentState.DENEGADO,
                null, null, null, null, List.of(), now, now
        );
        assertTrue(incident.isDenied());
    }

    @Test
    void isDeniedReturnsFalseForNonDeniedState() {
        var incident = Incident.create("user-1", "type-1", "comment");
        assertFalse(incident.isDenied());
    }

    /* == VALIDATION TESTS == */

    @Test
    void createTrimsWhitespaceFromTenantUserId() {
        var incident = Incident.create("  user-1  ", "type-1", "comment");
        assertEquals("user-1", incident.getTenantUserId());
    }

    @Test
    void createTrimsWhitespaceFromIncidentTypeId() {
        var incident = Incident.create("user-1", "  type-1  ", "comment");
        assertEquals("type-1", incident.getIncidentTypeId());
    }

    @Test
    void createTrimsWhitespaceFromComment() {
        var incident = Incident.create("user-1", "type-1", "  comment  ");
        assertEquals("comment", incident.getComment());
    }

    @Test
    void updateCommentTrimsWhitespace() {
        var incident = Incident.create("user-1", "type-1", "original");
        incident.updateComment("  nuevo  ");
        assertEquals("nuevo", incident.getComment());
    }

    @Test
    void getEvidencesReturnsUnmodifiableList() {
        var evidence = IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100);
        var incident = Incident.restore(
                "inc-1", "user-1", "type-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence),
                LocalDateTime.now(), LocalDateTime.now()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> incident.getEvidences().add(
                        IncidentEvidence.create("inc-1", "b.pdf", "http://b", "pdf", 200)
                )
        );
    }

}
