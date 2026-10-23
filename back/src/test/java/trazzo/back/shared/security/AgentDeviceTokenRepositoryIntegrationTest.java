package trazzo.back.shared.security;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tokens de dispositivo sobre las migraciones reales del master en PostgreSQL 15. Requiere Docker. */
@Testcontainers
class AgentDeviceTokenRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static DriverManagerDataSource dataSource;

    private JdbcTemplate jdbc;
    private AgentDeviceTokenRepository repository;
    private UUID tenantId;
    private String schema;

    @BeforeAll
    static void migrateMaster() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/master").load().migrate();
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        repository = new AgentDeviceTokenRepository(dataSource);
        schema = "tenant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        tenantId = jdbc.queryForObject("INSERT INTO tenants (sub_domain) VALUES (?) RETURNING id", UUID.class, schema);
        jdbc.update("INSERT INTO tenant_settings (tenant_id, schema_name) VALUES (?, ?)", tenantId, schema);
    }

    private static String newToken() {
        return "trzd_" + UUID.randomUUID().toString().replace("-", "") + "0123456789a";
    }

    private static String sha256(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void theTokenIsStoredOnlyAsItsSha256() throws Exception {
        String token = newToken();

        var issued = repository.replace(schema, 7L, "DVC-007", token);

        assertThat(issued.tenantId()).isEqualTo(tenantId);
        assertThat(issued.createdAt()).isNotNull();
        assertThat(jdbc.queryForList("SELECT token_hash FROM agent_device_tokens WHERE schema_name = ?", String.class, schema))
                .containsExactly(sha256(token));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agent_device_tokens WHERE token_hash = ?", Integer.class, token))
                .isZero();
    }

    @Test
    void anActiveTokenResolvesToItsTenantAndDevice() {
        String token = newToken();
        repository.replace(schema, 7L, "DVC-007", token);

        assertThat(repository.findActive(token)).contains(new DeviceAgentPrincipal(tenantId, schema, 7L, "DVC-007"));
        assertThat(repository.findActive(newToken())).isEmpty();
    }

    @Test
    void issuingAgainRevokesThePreviousTokenOfThatDevice() {
        String first = newToken();
        String second = newToken();
        repository.replace(schema, 7L, "DVC-007", first);

        repository.replace(schema, 7L, "DVC-007", second);

        assertThat(repository.findActive(first)).isEmpty();
        assertThat(repository.findActive(second)).isPresent();
    }

    @Test
    void anotherDevicesTokenIsNotRevoked() {
        String deviceSeven = newToken();
        repository.replace(schema, 7L, "DVC-007", deviceSeven);

        repository.replace(schema, 8L, "DVC-008", newToken());

        assertThat(repository.findActive(deviceSeven)).isPresent();
    }

    @Test
    void revokeAllLeavesTheDeviceWithoutAValidToken() {
        String token = newToken();
        repository.replace(schema, 7L, "DVC-007", token);

        repository.revokeAll(schema, 7L);

        assertThat(repository.findActive(token)).isEmpty();
    }

    @Test
    void aSuspendedOrDeletedTenantsTokenNoLongerWorks() {
        String token = newToken();
        repository.replace(schema, 7L, "DVC-007", token);

        jdbc.update("UPDATE tenants SET suspended_at = now() WHERE id = ?", tenantId);
        assertThat(repository.findActive(token)).isEmpty();

        jdbc.update("UPDATE tenants SET suspended_at = NULL, deleted_at = now() WHERE id = ?", tenantId);
        assertThat(repository.findActive(token)).isEmpty();
    }

    @Test
    void anUnknownSchemaKeepsThePreviousTokenAndFails() {
        String token = newToken();
        repository.replace(schema, 7L, "DVC-007", token);
        jdbc.update("DELETE FROM tenant_settings WHERE tenant_id = ?", tenantId);

        assertThatThrownBy(() -> repository.replace(schema, 7L, "DVC-007", newToken()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM agent_device_tokens WHERE schema_name = ? AND revoked_at IS NULL", Integer.class, schema))
                .isEqualTo(1);
    }

    @Test
    void theDatabaseAllowsOnlyOneActiveTokenPerDevice() {
        repository.replace(schema, 7L, "DVC-007", newToken());

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO agent_device_tokens (token_hash, tenant_id, schema_name, device_id, device_code) VALUES (?, ?, ?, 7, 'DVC-007')",
                "f".repeat(64), tenantId, schema))
                .hasMessageContaining("ux_agent_device_tokens_active");
    }
}
