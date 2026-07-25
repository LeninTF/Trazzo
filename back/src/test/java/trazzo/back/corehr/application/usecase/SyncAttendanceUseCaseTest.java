package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.dto.command.SyncAttendanceBatchItemCommand;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.SyncItemResult;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDateTime;
import java.util.List;

class SyncAttendanceUseCaseTest {

    private MarkAttendanceUseCase markAttendanceUseCase;
    private AttendanceRepositoryPort attendanceRepository;
    private EventPublisherPort eventPublisher;
    private SyncAttendanceUseCase useCase;

    @BeforeEach
    void setUp() {
        markAttendanceUseCase = mock(MarkAttendanceUseCase.class);
        attendanceRepository = mock(AttendanceRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        useCase = new SyncAttendanceUseCase(markAttendanceUseCase, attendanceRepository, eventPublisher);
    }

    private SyncAttendanceBatchItemCommand buildItem(String deviceCode, Integer offlineEventId) {
        return new SyncAttendanceBatchItemCommand(
                "encTemplate", "encAesKey", "iv", "tag",
                LocalDateTime.of(2025, 7, 15, 9, 0),
                deviceCode, offlineEventId, 0
        );
    }

    private AttendanceResult buildResult(String id, Long tenantUserId, AttendanceState state, int minutesLate, LocalDateTime checkIn) {
        return new AttendanceResult(
                id, tenantUserId, null, 10L, null, 1L, "DEV-01",
                checkIn, null, LocalDateTime.of(2025, 7, 15, 9, 0).toLocalDate(),
                minutesLate, state, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void syncBatch_shouldReturnAllSuccesses_whenNoDuplicatesOrErrors() {
        var item1 = buildItem("DEV-01", 101);
        var item2 = buildItem("DEV-01", 102);
        var tenantUserId = 1L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(101, "DEV-01")).thenReturn(false);
        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(102, "DEV-01")).thenReturn(false);

        var result1 = buildResult("att-1", tenantUserId, AttendanceState.PUNTUAL, 0,
                LocalDateTime.of(2025, 7, 15, 9, 0));
        var result2 = buildResult("att-2", tenantUserId, AttendanceState.TARDANZA, 15,
                LocalDateTime.of(2025, 7, 15, 9, 15));

        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class)))
                .thenReturn(result1)
                .thenReturn(result2);

        var batchResult = useCase.syncBatch(List.of(item1, item2), tenantUserId);

        assertNotNull(batchResult.correlationId());
        assertEquals(2, batchResult.acceptedCount());
        assertEquals(2, batchResult.itemResults().size());
        assertTrue(batchResult.itemResults().get(0).success());
        assertTrue(batchResult.itemResults().get(1).success());
        assertEquals("att-1", batchResult.itemResults().get(0).attendanceId());
        assertEquals("att-2", batchResult.itemResults().get(1).attendanceId());
        verify(eventPublisher).publish(any());
    }

    @Test
    void syncBatch_shouldSkipDuplicateEvents() {
        var item1 = buildItem("DEV-01", 101);
        var item2 = buildItem("DEV-01", 102);
        var tenantUserId = 1L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(101, "DEV-01")).thenReturn(true);
        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(102, "DEV-01")).thenReturn(false);

        var result2 = buildResult("att-2", tenantUserId, AttendanceState.PUNTUAL, 0,
                LocalDateTime.of(2025, 7, 15, 9, 0));
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class))).thenReturn(result2);

        var batchResult = useCase.syncBatch(List.of(item1, item2), tenantUserId);

        assertEquals(1, batchResult.acceptedCount());
        assertEquals(2, batchResult.itemResults().size());
        assertFalse(batchResult.itemResults().get(0).success());
        assertNull(batchResult.itemResults().get(0).attendanceId());
        assertTrue(batchResult.itemResults().get(1).success());
        assertEquals(101, batchResult.itemResults().get(0).offlineEventId());
        verify(markAttendanceUseCase, times(1)).mark(any());
    }

    @Test
    void syncBatch_shouldReturnFailures_whenMarkThrows() {
        var item1 = buildItem("DEV-01", 101);
        var tenantUserId = 1L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(101, "DEV-01")).thenReturn(false);
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class)))
                .thenThrow(new IllegalArgumentException("Huella no reconocida"));

        var batchResult = useCase.syncBatch(List.of(item1), tenantUserId);

        assertEquals(0, batchResult.acceptedCount());
        assertEquals(1, batchResult.itemResults().size());
        assertFalse(batchResult.itemResults().get(0).success());
        assertNotNull(batchResult.itemResults().get(0).error());
        verify(eventPublisher).publish(any());
    }

    @Test
    void syncBatch_shouldReturnMixedResults_whenMixOfSuccessDuplicatesAndFailures() {
        var item1 = buildItem("DEV-01", 101);
        var item2 = buildItem("DEV-01", 102);
        var item3 = buildItem("DEV-01", 103);
        var tenantUserId = 1L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(101, "DEV-01")).thenReturn(true);
        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(102, "DEV-01")).thenReturn(false);
        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(103, "DEV-01")).thenReturn(false);

        var successResult = buildResult("att-2", tenantUserId, AttendanceState.PUNTUAL, 0,
                LocalDateTime.of(2025, 7, 15, 9, 0));
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class)))
                .thenReturn(successResult)
                .thenThrow(new IllegalStateException("Dispositivo inactivo"));

        var batchResult = useCase.syncBatch(List.of(item1, item2, item3), tenantUserId);

        assertEquals(1, batchResult.acceptedCount());
        assertEquals(3, batchResult.itemResults().size());

        assertFalse(batchResult.itemResults().get(0).success());
        assertNull(batchResult.itemResults().get(0).attendanceId());

        assertTrue(batchResult.itemResults().get(1).success());
        assertEquals("att-2", batchResult.itemResults().get(1).attendanceId());

        assertFalse(batchResult.itemResults().get(2).success());
        assertNotNull(batchResult.itemResults().get(2).error());
    }

    @Test
    void syncBatch_shouldPublishBatchAcceptedEvent_withAcceptedCount() {
        var item1 = buildItem("DEV-01", 101);
        var item2 = buildItem("DEV-01", 102);
        var tenantUserId = 5L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(101, "DEV-01")).thenReturn(false);
        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(102, "DEV-01")).thenReturn(true);

        var result1 = buildResult("att-1", tenantUserId, AttendanceState.PUNTUAL, 0,
                LocalDateTime.of(2025, 7, 15, 9, 0));
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class))).thenReturn(result1);

        useCase.syncBatch(List.of(item1, item2), tenantUserId);

        verify(eventPublisher).publish(argThat(event ->
                event instanceof trazzo.back.corehr.domain.event.AttendanceSyncBatchAcceptedEvent
                        && ((trazzo.back.corehr.domain.event.AttendanceSyncBatchAcceptedEvent) event).acceptedCount() == 1
                        && ((trazzo.back.corehr.domain.event.AttendanceSyncBatchAcceptedEvent) event).tenantUserId().equals(5L)
        ));
    }

    @Test
    void syncBatch_shouldReturnEmptyResults_whenNoItems() {
        var batchResult = useCase.syncBatch(List.of(), 1L);

        assertNotNull(batchResult.correlationId());
        assertEquals(0, batchResult.acceptedCount());
        assertTrue(batchResult.itemResults().isEmpty());
        verify(eventPublisher).publish(any());
    }

    @Test
    void syncBatch_shouldGenerateUniqueCorrelationIds() {
        var batch1 = useCase.syncBatch(List.of(), 1L);
        var batch2 = useCase.syncBatch(List.of(), 1L);

        assertNotEquals(batch1.correlationId(), batch2.correlationId());
    }

    @Test
    void syncBatch_shouldForwardCorrectCommandToMarkAttendance() {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 10, 30);
        var item = new SyncAttendanceBatchItemCommand(
                "templateData", "aesKeyData", "ivData", "tagData",
                capturedAt, "DEV-02", 200, 3
        );
        var tenantUserId = 7L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(200, "DEV-02")).thenReturn(false);

        var markResult = buildResult("att-forward", tenantUserId, AttendanceState.TARDANZA, 20, capturedAt);
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class))).thenReturn(markResult);

        useCase.syncBatch(List.of(item), tenantUserId);

        verify(markAttendanceUseCase).mark(argThat(cmd ->
                cmd.encryptedTemplateBase64().equals("templateData")
                        && cmd.encryptedAesKeyBase64().equals("aesKeyData")
                        && cmd.ivBase64().equals("ivData")
                        && cmd.tagBase64().equals("tagData")
                        && cmd.capturedAtUtc().equals(capturedAt)
                        && cmd.deviceCode().equals("DEV-02")
        ));
    }

    @Test
    void syncBatch_shouldContinueProcessingAfterFailure() {
        var item1 = buildItem("DEV-01", 101);
        var item2 = buildItem("DEV-01", 102);
        var tenantUserId = 1L;

        when(attendanceRepository.existsByOfflineEventIdAndDeviceCode(anyInt(), anyString())).thenReturn(false);

        var failResult = mock(MarkAttendanceCommand.class);
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class)))
                .thenThrow(new RuntimeException("Network error"))
                .thenReturn(buildResult("att-2", tenantUserId, AttendanceState.PUNTUAL, 0,
                        LocalDateTime.of(2025, 7, 15, 9, 0)));

        var batchResult = useCase.syncBatch(List.of(item1, item2), tenantUserId);

        assertEquals(1, batchResult.acceptedCount());
        assertFalse(batchResult.itemResults().get(0).success());
        assertTrue(batchResult.itemResults().get(1).success());
    }

    @Test
    void syncBatch_shouldSkip_whenOfflineEventIdIsNull() {
        var item = buildItem("DEV-01", null);
        var tenantUserId = 1L;

        var result = buildResult("att-1", tenantUserId, AttendanceState.PUNTUAL, 0,
                LocalDateTime.of(2025, 7, 15, 9, 0));
        when(markAttendanceUseCase.mark(any(MarkAttendanceCommand.class))).thenReturn(result);

        var batchResult = useCase.syncBatch(List.of(item), tenantUserId);

        assertEquals(1, batchResult.acceptedCount());
        assertTrue(batchResult.itemResults().get(0).success());
        verify(attendanceRepository, never()).existsByOfflineEventIdAndDeviceCode(anyInt(), anyString());
    }
}
