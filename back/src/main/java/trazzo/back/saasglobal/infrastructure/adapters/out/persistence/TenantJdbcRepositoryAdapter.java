package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantBranding;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@Repository
@RequiredArgsConstructor
public class TenantJdbcRepositoryAdapter implements TenantRepositoryPort {

    private static final String SELECT_COLUMNS = """
            SELECT
                t.id               AS t_id,
                t.holding_id       AS t_holding_id,
                t.sub_domain       AS t_sub_domain,
                t.plan_id          AS t_plan_id,
                t.activated_at     AS t_activated_at,
                t.suspended_at     AS t_suspended_at,
                t.created_at       AS t_created_at,
                t.updated_at       AS t_updated_at,
                t.deleted_at       AS t_deleted_at,
                ts.schema_name     AS ts_schema_name,
                ts.created_at      AS ts_created_at,
                ts.updated_at      AS ts_updated_at,
                tb.logo_url        AS tb_logo_url,
                tb.slogan          AS tb_slogan,
                tb.primary_color   AS tb_primary_color,
                tb.secondary_color AS tb_secondary_color,
                tb.created_at      AS tb_created_at,
                tb.updated_at      AS tb_updated_at
            FROM tenants t
            LEFT JOIN tenant_settings ts ON ts.tenant_id = t.id
            LEFT JOIN tenant_branding  tb ON tb.tenant_id = t.id
            """;

    private static final String LIST_FILTER_WHERE = """
            WHERE t.deleted_at IS NULL
              AND (:search IS NULL OR t.sub_domain ILIKE CONCAT('%', CAST(:search AS varchar), '%'))
              AND (:planId IS NULL OR t.plan_id = :planId)
              AND (
                    CAST(:status AS varchar) IS NULL
                    OR (:status = 'TRIAL' AND t.activated_at IS NULL)
                    OR (:status = 'ACTIVE' AND t.activated_at IS NOT NULL AND t.suspended_at IS NULL)
                    OR (:status = 'SUSPENDED' AND t.suspended_at IS NOT NULL)
                  )
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public Tenant save(Tenant tenant) {
        upsertTenant(tenant);
        if (tenant.getSettings() != null) {
            upsertSettings(tenant);
        }
        if (tenant.getBranding() != null) {
            upsertBranding(tenant);
        }
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(String id) {
        List<Tenant> rows = jdbc.query(SELECT_COLUMNS + " WHERE t.id = ?::uuid", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Tenant> findBySubDomain(String subDomain) {
        List<Tenant> rows = jdbc.query(SELECT_COLUMNS + " WHERE t.sub_domain = ?", this::mapRow, subDomain);
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsBySubDomain(String subDomain) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE sub_domain = ?", Integer.class, subDomain);
        return count != null && count > 0;
    }

    @Override
    public void purgeById(String id) {
        jdbc.update("DELETE FROM tenants WHERE id = ?::uuid", id);
    }

    @Override
    public List<Tenant> findAbandonedTrials(LocalDateTime cutoff) {
        return jdbc.query(
                SELECT_COLUMNS + """
                 WHERE t.deleted_at IS NULL
                   AND t.created_at < ?
                   AND EXISTS (SELECT 1 FROM subscriptions s WHERE s.tenant_id = t.id AND s.status = 'TRIAL')
                   AND NOT EXISTS (
                       SELECT 1 FROM subscriptions s WHERE s.tenant_id = t.id AND s.status IN ('ACTIVE', 'SUSPENDED')
                   )
                """,
                this::mapRow, cutoff);
    }

    @Override
    public List<Tenant> findAll(String search, Integer planId, String status, int page, int size) {
        MapSqlParameterSource params = listFilterParams(search, planId, status)
                .addValue("limit", size)
                .addValue("offset", Math.max(page, 0) * size);
        return namedJdbc.query(
                SELECT_COLUMNS + LIST_FILTER_WHERE + " ORDER BY t.created_at DESC LIMIT :limit OFFSET :offset",
                params, this::mapRow);
    }

    @Override
    public long countAll(String search, Integer planId, String status) {
        Long count = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants t " + LIST_FILTER_WHERE,
                listFilterParams(search, planId, status), Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countTotal() {
        return countWhere("deleted_at IS NULL", new MapSqlParameterSource());
    }

    @Override
    public long countActive() {
        return countWhere("deleted_at IS NULL AND activated_at IS NOT NULL AND suspended_at IS NULL",
                new MapSqlParameterSource());
    }

    @Override
    public long countCreatedSince(LocalDateTime since) {
        return countWhere("deleted_at IS NULL AND created_at >= :since",
                new MapSqlParameterSource("since", since));
    }

    @Override
    public long countTotalBefore(LocalDateTime cutoff) {
        return countWhere("deleted_at IS NULL AND created_at < :cutoff",
                new MapSqlParameterSource("cutoff", cutoff));
    }

    @Override
    public long countExistedBefore(LocalDateTime cutoff) {
        return countWhere("created_at < :cutoff", new MapSqlParameterSource("cutoff", cutoff));
    }

    @Override
    public long countDeletedBetween(LocalDateTime from, LocalDateTime toExclusive) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", toExclusive, Types.TIMESTAMP);
        return countWhere("deleted_at >= :from AND (CAST(:to AS timestamp) IS NULL OR deleted_at < :to)", params);
    }

    // whereClause is always a compile-time string literal from the private count methods
    // above in this class, never user input; actual values are bound via named parameters.
    @SuppressWarnings("java:S2077")
    private long countWhere(String whereClause, MapSqlParameterSource params) {
        Long count = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE " + whereClause, params, Long.class);
        return count != null ? count : 0L;
    }

    private static MapSqlParameterSource listFilterParams(String search, Integer planId, String status) {
        return new MapSqlParameterSource()
                .addValue("search", search, Types.VARCHAR)
                .addValue("planId", planId, Types.INTEGER)
                .addValue("status", status, Types.VARCHAR);
    }

    private void upsertTenant(Tenant tenant) {
        jdbc.update("""
                INSERT INTO tenants
                    (id, holding_id, sub_domain, plan_id, activated_at, suspended_at, created_at, updated_at, deleted_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    plan_id      = EXCLUDED.plan_id,
                    activated_at = EXCLUDED.activated_at,
                    suspended_at = EXCLUDED.suspended_at,
                    updated_at   = EXCLUDED.updated_at,
                    deleted_at   = EXCLUDED.deleted_at
                """,
                tenant.getId(),
                tenant.getHoldingId(),
                tenant.getSubDomain(),
                tenant.getPlanId(),
                tenant.getActivatedAt(),
                tenant.getSuspendedAt(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt(),
                tenant.getDeletedAt());
    }

    private void upsertSettings(Tenant tenant) {
        TenantSettings s = tenant.getSettings();
        jdbc.update("""
                INSERT INTO tenant_settings
                    (tenant_id, schema_name, created_at, updated_at)
                VALUES (?::uuid, ?, ?, ?)
                ON CONFLICT (tenant_id) DO UPDATE SET
                    schema_name = EXCLUDED.schema_name,
                    updated_at  = EXCLUDED.updated_at
                """,
                tenant.getId(),
                s.getSchemaName(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    private void upsertBranding(Tenant tenant) {
        TenantBranding b = tenant.getBranding();
        jdbc.update("""
                INSERT INTO tenant_branding
                    (tenant_id, logo_url, slogan, primary_color, secondary_color, created_at, updated_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id) DO UPDATE SET
                    logo_url        = EXCLUDED.logo_url,
                    slogan          = EXCLUDED.slogan,
                    primary_color   = EXCLUDED.primary_color,
                    secondary_color = EXCLUDED.secondary_color,
                    updated_at      = EXCLUDED.updated_at
                """,
                tenant.getId(),
                b.getLogoUrl(), b.getSlogan(), b.getPrimaryColor(), b.getSecondaryColor(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }

    private Tenant mapRow(ResultSet rs, int rowNum) throws SQLException {
        String tenantId = rs.getString("t_id");

        TenantSettings settings = null;
        if (rs.getString("ts_schema_name") != null) {
            settings = TenantSettings.restore(
                    tenantId,
                    rs.getString("ts_schema_name"),
                    rs.getObject("ts_created_at", LocalDateTime.class),
                    rs.getObject("ts_updated_at", LocalDateTime.class));
        }

        TenantBranding branding = null;
        if (rs.getString("tb_logo_url") != null || rs.getString("tb_slogan") != null) {
            branding = TenantBranding.restore(
                    tenantId,
                    rs.getString("tb_logo_url"),
                    rs.getString("tb_slogan"),
                    rs.getString("tb_primary_color"),
                    rs.getString("tb_secondary_color"),
                    rs.getObject("tb_created_at", LocalDateTime.class),
                    rs.getObject("tb_updated_at", LocalDateTime.class));
        }

        return Tenant.restore(
                tenantId,
                rs.getObject("t_holding_id", Integer.class),
                rs.getString("t_sub_domain"),
                rs.getObject("t_plan_id", Integer.class),
                settings,
                branding,
                rs.getObject("t_activated_at", LocalDateTime.class),
                rs.getObject("t_suspended_at", LocalDateTime.class),
                rs.getObject("t_created_at", LocalDateTime.class),
                rs.getObject("t_updated_at", LocalDateTime.class),
                rs.getObject("t_deleted_at", LocalDateTime.class));
    }
}
