package trazzo.back.shared.infrastructure.adapters.out.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.domain.exception.StorageException;

import java.io.InputStream;
import java.time.Duration;

@Component
@ConditionalOnExpression("'${cloudflare.r2.endpoint:}'.isEmpty()")
public class LocalStorageStub implements FileStoragePort {

    @Override
    public String generatePresignedPutUrl(String objectKey, String contentType, Duration duration) {
        throw new StorageException("Cloudflare R2 is not configured. Set CLOUDFLARE_R2_ENDPOINT to enable file storage.");
    }

    @Override
    public void uploadFile(String objectKey, InputStream data, long contentLength, String contentType) {
        throw new StorageException("Cloudflare R2 is not configured. Set CLOUDFLARE_R2_ENDPOINT to enable file storage.");
    }

    @Override
    public String buildPublicUrl(String objectKey) {
        throw new StorageException("Cloudflare R2 is not configured. Set CLOUDFLARE_R2_ENDPOINT to enable file storage.");
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        throw new StorageException("Cloudflare R2 is not configured. Set CLOUDFLARE_R2_ENDPOINT to enable file storage.");
    }
}
