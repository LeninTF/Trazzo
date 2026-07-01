package trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentEvidence;
import trazzo.back.incidents.domain.model.IncidentPermission;
import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEvidenceEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class IncidentMapper {

    private IncidentMapper() {
    }

    public static IncidentEntity toEntity(Incident domain) {
        var entity = new IncidentEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setIncidentTypeId(domain.getIncidentTypeId());
        entity.setState(domain.getState());
        entity.setComment(domain.getComment());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getType() != null) {
            entity.setIncidentTypeId(domain.getType().getId());
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
        IncidentType type = null;

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
                entity.getId(),
                entity.getTenantUserId(),
                entity.getIncidentTypeId(),
                entity.getState(),
                entity.getComment(),
                entity.getRejectionReason(),
                type,
                permission,
                evidences,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static IncidentEvidenceEntity toEntity(IncidentEvidence domain) {
        var entity = new IncidentEvidenceEntity();
        entity.setId(domain.getId());
        entity.setIncidentId(domain.getIncidentId());
        entity.setFileName(domain.getFileName());
        entity.setFileUrl(domain.getFileUrl());
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
                entity.getId(),
                entity.getIncidentId(),
                entity.getFileName(),
                entity.getFileUrl(),
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
        entity.setId(domain.getId());
        entity.setIncidentId(domain.getIncidentId());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setDaysGranted(domain.getDaysGranted());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static IncidentPermission toDomain(IncidentPermissionEntity entity) {
        return IncidentPermission.restore(
                entity.getId(),
                entity.getIncidentId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDaysGranted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
