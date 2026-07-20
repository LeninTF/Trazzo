package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.AuditJpaRepository;
import trazzo.back.audit.infrastructure.adapters.out.persistence.util.JsonUtils;
import trazzo.back.shared.util.SortUtils;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditRepositoryAdapter implements AuditRepositoryPort {

    private final AuditJpaRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String CREATED_AT = "created_at";
    private static final String IP_ADDRESS = "ip_address";
    private static final Set<String> SORT_WHITELIST = Set.of(CREATED_AT, "entity", "action", "id", IP_ADDRESS);

    private static final RowMapper<Audit> ROW_MAPPER = (rs, rowNum) -> Audit.restore(
            rs.getString("id"),
            rs.getString("entity"),
            rs.getString("entity_id"),
            Action.valueOf(rs.getString("action")),
            rs.getString("user_id"),
            rs.getString("endpoint"),
            rs.getString(IP_ADDRESS),
            rs.getString("user_agent"),
            deserializeJson(rs.getString("old_value")),
            deserializeJson(rs.getString("new_value")),
            rs.getTimestamp(CREATED_AT) != null ? rs.getTimestamp(CREATED_AT).toLocalDateTime() : null
    );

    @Override
    public List<Audit> findAll(String searchTerm, String tenantId, Action action, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta, org.springframework.data.domain.Pageable pageable) {
        var sql = new StringBuilder("SELECT a.* FROM audit a WHERE 1=1");
        var params = new ArrayList<SqlParameterValue>();

        appendSearchClause(sql, params, searchTerm);
        appendTenantClause(sql, params, tenantId);
        appendActionClause(sql, params, action);
        appendEntityClause(sql, params, entity);
        appendDateClauses(sql, params, fechaDesde, fechaHasta);

        int offset = pageable.getPageNumber() * pageable.getPageSize();

        String sortField = CREATED_AT;
        String sortDirection = "DESC";
        if (!pageable.getSort().isEmpty()) {
            var order = pageable.getSort().stream().findFirst().orElse(null);
            if (order != null) {
                String mappedField = SortUtils.AUDIT_SORT_FIELD_MAP.getOrDefault(order.getProperty(), order.getProperty());
                if (SORT_WHITELIST.contains(mappedField)) {
                    sortField = mappedField;
                    sortDirection = order.isAscending() ? "ASC" : "DESC";
                }
            }
        }
        // SQL does not support parameterized ORDER BY; the whitelist is the sole defense against injection.
        sql.append(" ORDER BY a.").append(sortField).append(" ").append(sortDirection);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(new SqlParameterValue(Types.INTEGER, pageable.getPageSize()));
        params.add(new SqlParameterValue(Types.INTEGER, offset));

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    @Override
    public long count(String searchTerm, String tenantId, Action action, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        var sql = new StringBuilder("SELECT COUNT(*) FROM audit a WHERE 1=1");
        var params = new ArrayList<SqlParameterValue>();

        appendSearchClause(sql, params, searchTerm);
        appendTenantClause(sql, params, tenantId);
        appendActionClause(sql, params, action);
        appendEntityClause(sql, params, entity);
        appendDateClauses(sql, params, fechaDesde, fechaHasta);

        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class,
                params.toArray());
        return result != null ? result : 0;
    }

    @Override
    public Optional<Audit> findById(String id) {
        return jpaRepository.findById(UUID.fromString(id))
                .map(e -> Audit.restore(
                        e.getId() != null ? e.getId().toString() : null,
                        e.getEntity(),
                        e.getEntityId(),
                        e.getAction(),
                        e.getUserId() != null ? e.getUserId().toString() : null,
                        e.getEndpoint(),
                        e.getIpAddress(),
                        e.getUserAgent(),
                        deserializeJson(e.getOldValue()),
                        deserializeJson(e.getNewValue()),
                        e.getCreatedAt()));
    }

    private void appendSearchClause(StringBuilder sql, List<SqlParameterValue> params, String searchTerm) {
        if (searchTerm != null && !searchTerm.isBlank()) {
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            sql.append(" AND (LOWER(a.entity) LIKE ? OR LOWER(a.").append(IP_ADDRESS).append(") LIKE ? OR LOWER(a.user_id::text) LIKE ? OR LOWER(a.endpoint) LIKE ?)");
            params.add(new SqlParameterValue(Types.VARCHAR, pattern));
            params.add(new SqlParameterValue(Types.VARCHAR, pattern));
            params.add(new SqlParameterValue(Types.VARCHAR, pattern));
            params.add(new SqlParameterValue(Types.VARCHAR, pattern));
        }
    }

    private void appendTenantClause(StringBuilder sql, List<SqlParameterValue> params, String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(tenantId);
            } catch (IllegalArgumentException e) {
                return;
            }
            sql.append(" AND a.user_id IN (SELECT id FROM users WHERE tenant_id = ?)");
            params.add(new SqlParameterValue(Types.OTHER, uuid));
        }
    }

    private void appendActionClause(StringBuilder sql, List<SqlParameterValue> params, Action action) {
        if (action != null) {
            sql.append(" AND a.action = ?");
            params.add(new SqlParameterValue(Types.VARCHAR, action.name()));
        }
    }

    private void appendEntityClause(StringBuilder sql, List<SqlParameterValue> params, String entity) {
        if (entity != null && !entity.isBlank()) {
            sql.append(" AND a.entity = ?");
            params.add(new SqlParameterValue(Types.VARCHAR, entity));
        }
    }

    private void appendDateClauses(StringBuilder sql, List<SqlParameterValue> params,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        if (fechaDesde != null) {
            sql.append(" AND a.").append(CREATED_AT).append(" >= ?");
            params.add(new SqlParameterValue(Types.TIMESTAMP, fechaDesde));
        }
        if (fechaHasta != null) {
            sql.append(" AND a.").append(CREATED_AT).append(" <= ?");
            params.add(new SqlParameterValue(Types.TIMESTAMP, fechaHasta));
        }
    }

    static Map<String, Object> deserializeJson(String json) {
        return JsonUtils.deserialize(json);
    }
}
