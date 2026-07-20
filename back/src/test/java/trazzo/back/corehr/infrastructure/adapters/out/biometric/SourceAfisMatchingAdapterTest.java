package trazzo.back.corehr.infrastructure.adapters.out.biometric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceAfisMatchingAdapterTest {

    @Mock
    CryptoKeyProviderPort cryptoKeyProvider;

    @InjectMocks
    SourceAfisMatchingAdapter adapter;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static UserBiometria anEnrolledTemplate() {
        return UserBiometria.restore(
                1L, 100L, 1L, "DVC-001", 1,
                "dGVtcA==", "a2V5", "aXY=", "dGFn",
                NOW, true, NOW, NOW
        );
    }

    @Test
    void identify_withEmptyEnrolledTemplates_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();

        var result = adapter.identify(probe, List.of(), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withGarbageProbeTemplate_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();

        var result = adapter.identify(probe, List.of(anEnrolledTemplate()), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withAllDecryptionFailures_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();

        var result = adapter.identify(probe, List.of(anEnrolledTemplate()), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withMultipleEnrolledTemplatesAndGarbageProbe_shouldReturnEmpty() {
        var template1 = UserBiometria.restore(
                1L, 100L, 1L, "DVC-001", 1,
                "dGVtcA==", "a2V5", "aXY=", "dGFn",
                NOW, true, NOW, NOW
        );
        var template2 = UserBiometria.restore(
                2L, 200L, 1L, "DVC-001", 2,
                "dGVtcA==", "a2V5", "aXY=", "dGFn",
                NOW, true, NOW, NOW
        );
        byte[] probe = "garbage-probe-data".getBytes();

        var result = adapter.identify(probe, List.of(template1, template2), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withEmptyByteArrayProbe_shouldReturnEmpty() {
        var result = adapter.identify(new byte[0], List.of(), 40);

        assertThat(result).isEmpty();
    }

    @Test
    void identify_withNullEnrolledTemplates_shouldReturnEmpty() {
        byte[] probe = "garbage-probe-data".getBytes();

        var result = adapter.identify(probe, List.of(), 40);

        assertThat(result).isEmpty();
    }
}
