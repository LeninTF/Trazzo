package trazzo.back.saasglobal.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.shared.security.TenantPermissionPort;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock UserRepositoryPort userRepository;
    @Mock TenantPermissionPort tenantPermissionPort;
    @InjectMocks UserDetailsServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DELETED_USER_ID = UUID.randomUUID();

    @Test
    void loadUserByUsername_returnsUserDetails_whenUserExists() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.restore(USER_ID.toString(), 1, null, "admin@test.com", null,
                "hashed_pass", List.of("admin_trazzo"), List.of(), false, now, now, null);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@test.com");

        assertThat(details.getUsername()).isEqualTo("admin@test.com");
        assertThat(details.getPassword()).isEqualTo("hashed_pass");
        var authorities = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertThat(authorities).containsExactlyInAnyOrder("ROLE_admin_trazzo", "ROLE_SAAS_ADMIN");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_doesNotGrantSaasAdmin_whenRoleIsNotAdminTrazzo() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.restore(USER_ID.toString(), 1, null, "user@test.com", null,
                "hashed_pass", List.of("financiero"), List.of(), false, now, now, null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@test.com");

        var authorities = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertThat(authorities).containsExactly("ROLE_financiero");
        assertThat(authorities).doesNotContain("ROLE_SAAS_ADMIN");
    }

    @Test
    void loadUserByUsername_grantsPermissionAuthorities() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.restore(USER_ID.toString(), 1, null, "financiero@test.com", null,
                "hashed_pass", List.of("financiero"),
                List.of("billing-suscripciones.gestionar-pagos", "monitoreo-sistema.dashboard-global"),
                false, now, now, null);
        when(userRepository.findByEmail("financiero@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("financiero@test.com");

        var authorities = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertThat(authorities).containsExactlyInAnyOrder(
                "ROLE_financiero",
                "billing-suscripciones.gestionar-pagos", "monitoreo-sistema.dashboard-global");
    }

    @Test
    void loadUserByUsername_doesNotGrantSaasAdmin_whenUserHasNoRoles() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.restore(USER_ID.toString(), 1, "tenant-1", "employee@test.com", null,
                "hashed_pass", List.of(), List.of(), false, now, now, null);
        when(userRepository.findByEmail("employee@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("employee@test.com");

        assertThat(details.getAuthorities()).isEmpty();
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
        User deletedUser = User.restore(DELETED_USER_ID.toString(), 1, null, "deleted@test.com", null,
                "pass", List.of(), List.of(), false, now, now, now);
        when(userRepository.findByEmail("deleted@test.com")).thenReturn(Optional.of(deletedUser));

        UserDetails details = service.loadUserByUsername("deleted@test.com");

        assertThat(details.isEnabled()).isFalse();
    }
}
