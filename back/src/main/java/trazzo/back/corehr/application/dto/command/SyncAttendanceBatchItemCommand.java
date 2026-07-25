package trazzo.back.corehr.application.dto.command;

import java.time.LocalDateTime;

public record SyncAttendanceBatchItemCommand(
        String encryptedTemplateBase64,
        String encryptedAesKeyBase64,
        String ivBase64,
        String tagBase64,
        LocalDateTime capturedAtUtc,
        String deviceCode,
        Integer offlineEventId,
        Integer retryCount
) {
}
