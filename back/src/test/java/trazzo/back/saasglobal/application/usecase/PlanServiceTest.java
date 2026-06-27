package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock PlanRepositoryPort planRepository;
    @InjectMocks PlanService service;

    private static Plan plan(int id) {
        var now = LocalDateTime.now();
        return Plan.restore(id, "Basic", BigDecimal.valueOf(99), "SOLES", "MONTHLY",
                true, now, now, null);
    }

    @Test
    void getById_returnsResultWhenFound() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));

        PlanResult result = service.getById(1);

        assertEquals(1, result.id());
        assertEquals("Basic", result.name());
        assertTrue(result.active());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(99));
    }

    @Test
    void listAll_returnsMappedResults() {
        when(planRepository.findAll()).thenReturn(List.of(plan(1), plan(2)));

        List<PlanResult> results = service.listAll();

        assertEquals(2, results.size());
    }

    @Test
    void listActive_returnsMappedResults() {
        when(planRepository.findAllActive()).thenReturn(List.of(plan(1)));

        List<PlanResult> results = service.listActive();

        assertEquals(1, results.size());
    }

    @Test
    void create_savesAndReturnsResult() {
        when(planRepository.save(any())).thenReturn(plan(1));
        var command = new CreatePlanCommand("Basic", BigDecimal.valueOf(99), "SOLES", "MONTHLY");

        PlanResult result = service.create(command);

        assertEquals(1, result.id());
        assertEquals("Basic", result.name());
        assertTrue(result.active());
    }

    @Test
    void update_savesAndReturnsUpdated() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var command = new UpdatePlanCommand(1, "Pro", BigDecimal.valueOf(199), "DOLAR", "ANNUAL");

        PlanResult result = service.update(command);

        assertEquals("Pro", result.name());
    }

    @Test
    void update_throwsWhenNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());
        var command = new UpdatePlanCommand(99, "Pro", BigDecimal.ONE, "SOLES", "MONTHLY");

        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }

    @Test
    void activate_savesAndReturnsActivated() {
        var now = LocalDateTime.now();
        var inactive = Plan.restore(1, "Basic", BigDecimal.valueOf(99), "SOLES", "MONTHLY",
                false, now, now, null);
        when(planRepository.findById(1)).thenReturn(Optional.of(inactive));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanResult result = service.activate(1);

        assertTrue(result.active());
    }

    @Test
    void activate_throwsWhenNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.activate(99));
    }

    @Test
    void deactivate_savesAndReturnsDeactivated() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PlanResult result = service.deactivate(1);

        assertFalse(result.active());
    }

    @Test
    void deactivate_throwsWhenNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deactivate(99));
    }

    @Test
    void deleteById_savesWithDeletedState() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.deleteById(1));
        verify(planRepository).save(any());
    }

    @Test
    void deleteById_throwsWhenNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99));
    }
}
