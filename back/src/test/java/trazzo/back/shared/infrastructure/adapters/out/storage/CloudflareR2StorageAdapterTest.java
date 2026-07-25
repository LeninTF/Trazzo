package trazzo.back.shared.infrastructure.adapters.out.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.ResponseInputStream;

import trazzo.back.shared.domain.exception.StorageException;
import trazzo.back.shared.infrastructure.config.CloudflareR2Properties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class CloudflareR2StorageAdapterTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    private CloudflareR2Properties properties;
    private CloudflareR2StorageAdapter adapter;

    private static final String BUCKET = "test-bucket";
    private static final String OBJECT_KEY = "reports/2025-06/reporte.xlsx";
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Duration EXPIRATION = Duration.ofMinutes(15);

    @BeforeEach
    void setUp() {
        properties = new CloudflareR2Properties();
        properties.setEndpoint("http://localhost:9000");
        properties.setRegion("auto");
        properties.setAccessKeyId("test-key");
        properties.setSecretAccessKey("test-secret");
        properties.setBucketName(BUCKET);
        properties.setPublicUrl("http://localhost:9000/test-bucket");

        adapter = new CloudflareR2StorageAdapter(s3Client, s3Presigner, properties);
    }

    @Test
    void shouldGeneratePresignedPutUrl() throws Exception {
        var presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://r2.example.com/presigned-url"));
        when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(presignedRequest);

        String url = adapter.generatePresignedPutUrl(OBJECT_KEY, CONTENT_TYPE, EXPIRATION);

        assertEquals("https://r2.example.com/presigned-url", url);
        verify(s3Presigner).presignPutObject(any(Consumer.class));
    }

    @Test
    void generatePresignedPutUrlShouldThrowStorageExceptionWhenS3Fails() {
        when(s3Presigner.presignPutObject(any(Consumer.class))).thenThrow(S3Exception.builder().message("Access Denied").build());

        var exception = assertThrows(StorageException.class,
                () -> adapter.generatePresignedPutUrl(OBJECT_KEY, CONTENT_TYPE, EXPIRATION));

        assertTrue(exception.getMessage().contains(OBJECT_KEY));
        assertInstanceOf(S3Exception.class, exception.getCause());
    }

    @Test
    void shouldUploadFileSuccessfully() {
        byte[] content = "report-data".getBytes();
        InputStream data = new ByteArrayInputStream(content);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        adapter.uploadFile(OBJECT_KEY, data, content.length, CONTENT_TYPE);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFileShouldThrowStorageExceptionWhenS3Fails() {
        byte[] content = "report-data".getBytes();
        InputStream data = new ByteArrayInputStream(content);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Bucket not found").build());

        var exception = assertThrows(StorageException.class,
                () -> adapter.uploadFile(OBJECT_KEY, data, content.length, CONTENT_TYPE));

        assertTrue(exception.getMessage().contains(OBJECT_KEY));
        assertInstanceOf(S3Exception.class, exception.getCause());
    }

    @Test
    void generatePresignedPutUrlShouldUseProvidedContentType() throws Exception {
        var presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://r2.example.com/upload"));
        when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(presignedRequest);

        adapter.generatePresignedPutUrl(OBJECT_KEY, "image/png", EXPIRATION);

        verify(s3Presigner).presignPutObject(any(Consumer.class));
    }

    @Test
    void uploadFileShouldHandleLargeContent() {
        byte[] largeContent = new byte[10_000_000];
        InputStream data = new ByteArrayInputStream(largeContent);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        adapter.uploadFile(OBJECT_KEY, data, largeContent.length, "application/octet-stream");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void shouldBuildPublicUrl() {
        String publicUrl = adapter.buildPublicUrl("evidences/uuid/doc.pdf");

        assertEquals("http://localhost:9000/test-bucket/evidences/uuid/doc.pdf", publicUrl);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldInvokePresignerConsumerWithCorrectBuilder() throws Exception {
        var presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://r2.example.com/upload").toURL());

        var putObjBuilder = mock(PutObjectRequest.Builder.class);
        when(putObjBuilder.bucket(anyString())).thenReturn(putObjBuilder);
        when(putObjBuilder.key(anyString())).thenReturn(putObjBuilder);
        when(putObjBuilder.contentType(anyString())).thenReturn(putObjBuilder);

        var presignBuilderMock = mock(PutObjectPresignRequest.Builder.class);
        when(presignBuilderMock.signatureDuration(any(Duration.class))).thenReturn(presignBuilderMock);
        doAnswer(inv -> {
            ((Consumer<PutObjectRequest.Builder>) inv.getArgument(0)).accept(putObjBuilder);
            return presignBuilderMock;
        }).when(presignBuilderMock).putObjectRequest(any(Consumer.class));

        ArgumentCaptor<Consumer<PutObjectPresignRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);
        when(s3Presigner.presignPutObject(captor.capture())).thenReturn(presignedRequest);

        adapter.generatePresignedPutUrl(OBJECT_KEY, CONTENT_TYPE, EXPIRATION);

        Consumer<PutObjectPresignRequest.Builder> captured = captor.getValue();
        captured.accept(presignBuilderMock);

        verify(presignBuilderMock).signatureDuration(EXPIRATION);
        verify(presignBuilderMock).putObjectRequest(any(Consumer.class));
        verify(putObjBuilder).bucket(BUCKET);
        verify(putObjBuilder).key(OBJECT_KEY);
        verify(putObjBuilder).contentType(CONTENT_TYPE);
    }

    @Test
    void shouldBuildPublicUrlWhenTrailingSlash() {
        properties.setPublicUrl("http://localhost:9000/test-bucket/");
        String publicUrl = adapter.buildPublicUrl("evidences/doc.pdf");

        assertEquals("http://localhost:9000/test-bucket/evidences/doc.pdf", publicUrl);
    }

    @Test
    void buildPublicUrlShouldThrowWhenPublicUrlIsNull() {
        properties.setPublicUrl(null);

        assertThrows(StorageException.class, () -> adapter.buildPublicUrl("key"));
    }

    @Test
    void buildPublicUrlShouldThrowWhenPublicUrlIsBlank() {
        properties.setPublicUrl("  ");

        assertThrows(StorageException.class, () -> adapter.buildPublicUrl("key"));
    }

    @Test
    void downloadFileShouldReturnStream() {
        byte[] content = "evidence-bytes".getBytes();
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        InputStream result = adapter.downloadFile(OBJECT_KEY);

        assertSame(responseStream, result);
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void downloadFileShouldThrowStorageExceptionWhenS3Fails() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Object not found").build());

        var exception = assertThrows(StorageException.class,
                () -> adapter.downloadFile(OBJECT_KEY));

        assertTrue(exception.getMessage().contains(OBJECT_KEY));
        assertInstanceOf(S3Exception.class, exception.getCause());
    }
}
