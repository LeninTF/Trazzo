package trazzo.back.shared.infrastructure.adapters.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import trazzo.back.shared.application.port.out.FileStoragePort;
import trazzo.back.shared.infrastructure.adapters.in.web.dto.PresignedUrlResponse;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final FileStoragePort fileStoragePort;

    public StorageController(FileStoragePort fileStoragePort) {
        this.fileStoragePort = fileStoragePort;
    }

    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {
        String objectKey = "evidences/" + UUID.randomUUID() + "/" + fileName;
        String presignedUrl = fileStoragePort.generatePresignedPutUrl(objectKey, contentType, Duration.ofMinutes(15));

        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, objectKey));
    }
}
