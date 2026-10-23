package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sesiones de enrolamiento en la tabla enroll_session del schema del tenant (el JdbcTemplate pasa por
 * TenantAwareDataSource): sobreviven a un reinicio y las comparten todas las instancias del backend.
 */
@Component
@RequiredArgsConstructor
public class EnrollSessionStore {

    private static final String COLUMNS = "enroll_token, tenant_user_id, device_id, finger_index, device_code, expires_at";

    private static final RowMapper<EnrollSession> SESSION = (rs, rowNum) -> new EnrollSession(
            rs.getString("enroll_token"),
            rs.getLong("tenant_user_id"),
            rs.getLong("device_id"),
            rs.getInt("finger_index"),
            rs.getString("device_code"),
            rs.getObject("expires_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public void createSession(EnrollSession session) {
        jdbcTemplate.update("DELETE FROM enroll_session WHERE expires_at <= ?", LocalDateTime.now());
        jdbcTemplate.update("INSERT INTO enroll_session (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)",
                session.enrollToken(), session.tenantUserId(), session.deviceId(), session.fingerIndex(),
                session.deviceCode(), session.expiresAt());
    }

    // Un único DELETE ... RETURNING: si dos peticiones traen el mismo token, solo una recibe la fila.
    public EnrollSession findAndConsume(String enrollToken) {
        var consumed = jdbcTemplate.query(
                "DELETE FROM enroll_session WHERE enroll_token = ? RETURNING " + COLUMNS, SESSION, enrollToken);
        if (consumed.isEmpty()) {
            return null;
        }
        var session = consumed.getFirst();
        return LocalDateTime.now().isAfter(session.expiresAt()) ? null : session;
    }

    public boolean existsActiveSession(Long tenantUserId, Long deviceId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM enroll_session WHERE tenant_user_id = ? AND device_id = ? AND expires_at > ?)",
                Boolean.class, tenantUserId, deviceId, LocalDateTime.now()));
    }

    public Optional<EnrollSession> findPendingByDeviceCode(String deviceCode) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM enroll_session WHERE device_code = ? AND expires_at > ? ORDER BY expires_at LIMIT 1",
                SESSION, deviceCode, LocalDateTime.now()).stream().findFirst();
    }
}
