package trazzo.back.incidents.infrastructure.adapters.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trazzo.back.incidents.application.port.out.EvidenceUrlResolver;
import trazzo.back.shared.application.port.out.FileStoragePort;

@Component
@RequiredArgsConstructor
public class FileStorageEvidenceUrlAdapter implements EvidenceUrlResolver {

    private final FileStoragePort fileStoragePort;

    @Override
    public String buildPublicUrl(String fileKey) {
        return fileStoragePort.buildPublicUrl(fileKey);
    }
}
