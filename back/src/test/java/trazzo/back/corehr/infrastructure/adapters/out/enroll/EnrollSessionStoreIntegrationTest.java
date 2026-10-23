package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import trazzo.back.saasglobal.infrastructure.adapters.out.provisioning.TenantFlywayMigrator;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** El store contra las migraciones reales de tenant en PostgreSQL 15. Requiere Docker. */
@Testcontainers
class EnrollSessionStoreIntegrationTest {

    private static final String SCHEMA = "tenant_enroll";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private JdbcTemplate jdbc;
    private long userA;
    private long userB;
    private long deviceA;
    private long deviceB;

    @BeforeAll
    static void migrateTenantSchema() throws Exception {
        var admin = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new JdbcTemplate(admin).execute("CREATE SCHEMA " + SCHEMA);
        try (Connection conn = admin.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + SCHEMA + ", public");
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/tenant/V1__tenant_db.sql"));
        }
        new TenantFlywayMigrator(admin).migrate(SCHEMA);
    }

    // Cada store usa su propio DataSource, como otra instancia del backend o el backend tras reiniciar.
    private static JdbcTemplate tenantJdbc() {
        String url = POSTGRES.getJdbcUrl();
        url += (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA;
        return new JdbcTemplate(new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private long newTenantUser() {
        return jdbc.queryForObject("INSERT INTO tenant_user (master_user_id) VALUES (gen_random_uuid()) RETURNING id", Long.class);
    }

    private long newDevice() {
        return jdbc.queryForObject("INSERT INTO device (code) VALUES (?) RETURNING id", Long.class, "DVC-" + UUID.randomUUID());
    }

    private String deviceCode(long deviceId) {
        return jdbc.queryForObject("SELECT code FROM device WHERE id = ?", String.class, deviceId);
    }

    private EnrollSession session(String token, long user, long device, LocalDateTime expiresAt) {
        return new EnrollSession(token, user, device, 3, deviceCode(device), expiresAt);
    }

    @BeforeEach
    void setUp() {
        jdbc = tenantJdbc();
        jdbc.update("DELETE FROM enroll_session");
        userA = newTenantUser();
        userB = newTenantUser();
        deviceA = newDevice();
        deviceB = newDevice();
    }

    @Test
    void aSessionSurvivesANewStoreInstanceAndIsConsumedOnlyOnce() {
        var expires = LocalDateTime.now().plusMinutes(2).withNano(0);
        new EnrollSessionStore(tenantJdbc()).createSession(session("token-1", userA, deviceA, expires));

        var afterRestart = new EnrollSessionStore(tenantJdbc());
        assertThat(afterRestart.findPendingByDeviceCode(deviceCode(deviceA))).isPresent();
        var consumed = afterRestart.findAndConsume("token-1");

        assertThat(consumed).isEqualTo(session("token-1", userA, deviceA, expires));
        assertThat(new EnrollSessionStore(tenantJdbc()).findAndConsume("token-1")).isNull();
        assertThat(afterRestart.findPendingByDeviceCode(deviceCode(deviceA))).isEmpty();
    }

    @Test
    void concurrentConsumersGetTheSessionExactlyOnce() throws Exception {
        new EnrollSessionStore(jdbc).createSession(session("token-2", userA, deviceA, LocalDateTime.now().plusMinutes(2)));
        int consumers = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(consumers);
        try {
            List<Future<EnrollSession>> results = new ArrayList<>();
            for (int i = 0; i < consumers; i++) {
                var store = new EnrollSessionStore(tenantJdbc());
                results.add(pool.submit(() -> {
                    start.await();
                    return store.findAndConsume("token-2");
                }));
            }
            start.countDown();

            int winners = 0;
            for (Future<EnrollSession> result : results) {
                if (result.get() != null) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void anExpiredSessionIsNeitherPendingNorActiveNorConsumable() {
        var store = new EnrollSessionStore(jdbc);
        store.createSession(session("expired", userA, deviceA, LocalDateTime.now().minusSeconds(1)));

        assertThat(store.findPendingByDeviceCode(deviceCode(deviceA))).isEmpty();
        assertThat(store.existsActiveSession(userA, deviceA)).isFalse();
        assertThat(store.findAndConsume("expired")).isNull();
    }

    @Test
    void anActiveSessionBelongsToItsUserAndDevice() {
        var store = new EnrollSessionStore(jdbc);
        store.createSession(session("token-3", userA, deviceA, LocalDateTime.now().plusMinutes(2)));

        assertThat(store.existsActiveSession(userA, deviceA)).isTrue();
        assertThat(store.existsActiveSession(userA, deviceB)).isFalse();
        assertThat(store.existsActiveSession(userB, deviceA)).isFalse();
        assertThat(store.findPendingByDeviceCode(deviceCode(deviceB))).isEmpty();
    }

    @Test
    void anUnknownTokenConsumesNothing() {
        var store = new EnrollSessionStore(jdbc);
        store.createSession(session("token-4", userA, deviceA, LocalDateTime.now().plusMinutes(2)));

        assertThat(store.findAndConsume("otro")).isNull();
        assertThat(store.existsActiveSession(userA, deviceA)).isTrue();
    }

    @Test
    void creatingASessionPurgesTheExpiredOnes() {
        var store = new EnrollSessionStore(jdbc);
        store.createSession(session("old", userA, deviceA, LocalDateTime.now().minusMinutes(5)));

        store.createSession(session("new", userB, deviceB, LocalDateTime.now().plusMinutes(2)));

        assertThat(jdbc.queryForList("SELECT enroll_token FROM enroll_session", String.class)).containsExactly("new");
    }

    @Test
    void deletingTheDeviceDeletesItsSessions() {
        new EnrollSessionStore(jdbc).createSession(session("token-5", userA, deviceA, LocalDateTime.now().plusMinutes(2)));

        jdbc.update("DELETE FROM device WHERE id = ?", deviceA);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM enroll_session", Integer.class)).isZero();
    }
}
