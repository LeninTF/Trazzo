package trazzo.back.shared.application.port.out;

import java.io.InputStream;
import java.time.Duration;

public interface FileStoragePort {

    String generatePresignedPutUrl(String objectKey, String contentType, Duration duration);

    void uploadFile(String objectKey, InputStream data, long contentLength, String contentType);

    String buildPublicUrl(String objectKey);

    InputStream downloadFile(String objectKey);
}
