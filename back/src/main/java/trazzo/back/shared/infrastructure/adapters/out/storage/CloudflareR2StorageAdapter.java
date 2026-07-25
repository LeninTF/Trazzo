package trazzo.back.shared.infrastructure.adapters.out.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.domain.exception.StorageException;
import trazzo.back.shared.infrastructure.config.CloudflareR2Properties;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

@Component
@ConditionalOnExpression("'${cloudflare.r2.endpoint:}'.length() > 0")
public class CloudflareR2StorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CloudflareR2Properties properties;

    public CloudflareR2StorageAdapter(S3Client s3Client, S3Presigner s3Presigner, CloudflareR2Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public String generatePresignedPutUrl(String objectKey, String contentType, Duration duration) {
        try {
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(builder ->
                    builder.signatureDuration(duration)
                            .putObjectRequest(req ->
                                    req.bucket(properties.getBucketName())
                                            .key(objectKey)
                                            .contentType(contentType)));

            URL url = presignedRequest.url();
            return url.toString();
        } catch (S3Exception e) {
            throw new StorageException("Failed to generate presigned PUT URL for key: " + objectKey, e);
        }
    }

    @Override
    public void uploadFile(String objectKey, InputStream data, long contentLength, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(data, contentLength));
        } catch (S3Exception e) {
            throw new StorageException("Failed to upload file with key: " + objectKey, e);
        }
    }

    @Override
    public String buildPublicUrl(String objectKey) {
        String publicUrl = properties.getPublicUrl();
        if (publicUrl == null || publicUrl.isBlank()) {
            throw new StorageException("cloudflare.r2.public-url is not configured");
        }
        if (publicUrl.endsWith("/")) {
            return publicUrl + objectKey;
        }
        return publicUrl + "/" + objectKey;
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();
            return s3Client.getObject(getRequest);
        } catch (S3Exception e) {
            throw new StorageException("Failed to download file with key: " + objectKey, e);
        }
    }
}
