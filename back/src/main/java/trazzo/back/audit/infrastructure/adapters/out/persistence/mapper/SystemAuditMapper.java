package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemAudit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SystemAuditEntity;

public final class SystemAuditMapper {

    private static final Logger log = LoggerFactory.getLogger(SystemAuditMapper.class);

    private SystemAuditMapper() {
    }

    public static SystemAudit toDomain(SystemAuditEntity entity) {
        HttpMethod httpMethod = null;
        try {
            httpMethod = HttpMethod.valueOf(entity.getAccionSistema().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.trace("Cannot map accionSistema '{}' to HttpMethod", entity.getAccionSistema());
        }

        return new SystemAudit(
                entity.getId(),
                null,
                null,
                entity.getModulo(),
                null,
                entity.getEntidadId(),
                httpMethod,
                entity.getEndpoint(),
                entity.getDescripcion(),
                AuditMapper.deserializeJson(entity.getValorAnterior()),
                AuditMapper.deserializeJson(entity.getValorNuevo()),
                entity.getIpAddress(),
                entity.getResultado(),
                entity.getDate()
        );
    }

    public static SystemAuditEntity toEntity(SystemAudit domain) {
        var entity = new SystemAuditEntity();
        entity.setId(domain.getId());
        entity.setAccionSistema(domain.getHttpMethod() != null ? domain.getHttpMethod().name() : null);
        entity.setModulo(domain.getModule());
        entity.setEntidadId(domain.getEntityId());
        entity.setEndpoint(domain.getEndpoint());
        entity.setDescripcion(domain.getDescription());
        entity.setValorAnterior(AuditMapper.serializeJson(domain.getPreviousValue()));
        entity.setValorNuevo(AuditMapper.serializeJson(domain.getNewValue()));
        entity.setIpAddress(domain.getIpAddress());
        entity.setResultado(domain.getResult());
        entity.setDate(domain.getCreatedAt());
        return entity;
    }
}
