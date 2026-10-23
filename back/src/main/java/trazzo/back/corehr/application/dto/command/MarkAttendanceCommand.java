package trazzo.back.corehr.application.dto.command;

import java.time.LocalDateTime;

/** offlineEventId: id del evento en la cola offline del agente (null en la marcación en vivo). */
public record MarkAttendanceCommand(
        String encryptedTemplateBase64,
        String encryptedAesKeyBase64,
        String ivBase64,
        String tagBase64,
        LocalDateTime capturedAtUtc,
        String deviceCode,
        Integer offlineEventId
) {
}
