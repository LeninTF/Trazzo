package trazzo.back.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tokens de dispositivo del agente en la tabla agent_device_tokens del master. Se consulta antes de que haya
 * TenantContext (es lo que lo fija), por eso usa el DataSource sin enrutar. Nunca guarda el token, solo su SHA-256.
 */
@Component
public class AgentDeviceTokenRepository {

    public record Issued(UUID tenantId, LocalDateTime createdAt) {}

    private static final RowMapper<DeviceAgentPrincipal> PRINCIPAL = (rs, rowNum) -> new DeviceAgentPrincipal(
            rs.getObject("tenant_id", UUID.class),
            rs.getString("schema_name"),
            rs.getLong("device_id"),
            rs.getString("device_code"));

    private static final String REVOKE_DEVICE =
            "UPDATE agent_device_tokens SET revoked_at = now() WHERE schema_name = ? AND device_id = ? AND revoked_at IS NULL";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public AgentDeviceTokenRepository(@Qualifier("rawDataSource") DataSource rawDataSource) {
        this.jdbc = new JdbcTemplate(rawDataSource);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(rawDataSource));
    }

    /** Revoca el token vigente del dispositivo y guarda el nuevo; si algo falla, el anterior sigue valiendo. */
    public Issued replace(String schemaName, long deviceId, String deviceCode, String token) {
        return transaction.execute(status -> {
            jdbc.update(REVOKE_DEVICE, schemaName, deviceId);
            List<Issued> inserted = jdbc.query("""
                    INSERT INTO agent_device_tokens (token_hash, tenant_id, schema_name, device_id, device_code)
                    SELECT ?, ts.tenant_id, ts.schema_name, ?, ? FROM tenant_settings ts WHERE ts.schema_name = ?
                    RETURNING tenant_id, created_at
                    """,
                    (rs, rowNum) -> new Issued(rs.getObject("tenant_id", UUID.class), rs.getObject("created_at", LocalDateTime.class)),
                    hash(token), deviceId, deviceCode, schemaName);
            if (inserted.isEmpty()) {
                throw new IllegalStateException("No hay un tenant con el schema " + schemaName);
            }
            return inserted.getFirst();
        });
    }

    /** El agente del token, si está vigente y su tenant no está suspendido ni borrado. */
    public Optional<DeviceAgentPrincipal> findActive(String token) {
        String tokenHash = hash(token);
        List<DeviceAgentPrincipal> found = jdbc.query("""
                SELECT t.tenant_id, t.schema_name, t.device_id, t.device_code
                FROM agent_device_tokens t JOIN tenants tn ON tn.id = t.tenant_id
                WHERE t.token_hash = ? AND t.revoked_at IS NULL AND tn.deleted_at IS NULL AND tn.suspended_at IS NULL
                """, PRINCIPAL, tokenHash);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        // Como mucho una escritura cada 5 minutos por token, aunque el agente consulte cada pocos segundos.
        jdbc.update("""
                UPDATE agent_device_tokens SET last_used_at = now()
                WHERE token_hash = ? AND (last_used_at IS NULL OR last_used_at < now() - INTERVAL '5 minutes')
                """, tokenHash);
        return Optional.of(found.getFirst());
    }

    public void revokeAll(String schemaName, long deviceId) {
        jdbc.update(REVOKE_DEVICE, schemaName, deviceId);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
