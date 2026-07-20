package trazzo.back.incidents.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.incidents.application.dto.command.CreateIncidentTypeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentTypeCommand;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.domain.model.IncidentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentTypeServiceTest {

    @Mock
    private IncidentTypeRepositoryPort repository;

    @InjectMocks
    private IncidentTypeService service;

    private IncidentType sampleType() {
        return IncidentType.restore("type-1", "Retiro", "Motivo personal", true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreateIncidentTypeCommand("Retiro", "Motivo personal");
        when(repository.existsByNombre("Retiro")).thenReturn(false);
        when(repository.save(any(IncidentType.class))).thenAnswer(i -> {
            IncidentType t = i.getArgument(0);
            return IncidentType.restore("type-1", t.getNombre(), t.getDescripcion(),
                    true, t.getCreatedAt(), t.getUpdatedAt());
        });

        var result = service.create(cmd);

        assertThat(result.nombre()).isEqualTo("Retiro");
    }

    @Test
    void create_shouldThrowWhenDuplicate() {
        var cmd = new CreateIncidentTypeCommand("Retiro", "Desc");
        when(repository.existsByNombre("Retiro")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya existe");
    }

    @Test
    void findById_shouldReturnResult() {
        when(repository.findById("type-1")).thenReturn(Optional.of(sampleType()));

        var result = service.findById("type-1");

        assertThat(result).isPresent();
        assertThat(result.get().nombre()).isEqualTo("Retiro");
    }

    @Test
    void findAll_shouldReturnPaginatedResult() {
        when(repository.findAll(true, 0, 10)).thenReturn(List.of(sampleType()));
        when(repository.count(true)).thenReturn(1L);

        var result = service.findAll(true, 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void patch_shouldRename() {
        var cmd = new PatchIncidentTypeCommand("Nuevo Nombre", null, null);
        when(repository.findById("type-1")).thenReturn(Optional.of(sampleType()));
        when(repository.save(any(IncidentType.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.patch("type-1", cmd);

        assertThat(result.nombre()).isEqualTo("Nuevo Nombre");
    }

    @Test
    void patch_shouldActivate() {
        var inactiveType = IncidentType.restore("type-1", "Retiro", "Desc", false,
                LocalDateTime.now(), LocalDateTime.now());
        var cmd = new PatchIncidentTypeCommand(null, null, true);
        when(repository.findById("type-1")).thenReturn(Optional.of(inactiveType));
        when(repository.save(any(IncidentType.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.patch("type-1", cmd);

        assertThat(result.activo()).isTrue();
    }

    @Test
    void patch_shouldDeactivate() {
        var cmd = new PatchIncidentTypeCommand(null, null, false);
        when(repository.findById("type-1")).thenReturn(Optional.of(sampleType()));
        when(repository.save(any(IncidentType.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.patch("type-1", cmd);

        assertThat(result.activo()).isFalse();
    }

    @Test
    void patch_shouldThrowWhenNotFound() {
        var cmd = new PatchIncidentTypeCommand("Name", null, null);
        when(repository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patch("bad-id", cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
