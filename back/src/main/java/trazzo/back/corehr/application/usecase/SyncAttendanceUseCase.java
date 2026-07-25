package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.dto.command.SyncAttendanceBatchItemCommand;
import trazzo.back.corehr.application.dto.result.SyncItemResult;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.domain.event.AttendanceSyncBatchAcceptedEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SyncAttendanceUseCase {

    private final MarkAttendanceUseCase markAttendanceUseCase;
    private final AttendanceRepositoryPort attendanceRepository;
    private final EventPublisherPort eventPublisher;

    public record SyncBatchResult(UUID correlationId, int acceptedCount, List<SyncItemResult> itemResults) {}

    public SyncBatchResult syncBatch(List<SyncAttendanceBatchItemCommand> items, Long tenantUserId) {
        UUID correlationId = UUID.randomUUID();
        List<SyncItemResult> itemResults = new ArrayList<>();
        int acceptedCount = 0;

        for (var item : items) {
            try {
                if (item.offlineEventId() != null &&
                        attendanceRepository.existsByOfflineEventIdAndDeviceCode(
                                item.offlineEventId(), item.deviceCode())) {
                    itemResults.add(SyncItemResult.skipped(item.offlineEventId()));
                    continue;
                }

                var command = new MarkAttendanceCommand(
                        item.encryptedTemplateBase64(),
                        item.encryptedAesKeyBase64(),
                        item.ivBase64(),
                        item.tagBase64(),
                        item.capturedAtUtc(),
                        item.deviceCode()
                );

                var result = markAttendanceUseCase.mark(command);
                itemResults.add(SyncItemResult.success(
                        result.id(), result.state(), result.minutesLate(),
                        result.checkIn(), item.offlineEventId()));
                acceptedCount++;
            } catch (Exception e) {
                log.warn("Failed to sync event offline_event_id={}, device_code={}: {}",
                        item.offlineEventId(), item.deviceCode(), e.getMessage());
                itemResults.add(SyncItemResult.failure(e.getMessage(), item.offlineEventId()));
            }
        }

        eventPublisher.publish(new AttendanceSyncBatchAcceptedEvent(
                correlationId, acceptedCount, tenantUserId, LocalDateTime.now()));

        return new SyncBatchResult(correlationId, acceptedCount, itemResults);
    }
}
