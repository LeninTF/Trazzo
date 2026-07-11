package trazzo.back.shared.infrastructure.adapters.out.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.shared.domain.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

class LocalStorageStubTest {

    private LocalStorageStub stub;

    @BeforeEach
    void setUp() {
        stub = new LocalStorageStub();
    }

    @Test
    void generatePresignedPutUrlShouldThrowStorageException() {
        var ex = assertThrows(StorageException.class,
                () -> stub.generatePresignedPutUrl("key", "text/plain", Duration.ofMinutes(5)));
        assertTrue(ex.getMessage().contains("Cloudflare R2 is not configured"));
    }

    @Test
    void uploadFileShouldThrowStorageException() {
        InputStream data = new ByteArrayInputStream("content".getBytes());
        var ex = assertThrows(StorageException.class,
                () -> stub.uploadFile("key", data, 7, "text/plain"));
        assertTrue(ex.getMessage().contains("Cloudflare R2 is not configured"));
    }

    @Test
    void buildPublicUrlShouldThrowStorageException() {
        var ex = assertThrows(StorageException.class,
                () -> stub.buildPublicUrl("key"));
        assertTrue(ex.getMessage().contains("Cloudflare R2 is not configured"));
    }
}
