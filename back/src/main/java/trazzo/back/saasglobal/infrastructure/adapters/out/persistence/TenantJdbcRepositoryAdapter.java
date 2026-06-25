package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
                t.create_at        AS t_create_at,
                t.update_at        AS t_update_at,
                t.deleted_at       AS t_deleted_at,
                ts.db_name         AS ts_db_name,
                ts.db_host         AS ts_db_host,
                ts.db_port         AS ts_db_port,
                ts.db_user         AS ts_db_user,
                ts.db_password     AS ts_db_password,
                ts.create_at       AS ts_create_at,
                ts.update_at       AS ts_update_at,
                tb.logo_url        AS tb_logo_url,
                tb.slogan          AS tb_slogan,
                tb.primary_color   AS tb_primary_color,
                tb.secondary_color AS tb_secondary_color,
                tb.create_at       AS tb_create_at,
                tb.update_at       AS tb_update_at
            FROM tenants t
            LEFT JOIN tenant_settings ts ON ts.tenant_id = t.id
            LEFT JOIN tenant_branding  tb ON tb.tenant_id = t.id
            """;

    private final JdbcTemplate jdbc;

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

    private void upsertTenant(Tenant tenant) {
        jdbc.update("""
                INSERT INTO tenants
                    (id, holding_id, sub_domain, plan_id, activated_at, create_at, update_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    plan_id      = EXCLUDED.plan_id,
                    activated_at = EXCLUDED.activated_at,
                    update_at    = EXCLUDED.update_at,
                    deleted_at   = EXCLUDED.deleted_at
                """,
                tenant.getId(),
                tenant.getHoldingId(),
                tenant.getSubDomain(),
                tenant.getPlanId(),
                tenant.getActivatedAt(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt(),
                tenant.getDeletedAt());
    }

    private void upsertSettings(Tenant tenant) {
        TenantSettings s = tenant.getSettings();
        jdbc.update("""
                INSERT INTO tenant_settings
                    (tenant_id, db_name, db_host, db_port, db_user, db_password, create_at, update_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id) DO UPDATE SET
                    db_name     = EXCLUDED.db_name,
                    db_host     = EXCLUDED.db_host,
                    db_port     = EXCLUDED.db_port,
                    db_user     = EXCLUDED.db_user,
                    db_password = EXCLUDED.db_password,
                    update_at   = EXCLUDED.update_at
                """,
                tenant.getId(),
                s.getDbName(), s.getDbHost(), s.getDbPort(), s.getDbUser(), s.getDbPassword(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    private void upsertBranding(Tenant tenant) {
        TenantBranding b = tenant.getBranding();
        jdbc.update("""
                INSERT INTO tenant_branding
                    (tenant_id, logo_url, slogan, primary_color, secondary_color, create_at, update_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id) DO UPDATE SET
                    logo_url        = EXCLUDED.logo_url,
                    slogan          = EXCLUDED.slogan,
                    primary_color   = EXCLUDED.primary_color,
                    secondary_color = EXCLUDED.secondary_color,
                    update_at       = EXCLUDED.update_at
                """,
                tenant.getId(),
                b.getLogoUrl(), b.getSlogan(), b.getPrimaryColor(), b.getSecondaryColor(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }

    private Tenant mapRow(ResultSet rs, int rowNum) throws SQLException {
        String tenantId = rs.getString("t_id");

        TenantSettings settings = null;
        if (rs.getString("ts_db_name") != null) {
            settings = TenantSettings.restore(
                    tenantId,
                    rs.getString("ts_db_name"),
                    rs.getString("ts_db_host"),
                    rs.getString("ts_db_port"),
                    rs.getString("ts_db_user"),
                    rs.getString("ts_db_password"),
                    rs.getObject("ts_create_at", LocalDateTime.class),
                    rs.getObject("ts_update_at", LocalDateTime.class));
        }

        TenantBranding branding = null;
        if (rs.getString("tb_logo_url") != null || rs.getString("tb_slogan") != null) {
            branding = TenantBranding.restore(
                    tenantId,
                    rs.getString("tb_logo_url"),
                    rs.getString("tb_slogan"),
                    rs.getString("tb_primary_color"),
                    rs.getString("tb_secondary_color"),
                    rs.getObject("tb_create_at", LocalDateTime.class),
                    rs.getObject("tb_update_at", LocalDateTime.class));
        }

        return Tenant.restore(
                tenantId,
                rs.getObject("t_holding_id", Integer.class),
                rs.getString("t_sub_domain"),
                rs.getObject("t_plan_id", Integer.class),
                settings,
                branding,
                rs.getObject("t_activated_at", LocalDateTime.class),
                rs.getObject("t_create_at", LocalDateTime.class),
                rs.getObject("t_update_at", LocalDateTime.class),
                rs.getObject("t_deleted_at", LocalDateTime.class));
    }
}
