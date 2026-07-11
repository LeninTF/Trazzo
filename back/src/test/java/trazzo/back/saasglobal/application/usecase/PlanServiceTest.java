package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.out.FeatureRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanFeatureRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.PlanFeature;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock PlanRepositoryPort planRepository;
    @Mock PlanFeatureRepositoryPort planFeatureRepository;
    @Mock FeatureRepositoryPort featureRepository;
    @InjectMocks PlanService service;

    private static Plan plan(int id) {
        var now = LocalDateTime.now();
        return Plan.restore(id, "Basic", BigDecimal.valueOf(99), BigDecimal.valueOf(999), "SOLES", "MONTHLY",
                true, now, now, null);
    }

    private static Feature feature(int id, String name) {
        var now = LocalDateTime.now();
        return Feature.restore(id, name, "desc", now, now);
    }

    @Test
    void getById_returnsResultWhenFound() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());

        PlanResult result = service.getById(1);

        assertEquals(1, result.id());
        assertEquals("Basic", result.name());
        assertEquals(BigDecimal.valueOf(999), result.priceAnnual());
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
        when(planFeatureRepository.findByPlanId(any())).thenReturn(List.of());

        List<PlanResult> results = service.listAll();

        assertEquals(2, results.size());
    }

    @Test
    void listActive_returnsMappedResults() {
        when(planRepository.findAllActive()).thenReturn(List.of(plan(1)));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());

        List<PlanResult> results = service.listActive();

        assertEquals(1, results.size());
    }

    @Test
    void create_savesAndReturnsResult() {
        when(planRepository.save(any())).thenReturn(plan(1));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());
        var command = new CreatePlanCommand("Basic", BigDecimal.valueOf(99), BigDecimal.valueOf(999),
                "SOLES", "MONTHLY", null);

        PlanResult result = service.create(command);

        assertEquals(1, result.id());
        assertEquals("Basic", result.name());
        assertTrue(result.active());
        verify(planFeatureRepository).deleteByPlanId(1);
    }

    @Test
    void create_withFeatures_savesBooleanTrueAndNumericFeatures() {
        when(planRepository.save(any())).thenReturn(plan(1));
        when(featureRepository.findAll()).thenReturn(List.of(
                feature(10, "max_trabajadores"), feature(11, "reportes"), feature(12, "soporte-24-7")));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());
        var features = Map.<String, Object>of(
                "max_trabajadores", 250,
                "reportes", true,
                "soporte-24-7", false);
        var command = new CreatePlanCommand("Basic", BigDecimal.valueOf(99), BigDecimal.valueOf(999),
                "SOLES", "MONTHLY", features);

        service.create(command);

        verify(planFeatureRepository).save(argThat(pf ->
                pf.getFeatureId() == 10 && "INT".equals(pf.getDataType()) && "250".equals(pf.getValue())));
        verify(planFeatureRepository).save(argThat(pf ->
                pf.getFeatureId() == 11 && "BOOLEAN".equals(pf.getDataType()) && "true".equals(pf.getValue())));
        verify(planFeatureRepository, never()).save(argThat(pf -> pf.getFeatureId() == 12));
    }

    @Test
    void getById_mapsPersistedFeaturesBackToTypedValues() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));
        when(featureRepository.findAll()).thenReturn(List.of(
                feature(10, "max_trabajadores"), feature(11, "reportes")));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of(
                PlanFeature.restore(1, 1, 10, "INT", "250", LocalDate.now(), null, true, LocalDateTime.now(), LocalDateTime.now()),
                PlanFeature.restore(2, 1, 11, "BOOLEAN", "true", LocalDate.now(), null, true, LocalDateTime.now(), LocalDateTime.now())));

        PlanResult result = service.getById(1);

        assertEquals(250L, result.features().get("max_trabajadores"));
        assertEquals(true, result.features().get("reportes"));
    }

    @Test
    void update_savesAndReturnsUpdated() {
        when(planRepository.findById(1)).thenReturn(Optional.of(plan(1)));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());
        var command = new UpdatePlanCommand(1, "Pro", BigDecimal.valueOf(199), BigDecimal.valueOf(1999),
                "DOLAR", "ANNUAL", null);

        PlanResult result = service.update(command);

        assertEquals("Pro", result.name());
        assertEquals(BigDecimal.valueOf(1999), result.priceAnnual());
        verify(planFeatureRepository).deleteByPlanId(1);
    }

    @Test
    void update_throwsWhenNotFound() {
        when(planRepository.findById(99)).thenReturn(Optional.empty());
        var command = new UpdatePlanCommand(99, "Pro", BigDecimal.ONE, BigDecimal.ONE, "SOLES", "MONTHLY", null);

        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }

    @Test
    void activate_savesAndReturnsActivated() {
        var now = LocalDateTime.now();
        var inactive = Plan.restore(1, "Basic", BigDecimal.valueOf(99), BigDecimal.valueOf(999), "SOLES", "MONTHLY",
                false, now, now, null);
        when(planRepository.findById(1)).thenReturn(Optional.of(inactive));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());

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
        when(planFeatureRepository.findByPlanId(1)).thenReturn(List.of());

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
