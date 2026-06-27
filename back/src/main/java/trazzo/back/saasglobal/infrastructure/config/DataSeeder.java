package trazzo.back.saasglobal.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@trazzo.pe";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.info("Admin user already exists, skipping seed");
            return;
        }

        log.info("Creating admin user...");

        Integer personId = insertPerson();
        String encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD);

        User admin = User.create(
                personId,
                null,
                ADMIN_EMAIL,
                null,
                encodedPassword
        );

        userRepository.save(admin);
        ensureRoleExists();
        assignRoleToUser(admin.getId());

        log.info("Admin user created successfully: {}", ADMIN_EMAIL);
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
        log.info("Role {} assigned to user {}", ADMIN_ROLE, ADMIN_EMAIL);
    }
}
