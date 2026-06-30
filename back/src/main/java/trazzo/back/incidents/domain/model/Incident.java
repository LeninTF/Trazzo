package trazzo.back.incidents.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.incidents.domain.event.IncidentCreatedEvent;
import trazzo.back.incidents.domain.event.IncidentDomainEvent;
import trazzo.back.incidents.domain.event.IncidentEvidenceDeletedEvent;
import trazzo.back.incidents.domain.event.IncidentEvidenceRegisteredEvent;
import trazzo.back.incidents.domain.event.IncidentJustificationRequestedEvent;
import trazzo.back.incidents.domain.event.IncidentStateChangedEvent;
import trazzo.back.incidents.domain.exception.InactiveIncidentTypeException;
import trazzo.back.incidents.domain.exception.IncidentValidationException;
import trazzo.back.incidents.domain.exception.InvalidIncidentEvidenceException;
import trazzo.back.incidents.domain.exception.InvalidIncidentPermissionException;
import trazzo.back.incidents.domain.exception.InvalidIncidentStateException;
import trazzo.back.incidents.domain.specification.ActiveIncidentTypeSpec;
import trazzo.back.incidents.domain.specification.IncidentEvidenceSpec;
import trazzo.back.incidents.domain.specification.IncidentStateTransitionSpec;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident {

    private String id;
    private String tenantUserId;
    private String incidentTypeId;
    private IncidentState state;
    private String comment;
    private String rejectionReason;
    private IncidentType type;
    private IncidentPermission permission;
    private List<IncidentEvidence> evidences = new ArrayList<>();
    private transient List<IncidentDomainEvent> domainEvents = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonIgnore
    transient Clock clock = Clock.systemDefaultZone();

    private Incident(
            String id,
            String tenantUserId,
            String incidentTypeId,
            IncidentState state,
            String comment,
            String rejectionReason,
            IncidentType type,
            IncidentPermission permission,
            List<IncidentEvidence> evidences,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = normalizeOptionalId(id);
        this.tenantUserId = requireText(tenantUserId, "tenantUserId");
        this.incidentTypeId = requireText(incidentTypeId, "incidentTypeId");
        this.state = requireState(state);
        this.comment = normalizeOptionalText(comment);
        this.rejectionReason = normalizeOptionalText(rejectionReason);
        this.type = type;
        this.permission = permission;
        this.evidences = normalizeEvidences(evidences);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validateAggregateConsistency();
    }

    private void validateAggregateConsistency() {
        if (id == null) {
            return;
        }
        if (permission != null && !permission.belongsTo(id)) {
            throw new InvalidIncidentPermissionException("permission does not belong to this incident");
        }
        for (IncidentEvidence evidence : this.evidences) {
            if (!evidence.belongsTo(id)) {
                throw new InvalidIncidentEvidenceException("evidence does not belong to this incident");
            }
        }
    }

    public static Incident create(String tenantUserId, String incidentTypeId, String comment) {
        LocalDateTime now = LocalDateTime.now();
        Incident incident = new Incident(
                generateId(),
                tenantUserId,
                incidentTypeId,
                IncidentState.PENDIENTE,
                comment,
                null,
                null,
                null,
                Collections.emptyList(),
                now,
                now
        );
        incident.recordEvent(new IncidentCreatedEvent(
                incident.getId(),
                incident.getTenantUserId(),
                incident.getIncidentTypeId(),
                now
        ));
        return incident;
    }

    public static Incident restore(
            String id,
            String tenantUserId,
            String incidentTypeId,
            IncidentState state,
            String comment,
            String rejectionReason,
            IncidentType type,
            IncidentPermission permission,
            List<IncidentEvidence> evidences,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Incident(
                id,
                tenantUserId,
                incidentTypeId,
                state,
                comment,
                rejectionReason,
                type,
                permission,
                evidences,
                createdAt,
                updatedAt
        );
    }

    public List<IncidentEvidence> getEvidences() {
        return Collections.unmodifiableList(evidences);
    }

    @JsonIgnore
    public List<IncidentDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public List<IncidentDomainEvent> pullDomainEvents() {
        List<IncidentDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public void updateComment(String comment) {
        requirePending("Only pending incidents can update comment");
        this.comment = normalizeOptionalText(comment);
        touch();
    }

    public void attachType(IncidentType type) {
        requirePending("Only pending incidents can accept a type");
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (!new ActiveIncidentTypeSpec().isSatisfiedBy(type)) {
            throw new InactiveIncidentTypeException("incident type must be active");
        }
        this.type = type;
        touch();
    }

    public void addEvidence(IncidentEvidence evidence) {
        requirePersistedId();
        requirePending("Only pending incidents can add evidence");
        if (evidence == null) {
            throw new IllegalArgumentException("evidence is required");
        }
        if (hasActiveEvidence()) {
            throw new InvalidIncidentStateException("Only one active evidence is allowed");
        }
        if (!evidence.belongsTo(id)) {
            throw new InvalidIncidentEvidenceException("evidence does not belong to this incident");
        }
        this.evidences.add(evidence);
        touch();
        recordEvent(new IncidentEvidenceRegisteredEvent(id, evidence.getId(), evidence.getFileName(), evidence.getFileKey(), updatedAt));
    }

    public void deleteEvidence(String evidenceId) {
        requirePending("Only pending incidents can delete evidence");
        IncidentEvidence evidence = findEvidence(evidenceId);
        if (!evidence.canBeDeleted(clock)) {
            throw new InvalidIncidentStateException("Evidence can only be deleted within 15 minutes after upload");
        }
        evidence.markAsDeleted();
        touch();
        recordEvent(new IncidentEvidenceDeletedEvent(id, evidence.getId(), updatedAt));
    }

    public void approve() {
        changePendingStateTo(IncidentState.APROBADO, null);
        recordEvent(new IncidentJustificationRequestedEvent(id, tenantUserId, null, null, updatedAt));
    }

    public void approveWithPermission(LocalDate startDate, LocalDate endDate, int daysGranted) {
        requirePersistedId();
        this.permission = IncidentPermission.create(id, startDate, endDate, daysGranted);
        changePendingStateTo(IncidentState.APROBADO, null);
        recordEvent(new IncidentJustificationRequestedEvent(id, tenantUserId, startDate, endDate, updatedAt));
    }

    public void deny(String rejectionReason) {
        changePendingStateTo(IncidentState.DENEGADO, requireText(rejectionReason, "rejectionReason"));
    }

    public boolean isPending() {
        return state == IncidentState.PENDIENTE;
    }

    public boolean isApproved() {
        return state == IncidentState.APROBADO;
    }

    public boolean isDenied() {
        return state == IncidentState.DENEGADO;
    }

    private void changePendingStateTo(IncidentState targetState, String rejectionReason) {
        IncidentState previousState = this.state;
        if (!new IncidentStateTransitionSpec().canTransition(previousState, targetState)) {
            throw new InvalidIncidentStateException("Only pending incidents can change state to APROBADO or DENEGADO");
        }
        this.state = targetState;
        this.rejectionReason = normalizeOptionalText(rejectionReason);
        touch();
        recordEvent(new IncidentStateChangedEvent(id, tenantUserId, previousState, targetState, this.rejectionReason, updatedAt));
    }

    private void requirePending(String message) {
        if (!isPending()) {
            throw new InvalidIncidentStateException(message);
        }
    }

    private void requirePersistedId() {
        if (id == null || id.isBlank()) {
            throw new InvalidIncidentStateException("incident id is required to create a permission");
        }
    }

    private boolean hasActiveEvidence() {
        return evidences.stream().anyMatch(evidence -> !evidence.isDeleted());
    }

    private IncidentEvidence findEvidence(String evidenceId) {
        String normalizedId = requireText(evidenceId, "evidenceId");
        return evidences.stream()
                .filter(evidence -> normalizedId.equals(evidence.getId()))
                .findFirst()
                .orElseThrow(() -> new InvalidIncidentEvidenceException("evidence was not found"));
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private void recordEvent(IncidentDomainEvent event) {
        this.domainEvents.add(event);
    }

    private static IncidentState requireState(IncidentState state) {
        if (state == null) {
            throw new IncidentValidationException("state is required");
        }
        return state;
    }

    private static List<IncidentEvidence> normalizeEvidences(List<IncidentEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return new ArrayList<>();
        }
        if (!new IncidentEvidenceSpec().allowsSingleActiveEvidence(evidences)) {
            throw new InvalidIncidentEvidenceException("Only one active evidence is allowed");
        }
        return new ArrayList<>(evidences);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IncidentValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }
}
