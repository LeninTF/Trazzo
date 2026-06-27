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
import trazzo.back.saasglobal.application.dto.command.CreateFeatureCommand;
import trazzo.back.saasglobal.application.dto.result.FeatureResult;
import trazzo.back.saasglobal.application.port.out.FeatureRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock FeatureRepositoryPort featureRepository;
    @InjectMocks FeatureService service;

    private static Feature feature(int id) {
        var now = LocalDateTime.now();
        return Feature.restore(id, "Biometric", "Fingerprint auth", now, now);
    }

    @Test
    void create_savesAndReturnsResult() {
        when(featureRepository.save(any())).thenReturn(feature(1));

        FeatureResult result = service.create(new CreateFeatureCommand("Biometric", "Fingerprint auth"));

        assertEquals(1, result.id());
        assertEquals("Biometric", result.name());
    }

    @Test
    void getById_returnsResultWhenFound() {
        when(featureRepository.findById(1)).thenReturn(Optional.of(feature(1)));

        FeatureResult result = service.getById(1);

        assertEquals(1, result.id());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(featureRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(99));
    }

    @Test
    void listAll_returnsMappedResults() {
        when(featureRepository.findAll()).thenReturn(List.of(feature(1), feature(2)));

        List<FeatureResult> results = service.listAll();

        assertEquals(2, results.size());
    }

    @Test
    void deleteById_throwsWhenNotFound() {
        when(featureRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99));
        verify(featureRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_succeedsWhenFound() {
        when(featureRepository.findById(1)).thenReturn(Optional.of(feature(1)));

        assertDoesNotThrow(() -> service.deleteById(1));
        verify(featureRepository).deleteById(1);
    }
}
