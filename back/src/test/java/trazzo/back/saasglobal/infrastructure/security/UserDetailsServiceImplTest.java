package trazzo.back.saasglobal.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock UserRepositoryPort userRepository;
    @InjectMocks UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_returnsUserDetails_whenUserExists() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.restore("id-1", 1, null, "admin@test.com", null,
                "hashed_pass", List.of("ADMIN"), now, now, null);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@test.com");

        assertThat(details.getUsername()).isEqualTo("admin@test.com");
        assertThat(details.getPassword()).isEqualTo("hashed_pass");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@test.com");
    }

    @Test
    void loadUserByUsername_setsDisabledWhenUserDeleted() {
        LocalDateTime now = LocalDateTime.now();
        User deletedUser = User.restore("id-2", 1, null, "deleted@test.com", null,
                "pass", List.of(), now, now, now);
        when(userRepository.findByEmail("deleted@test.com")).thenReturn(Optional.of(deletedUser));

        UserDetails details = service.loadUserByUsername("deleted@test.com");

        assertThat(details.isEnabled()).isFalse();
    }
}
