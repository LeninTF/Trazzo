package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El adaptador de usuarios sobre las migraciones reales del master en PostgreSQL 15. Requiere Docker. */
@Testcontainers
class UserJdbcRepositoryAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static DriverManagerDataSource dataSource;

    @BeforeAll
    static void migrateMaster() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/master").load().migrate();
    }

    @Test
    void updatePasswordReplacesTheHashAndClearsMustChangePassword() {
        var jdbc = new JdbcTemplate(dataSource);
        String id = jdbc.queryForObject("""
                INSERT INTO users (email, password, must_change_password) VALUES (?, 'hash-temporal', true) RETURNING id
                """, String.class, "pwd-" + UUID.randomUUID() + "@trazzo.pe");
        var adapter = new UserJdbcRepositoryAdapter(dataSource);

        adapter.updatePassword(id, "hash-nuevo");

        var stored = adapter.findById(id).orElseThrow();
        assertThat(stored.getPassword()).isEqualTo("hash-nuevo");
        assertThat(stored.isMustChangePassword()).isFalse();
    }
}
