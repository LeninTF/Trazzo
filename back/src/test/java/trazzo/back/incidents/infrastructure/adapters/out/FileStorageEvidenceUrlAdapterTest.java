package trazzo.back.incidents.infrastructure.adapters.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.shared.application.port.out.FileStoragePort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageEvidenceUrlAdapterTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private FileStorageEvidenceUrlAdapter adapter;

    @Test
    void buildPublicUrl_delegatesToFileStorage() {
        when(fileStoragePort.buildPublicUrl("evidence/123.png")).thenReturn("https://cdn.example.com/evidence/123.png");

        var result = adapter.buildPublicUrl("evidence/123.png");

        assertThat(result).isEqualTo("https://cdn.example.com/evidence/123.png");
    }

    @Test
    void buildPublicUrl_returnsNullWhenStorageReturnsNull() {
        when(fileStoragePort.buildPublicUrl("missing")).thenReturn(null);

        var result = adapter.buildPublicUrl("missing");

        assertThat(result).isNull();
    }
}
