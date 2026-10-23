package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;
import trazzo.back.saasglobal.infrastructure.adapters.out.provisioning.TenantFlywayMigrator;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La entidad contra el schema real de tenant en PostgreSQL 15: H2 acepta tipos que PostgreSQL rechaza
 * (attendances.id es UUID; state era un enum de PostgreSQL). Requiere Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AttendancePostgresPersistenceTest {

    private static final String SCHEMA = "tenant_attendance";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void tenantDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> {
            String url = POSTGRES.getJdbcUrl();
            return url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA;
        });
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

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

    @Autowired
    private AttendanceJpaRepository attendances;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anAttendanceIsStoredAndFoundWithTheTenantSchemaTypes() {
        Long tenantUserId = jdbc.queryForObject(
                "INSERT INTO tenant_user (master_user_id) VALUES (gen_random_uuid()) RETURNING id", Long.class);
        var id = UUID.randomUUID();
        var entity = new AttendanceEntity();
        entity.setId(id);
        entity.setTenantUserId(tenantUserId);
        entity.setCheckIn(LocalDateTime.of(2026, 10, 23, 7, 55));
        entity.setAttendanceDate(LocalDate.of(2026, 10, 23));
        entity.setMinutesLate(0);
        entity.setState(AttendanceState.PUNTUAL);
        entity.setOfflineEventId(55);
        entity.setDeviceCode("ZK-001");

        attendances.saveAndFlush(entity);

        var found = attendances.findById(id).orElseThrow();
        assertThat(found.getState()).isEqualTo(AttendanceState.PUNTUAL);
        assertThat(attendances.findByTenantUserIdAndAttendanceDate(tenantUserId, LocalDate.of(2026, 10, 23))).isPresent();
        assertThat(attendances.existsByOfflineEventIdAndDeviceCode(55, "ZK-001")).isTrue();
    }
}
