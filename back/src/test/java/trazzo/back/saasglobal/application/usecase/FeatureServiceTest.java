package trazzo.back.saasglobal.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreateFeatureCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateFeatureCommand;
import trazzo.back.saasglobal.application.port.out.FeatureRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private FeatureRepositoryPort featureRepository;

    @InjectMocks
    private FeatureService service;

    private Feature sampleFeature() {
        return Feature.restore(1, "Reports", "Generate reports", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreateFeatureCommand("Reports", "Generate reports");
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> {
            Feature f = i.getArgument(0);
            return Feature.restore(1, f.getName(), f.getDescription(),
                    f.getCreatedAt(), f.getUpdatedAt());
        });

        var result = service.create(cmd);

        assertThat(result.name()).isEqualTo("Reports");
        verify(featureRepository).save(any(Feature.class));
    }

    @Test
    void getById_shouldReturnResult() {
        when(featureRepository.findById(1)).thenReturn(Optional.of(sampleFeature()));

        var result = service.getById(1);

        assertThat(result.id()).isEqualTo(1);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(featureRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Feature not found");
    }

    @Test
    void listAll_shouldReturnList() {
        when(featureRepository.findAll()).thenReturn(List.of(sampleFeature()));

        var result = service.listAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldReturnResult() {
        var cmd = new UpdateFeatureCommand(1, "New Reports", "Updated desc");
        when(featureRepository.findById(1)).thenReturn(Optional.of(sampleFeature()));
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.update(cmd);

        assertThat(result.name()).isEqualTo("New Reports");
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        var cmd = new UpdateFeatureCommand(999, "Name", "Desc");
        when(featureRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteById_shouldDelete() {
        when(featureRepository.findById(1)).thenReturn(Optional.of(sampleFeature()));

        service.deleteById(1);

        verify(featureRepository).deleteById(1);
    }

    @Test
    void deleteById_shouldThrowWhenNotFound() {
        when(featureRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(999))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
