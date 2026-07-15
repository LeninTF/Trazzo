package trazzo.back.saasglobal.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepositoryPort planRepository;

    @InjectMocks
    private PlanService service;

    private Plan samplePlan() {
        return Plan.restore(1, "Basic", BigDecimal.valueOf(99), "USD", "monthly",
                true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreatePlanCommand("Basic", BigDecimal.valueOf(99), "USD", "monthly");
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> {
            Plan p = i.getArgument(0);
            return Plan.restore(1, p.getName(), p.getPrice(), p.getCurrency(),
                    p.getBillingPeriod(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt(), null);
        });

        var result = service.create(cmd);

        assertThat(result.name()).isEqualTo("Basic");
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void getById_shouldReturnResult() {
        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan()));

        var result = service.getById(1);

        assertThat(result.id()).isEqualTo(1);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(planRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan not found");
    }

    @Test
    void listActive_shouldReturnList() {
        when(planRepository.findAllActive()).thenReturn(List.of(samplePlan()));

        var result = service.listActive();

        assertThat(result).hasSize(1);
    }

    @Test
    void listAll_shouldReturnList() {
        when(planRepository.findAll()).thenReturn(List.of(samplePlan()));

        var result = service.listAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldReturnResult() {
        var cmd = new UpdatePlanCommand(1, "Pro", BigDecimal.valueOf(199), "USD", "yearly");
        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan()));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.update(cmd);

        assertThat(result.name()).isEqualTo("Pro");
    }

    @Test
    void activate_shouldSetActive() {
        var plan = Plan.restore(1, "Basic", BigDecimal.valueOf(99), "USD", "monthly",
                false, LocalDateTime.now(), LocalDateTime.now(), null);
        when(planRepository.findById(1)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.activate(1);

        assertThat(result.active()).isTrue();
    }

    @Test
    void deactivate_shouldSetInactive() {
        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan()));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.deactivate(1);

        assertThat(result.active()).isFalse();
    }

    @Test
    void deleteById_shouldMarkDeleted() {
        when(planRepository.findById(1)).thenReturn(Optional.of(samplePlan()));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        service.deleteById(1);

        verify(planRepository).save(any(Plan.class));
    }
}
