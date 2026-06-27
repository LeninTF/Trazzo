package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
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
}
