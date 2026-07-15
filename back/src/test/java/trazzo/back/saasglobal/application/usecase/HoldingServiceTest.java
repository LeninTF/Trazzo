package trazzo.back.saasglobal.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreateHoldingCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateHoldingCommand;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private HoldingRepositoryPort holdingRepository;

    @InjectMocks
    private HoldingService service;

    private Holding sampleHolding() {
        return Holding.restore(1, "TAX001", "Test Holding", HoldingType.PRIVADO,
                true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreateHoldingCommand("TAX001", "Test Holding", "PRIVADO");
        when(holdingRepository.existsByTaxId("TAX001")).thenReturn(false);
        when(holdingRepository.save(any(Holding.class))).thenAnswer(i -> {
            Holding h = i.getArgument(0);
            return Holding.restore(1, h.getTaxId(), h.getLegalName(), h.getType(),
                    h.isActive(), h.getCreatedAt(), h.getUpdatedAt(), null);
        });

        var result = service.create(cmd);

        assertThat(result.taxId()).isEqualTo("TAX001");
        assertThat(result.legalName()).isEqualTo("Test Holding");
        verify(holdingRepository).save(any(Holding.class));
    }

    @Test
    void create_shouldThrowWhenTaxIdExists() {
        var cmd = new CreateHoldingCommand("TAX001", "Test", "PRIVADO");
        when(holdingRepository.existsByTaxId("TAX001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tax ID already registered");
    }

    @Test
    void getById_shouldReturnResult() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(sampleHolding()));

        var result = service.getById(1);

        assertThat(result.id()).isEqualTo(1);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(holdingRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Holding not found");
    }

    @Test
    void listAll_shouldReturnList() {
        when(holdingRepository.findAll()).thenReturn(List.of(sampleHolding()));

        var result = service.listAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldReturnResult() {
        var cmd = new UpdateHoldingCommand(1, "Updated Name", "PUBLICO");
        when(holdingRepository.findById(1)).thenReturn(Optional.of(sampleHolding()));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.update(cmd);

        assertThat(result.legalName()).isEqualTo("Updated Name");
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        var cmd = new UpdateHoldingCommand(999, "Name", "PRIVADO");
        when(holdingRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activate_shouldSetActive() {
        var holding = Holding.restore(1, "TAX001", "H", HoldingType.PRIVADO,
                false, LocalDateTime.now(), LocalDateTime.now(), null);
        when(holdingRepository.findById(1)).thenReturn(Optional.of(holding));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.activate(1);

        assertThat(result.active()).isTrue();
    }

    @Test
    void deactivate_shouldSetInactive() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(sampleHolding()));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.deactivate(1);

        assertThat(result.active()).isFalse();
    }

    @Test
    void deleteById_shouldMarkDeleted() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(sampleHolding()));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(i -> i.getArgument(0));

        service.deleteById(1);

        verify(holdingRepository).save(any(Holding.class));
    }
}
