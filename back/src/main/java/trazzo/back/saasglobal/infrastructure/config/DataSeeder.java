package trazzo.back.saasglobal.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

@Component
// @Profile("local")
// TODO: This profile restriction has been removed temporarily so the seeder runs in production.
//       Disable it (re-add @Profile("local") or set trazzo.seed.admin.enabled=false) before the next push.
@ConditionalOnProperty(name = "trazzo.seed.admin.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_ROLE = "admin_trazzo";

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc,
            @Value("${trazzo.seed.admin.email}") String adminEmail,
            @Value("${trazzo.seed.admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.adminEmail = requireNonBlank(adminEmail, "trazzo.seed.admin.email");
        this.adminPassword = requireNonBlank(adminPassword, "trazzo.seed.admin.password");
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists, skipping seed");
            return;
        }

        log.info("Creating admin user...");

        Integer personId = insertPerson();
        String encodedPassword = passwordEncoder.encode(adminPassword);

        User admin = User.create(
                personId,
                null,
                adminEmail,
                null,
                encodedPassword
        );

        userRepository.save(admin);
        ensureRoleExists();
        assignRoleToUser(admin.getId());

        log.info("Admin user created successfully: {}", adminEmail);
    }

    private static String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Property '" + propertyName + "' must be set when seeding is enabled"
            );
        }
        return value.trim();
    }

    @SuppressWarnings("java:S5145")
    private Integer insertPerson() {
        jdbc.update("""
                INSERT INTO persons (document_type, document_value, name, father_surname, mother_surname)
                VALUES ('DNI', '00000000', 'Admin', 'Trazzo', 'Admin')
                """);
        return jdbc.queryForObject("SELECT LASTVAL()", Integer.class);
    }

    private void ensureRoleExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM roles_master WHERE name = ?",
                Integer.class,
                ADMIN_ROLE
        );
        if (count == null || count == 0) {
            jdbc.update(
                    "INSERT INTO roles_master (name, description) VALUES (?, ?)",
                    ADMIN_ROLE,
                    "Administrator with full access"
            );
            log.info("Role {} created", ADMIN_ROLE);
        }
    }

    private void assignRoleToUser(String userId) {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles_master WHERE name = ?",
                Integer.class,
                ADMIN_ROLE
        );
        jdbc.update(
                "INSERT INTO user_roles_master (user_id, roles_master_id) VALUES (?::uuid, ?)",
                userId,
                roleId
        );
        log.info("Role {} assigned to user {}", ADMIN_ROLE, adminEmail);
    }
}
