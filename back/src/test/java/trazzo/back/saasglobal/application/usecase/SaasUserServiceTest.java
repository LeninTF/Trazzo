package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import trazzo.back.saasglobal.application.dto.command.AssignSaasUserRolesCommand;
import trazzo.back.saasglobal.application.dto.command.CreateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SaasUserResult;
import trazzo.back.saasglobal.application.port.out.EmailService;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.RoleMasterRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRolesMasterRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.DocumentType;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;
import trazzo.back.saasglobal.domain.model.iam.User;

@ExtendWith(MockitoExtension.class)
class SaasUserServiceTest {

    @Mock UserRepositoryPort userRepository;
    @Mock PersonRepositoryPort personRepository;
    @Mock UserRolesMasterRepositoryPort userRolesRepository;
    @Mock RoleMasterRepositoryPort roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @InjectMocks SaasUserService service;

    private static Person person(int id) {
        return Person.restore(id, null, DocumentType.DNI, "00000000", "Ana", "Perez", "Lopez",
                null, LocalDateTime.now(), LocalDateTime.now());
    }

    private static User user(String id) {
        var now = LocalDateTime.now();
        return User.restore(id, 1, null, "ana@example.com", null, "encoded",
                List.of("soporte"), List.of("monitoreo-sistema.dashboard-global"), false, now, now, null);
    }

    @Test
    void create_withExplicitPassword_doesNotSendEmail() {
        when(personRepository.save(any())).thenReturn(person(1));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user("user-1"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1")));
        when(roleRepository.findAll()).thenReturn(List.of());

        var command = new CreateSaasUserCommand("DNI", "00000000", "Ana", "Perez", "Lopez",
                "ana@example.com", null, "MyPassword1", List.of(2));

        SaasUserResult result = service.create(command);

        assertEquals("ana@example.com", result.email());
        verify(userRolesRepository).replaceForUser("user-1", List.of(2));
        verifyNoInteractions(emailService);
    }

    @Test
    void create_withoutPassword_generatesTempPasswordAndSendsEmail() {
        when(personRepository.save(any())).thenReturn(person(1));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user("user-1"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1")));
        when(roleRepository.findAll()).thenReturn(List.of());

        var command = new CreateSaasUserCommand("DNI", "00000000", "Ana", "Perez", "Lopez",
                "ana@example.com", null, null, List.of());

        service.create(command);

        verify(emailService).send(eq("ana@example.com"), anyString(), anyString());
    }

    @Test
    void getById_hydratesPersonAndRoleTags() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1")));
        when(personRepository.findById(1)).thenReturn(Optional.of(person(1)));
        when(roleRepository.findAll()).thenReturn(List.of(
                RoleMaster.restore(2, "soporte", "Soporte", "desc", List.of("monitoreo-sistema.dashboard-global"))));

        SaasUserResult result = service.getById("user-1");

        assertEquals("Ana", result.person().name());
        assertEquals(1, result.roles().size());
        assertEquals("soporte", result.roles().get(0).name());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById("missing"));
    }

    @Test
    void update_updatesContactFields() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1")));
        when(userRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));
        when(personRepository.findById(1)).thenReturn(Optional.of(person(1)));
        when(roleRepository.findAll()).thenReturn(List.of());

        var command = new UpdateSaasUserCommand("user-1", "new@example.com", "999888777");
        service.update(command);

        verify(userRepository).update(argThat(u -> u.getEmail().equals("new@example.com")));
    }

    @Test
    void deleteById_softDeletesUser() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1")));
        when(userRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteById("user-1");

        verify(userRepository).update(argThat(u -> !u.isActive()));
    }

    @Test
    void assignRoles_replacesRoles() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1")));
        when(personRepository.findById(1)).thenReturn(Optional.of(person(1)));
        when(roleRepository.findAll()).thenReturn(List.of());

        var command = new AssignSaasUserRolesCommand("user-1", List.of(3, 4));
        service.assignRoles(command);

        verify(userRolesRepository).replaceForUser("user-1", List.of(3, 4));
    }

    @Test
    void listAll_returnsPaginatedResults() {
        when(userRepository.findAll(null, 0, 20)).thenReturn(List.of(user("user-1")));
        when(userRepository.countAll(null)).thenReturn(1L);
        when(personRepository.findById(1)).thenReturn(Optional.of(person(1)));
        when(roleRepository.findAll()).thenReturn(List.of());

        PaginatedResult<SaasUserResult> result = service.listAll(null, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
    }
}
