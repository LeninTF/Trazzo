package trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentEvidence;
import trazzo.back.incidents.domain.model.IncidentPermission;
import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEvidenceEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class IncidentMapper {

    private IncidentMapper() {
    }

    public static IncidentEntity toEntity(Incident domain) {
        var entity = new IncidentEntity();
        entity.setId(toInt(domain.getId()));
        entity.setTenantUserId(toInt(domain.getTenantUserId()));
        entity.setIncidentTypeId(toInt(domain.getIncidentTypeId()));
        entity.setState(domain.getState());
        entity.setComment(domain.getComment());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getType() != null) {
            entity.setIncidentTypeId(toInt(domain.getType().getId()));
        }

        var evidenceEntities = Optional.ofNullable(domain.getEvidences())
                .orElse(Collections.emptyList())
                .stream()
                .map(IncidentMapper::toEntity)
                .toList();
        entity.setEvidences(evidenceEntities);

        if (domain.getPermission() != null) {
            entity.setPermission(toEntity(domain.getPermission()));
        }

        return entity;
    }

    public static Incident toDomain(IncidentEntity entity) {
        IncidentPermission permission = null;
        if (entity.getPermission() != null) {
            permission = toDomain(entity.getPermission());
        }

        List<IncidentEvidence> evidences = Optional.ofNullable(entity.getEvidences())
                .orElse(Collections.emptyList())
                .stream()
                .map(IncidentMapper::toDomain)
                .toList();

        return Incident.restore(
                toString(entity.getId()),
                toString(entity.getTenantUserId()),
                toString(entity.getIncidentTypeId()),
                entity.getState(),
                entity.getComment(),
                entity.getRejectionReason(),
                null,
                permission,
                evidences,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static IncidentEvidenceEntity toEntity(IncidentEvidence domain) {
        var entity = new IncidentEvidenceEntity();
        entity.setId(toInt(domain.getId()));
        entity.setIncidentId(toInt(domain.getIncidentId()));
        entity.setFileName(domain.getFileName());
        entity.setFileKey(domain.getFileKey());
        entity.setMimeType(domain.getMimeType());
        entity.setFileSize(domain.getFileSize());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setUploadedAt(domain.getUploadedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static IncidentEvidence toDomain(IncidentEvidenceEntity entity) {
        return IncidentEvidence.restore(
                toString(entity.getId()),
                toString(entity.getIncidentId()),
                entity.getFileName(),
                entity.getFileKey(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.isDeleted(),
                entity.getDeletedAt(),
                entity.getUploadedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static IncidentPermissionEntity toEntity(IncidentPermission domain) {
        var entity = new IncidentPermissionEntity();
        entity.setId(toInt(domain.getId()));
        entity.setIncidentId(toInt(domain.getIncidentId()));
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setDaysGranted(domain.getDaysGranted());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static IncidentPermission toDomain(IncidentPermissionEntity entity) {
        return IncidentPermission.restore(
                toString(entity.getId()),
                toString(entity.getIncidentId()),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDaysGranted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static Integer toInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toString(Integer value) {
        return value != null ? String.valueOf(value) : null;
    }
}
