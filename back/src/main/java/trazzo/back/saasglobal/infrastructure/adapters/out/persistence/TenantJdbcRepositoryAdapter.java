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
import trazzo.back.saasglobal.infrastructure.security.EncryptionService;

@Repository
@RequiredArgsConstructor
public class TenantJdbcRepositoryAdapter implements TenantRepositoryPort {

    private static final String SELECT_COLUMNS = """
            SELECT
                t."id"              AS t_id,
                t."holdingId"       AS t_holding_id,
                t."subDomain"       AS t_sub_domain,
                t."planId"          AS t_plan_id,
                t."activatedAt"     AS t_activated_at,
                t."createdAt"       AS t_created_at,
                t."updatedAt"       AS t_updated_at,
                t."deletedAt"       AS t_deleted_at,
                ts."dbName"         AS ts_db_name,
                ts."dbHost"         AS ts_db_host,
                ts."dbPort"         AS ts_db_port,
                ts."dbUser"         AS ts_db_user,
                ts."dbPassword"     AS ts_db_password,
                ts."createdAt"      AS ts_created_at,
                ts."updatedAt"      AS ts_updated_at,
                tb."logoUrl"        AS tb_logo_url,
                tb."slogan"         AS tb_slogan,
                tb."primaryColor"   AS tb_primary_color,
                tb."secondaryColor" AS tb_secondary_color,
                tb."createdAt"      AS tb_created_at,
                tb."updatedAt"      AS tb_updated_at
            FROM "Tenants" t
            LEFT JOIN "TenantSettings" ts ON ts."tenantId" = t."id"
            LEFT JOIN "TenantBranding"  tb ON tb."tenantId" = t."id"
            """;

    private final JdbcTemplate jdbc;
    private final EncryptionService encryptionService;

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
        List<Tenant> rows = jdbc.query(SELECT_COLUMNS + " WHERE t.\"id\" = ?::uuid", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Tenant> findBySubDomain(String subDomain) {
        List<Tenant> rows = jdbc.query(SELECT_COLUMNS + " WHERE t.\"subDomain\" = ?", this::mapRow, subDomain);
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsBySubDomain(String subDomain) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Tenants\" WHERE \"subDomain\" = ?", Integer.class, subDomain);
        return count != null && count > 0;
    }

    private void upsertTenant(Tenant tenant) {
        jdbc.update("""
                INSERT INTO "Tenants"
                    ("id", "holdingId", "subDomain", "planId", "activatedAt", "createdAt", "updatedAt", "deletedAt")
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("id") DO UPDATE SET
                    "planId"      = EXCLUDED."planId",
                    "activatedAt" = EXCLUDED."activatedAt",
                    "updatedAt"   = EXCLUDED."updatedAt",
                    "deletedAt"   = EXCLUDED."deletedAt"
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
                INSERT INTO "TenantSettings"
                    ("tenantId", "dbName", "dbHost", "dbPort", "dbUser", "dbPassword", "createdAt", "updatedAt")
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("tenantId") DO UPDATE SET
                    "dbName"     = EXCLUDED."dbName",
                    "dbHost"     = EXCLUDED."dbHost",
                    "dbPort"     = EXCLUDED."dbPort",
                    "dbUser"     = EXCLUDED."dbUser",
                    "dbPassword" = EXCLUDED."dbPassword",
                    "updatedAt"  = EXCLUDED."updatedAt"
                """,
                tenant.getId(),
                s.getDbName(), s.getDbHost(), s.getDbPort(), s.getDbUser(),
                encryptionService.encrypt(s.getDbPassword()),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    private void upsertBranding(Tenant tenant) {
        TenantBranding b = tenant.getBranding();
        jdbc.update("""
                INSERT INTO "TenantBranding"
                    ("tenantId", "logoUrl", "slogan", "primaryColor", "secondaryColor", "createdAt", "updatedAt")
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ("tenantId") DO UPDATE SET
                    "logoUrl"        = EXCLUDED."logoUrl",
                    "slogan"         = EXCLUDED."slogan",
                    "primaryColor"   = EXCLUDED."primaryColor",
                    "secondaryColor" = EXCLUDED."secondaryColor",
                    "updatedAt"      = EXCLUDED."updatedAt"
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
                    encryptionService.decrypt(rs.getString("ts_db_password")),
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
                rs.getObject("t_created_at", LocalDateTime.class),
                rs.getObject("t_updated_at", LocalDateTime.class),
                rs.getObject("t_deleted_at", LocalDateTime.class));
    }
}
