package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreateHoldingCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateHoldingCommand;
import trazzo.back.saasglobal.application.dto.result.HoldingResult;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock HoldingRepositoryPort holdingRepository;
    @InjectMocks HoldingService service;

    private static Holding holding(int id) {
        var now = LocalDateTime.now();
        return Holding.restore(id, "20111111111", "Corp SA", HoldingType.PUBLICO, true, now, now, null);
    }

    @Test
    void create_savesAndReturnsResult() {
        var command = new CreateHoldingCommand("20111111111", "Corp SA", "PUBLICO");
        when(holdingRepository.existsByTaxId("20111111111")).thenReturn(false);
        when(holdingRepository.save(any())).thenReturn(holding(1));

        HoldingResult result = service.create(command);

        assertEquals(1, result.id());
        assertEquals("20111111111", result.taxId());
        assertEquals("Corp SA", result.legalName());
        assertTrue(result.active());
    }

    @Test
    void create_throwsWhenTaxIdAlreadyExists() {
        when(holdingRepository.existsByTaxId("20111111111")).thenReturn(true);
        var command = new CreateHoldingCommand("20111111111", "Corp SA", "PUBLICO");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenTypeInvalid() {
        when(holdingRepository.existsByTaxId(any())).thenReturn(false);
        var command = new CreateHoldingCommand("20111111111", "Corp SA", "INVALID");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
    }

    @Test
    void getById_returnsResultWhenFound() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(holding(1)));

        HoldingResult result = service.getById(1);

        assertEquals(1, result.id());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(holdingRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(99));
    }

    @Test
    void listAll_returnsAllMappedResults() {
        when(holdingRepository.findAll()).thenReturn(List.of(holding(1), holding(2)));

        List<HoldingResult> results = service.listAll();

        assertEquals(2, results.size());
    }

    @Test
    void activate_savesAndReturnsActivatedHolding() {
        var now = LocalDateTime.now();
        var inactive = Holding.restore(1, "20111111111", "Corp SA", HoldingType.PUBLICO,
                false, now, now, null);
        when(holdingRepository.findById(1)).thenReturn(Optional.of(inactive));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HoldingResult result = service.activate(1);

        assertTrue(result.active());
    }

    @Test
    void activate_throwsWhenNotFound() {
        when(holdingRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.activate(99));
    }

    @Test
    void deactivate_savesAndReturnsDeactivatedHolding() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(holding(1)));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HoldingResult result = service.deactivate(1);

        assertFalse(result.active());
    }

    @Test
    void deactivate_throwsWhenNotFound() {
        when(holdingRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deactivate(99));
    }

    @Test
    void update_savesAndReturnsUpdated() {
        var command = new UpdateHoldingCommand(1, "Corp Nueva SA", "PUBLICO");
        when(holdingRepository.findById(1)).thenReturn(Optional.of(holding(1)));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HoldingResult result = service.update(command);

        assertEquals("Corp Nueva SA", result.legalName());
        assertEquals("PUBLICO", result.type());
    }

    @Test
    void update_throwsWhenNotFound() {
        when(holdingRepository.findById(99)).thenReturn(Optional.empty());
        var command = new UpdateHoldingCommand(99, "Corp", "PUBLICO");

        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }

    @Test
    void update_throwsWhenTypeInvalid() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(holding(1)));
        var command = new UpdateHoldingCommand(1, "Corp", "INVALID");

        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }

    @Test
    void deleteById_setsDeletedAndSaves() {
        when(holdingRepository.findById(1)).thenReturn(Optional.of(holding(1)));
        when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.deleteById(1));
        verify(holdingRepository).save(any());
    }

    @Test
    void deleteById_throwsWhenNotFound() {
        when(holdingRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99));
    }
}
