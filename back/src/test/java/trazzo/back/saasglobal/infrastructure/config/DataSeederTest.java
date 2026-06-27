package trazzo.back.saasglobal.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

import org.mockito.ArgumentCaptor;
import trazzo.back.saasglobal.domain.model.iam.User;
import java.util.Optional;

class DataSeederTest {

    private UserRepositoryPort userRepository;
    private PasswordEncoder passwordEncoder;
    private JdbcTemplate jdbc;
    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jdbc = mock(JdbcTemplate.class);
        seeder = new DataSeeder(userRepository, passwordEncoder, jdbc);
    }

    @Test
    void runWhenAdminExistsSkipsSeed() {
        when(userRepository.findByEmail("admin@trazzo.pe")).thenReturn(Optional.of(mock(User.class)));

        seeder.run();

        verify(userRepository, never()).save(any());
        verify(jdbc, never()).update(anyString());
    }

    @Test
    void runWhenAdminNotFoundCreatesAdminUser() {
        var userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findByEmail("admin@trazzo.pe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");
        when(jdbc.queryForObject("SELECT LASTVAL()", Integer.class)).thenReturn(1);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM roles_master WHERE name = ?", Integer.class, "ADMIN"))
                .thenReturn(0);
        when(jdbc.queryForObject("SELECT id FROM roles_master WHERE name = ?", Integer.class, "ADMIN"))
                .thenReturn(1);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.<User>getArgument(0));

        seeder.run();

        verify(passwordEncoder).encode("admin123");
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getId());
        verify(jdbc).update(contains("INSERT INTO persons"));
        verify(jdbc).update(contains("INSERT INTO roles_master"), eq("ADMIN"), eq("Administrator with full access"));
        verify(jdbc).update(contains("INSERT INTO user_roles_master"), eq(userCaptor.getValue().getId()), eq(1));
    }

    @Test
    void runWhenRoleExistsDoesNotCreateRole() {
        var userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findByEmail("admin@trazzo.pe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");
        when(jdbc.queryForObject("SELECT LASTVAL()", Integer.class)).thenReturn(1);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM roles_master WHERE name = ?", Integer.class, "ADMIN"))
                .thenReturn(1);
        when(jdbc.queryForObject("SELECT id FROM roles_master WHERE name = ?", Integer.class, "ADMIN"))
                .thenReturn(1);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.<User>getArgument(0));

        seeder.run();

        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getId());
        verify(jdbc, never()).update(contains("INSERT INTO roles_master"), any(), any());
        verify(jdbc).update(contains("INSERT INTO user_roles_master"), eq(userCaptor.getValue().getId()), eq(1));
    }
}
