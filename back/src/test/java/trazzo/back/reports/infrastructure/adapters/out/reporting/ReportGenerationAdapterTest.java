package trazzo.back.reports.infrastructure.adapters.out.reporting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class ReportGenerationAdapterTest {

    private FileStoragePort fileStoragePort;
    private ReportGenerationAdapter adapter;

    private MonthlyClosure sampleClosure;
    private MonthlyClosureDetail sampleDetail;

    @BeforeEach
    void setUp() {
        fileStoragePort = mock(FileStoragePort.class);
        doNothing().when(fileStoragePort).uploadFile(anyString(), any(), anyLong(), anyString());
        when(fileStoragePort.buildPublicUrl(anyString())).thenAnswer(inv ->
                "http://r2.example.com/" + inv.getArgument(0));
        adapter = new ReportGenerationAdapter(fileStoragePort);

        sampleClosure = new MonthlyClosure(
                UUID.randomUUID(), 6, 2025, 10, null, null, UUID.randomUUID(), LocalDateTime.now());
        sampleDetail = new MonthlyClosureDetail(
                UUID.randomUUID(), sampleClosure.getId(), 1L,
                "Juan Perez", "12345678", "IT", "Dev",
                160.0, 30, 2, 10.0, LocalDateTime.now());
    }

    @Test
    void shouldGenerateExcelReportUrl() {
        String url = adapter.generateExcelReport(sampleClosure, List.of());

        assertTrue(url.startsWith("http://r2.example.com/"));
        assertTrue(url.contains("excel.xlsx"));
        assertTrue(url.contains(sampleClosure.getId().toString()));
    }

    @Test
    void shouldGenerateExcelReportWithDetails() {
        String url = adapter.generateExcelReport(sampleClosure, List.of(sampleDetail));

        assertTrue(url.contains("excel.xlsx"));
        verify(fileStoragePort).uploadFile(
                argThat(key -> key.contains("excel.xlsx")),
                any(), anyLong(),
                argThat(ct -> ct.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @Test
    void shouldGeneratePdfReportUrl() {
        String url = adapter.generatePdfReport(sampleClosure, List.of());

        assertTrue(url.startsWith("http://r2.example.com/"));
        assertTrue(url.contains("report.pdf"));
        assertTrue(url.contains(sampleClosure.getId().toString()));
    }

    @Test
    void shouldGeneratePdfReportWithDetails() {
        String url = adapter.generatePdfReport(sampleClosure, List.of(sampleDetail));

        assertTrue(url.contains("report.pdf"));
        verify(fileStoragePort).uploadFile(
                argThat(key -> key.contains("report.pdf")),
                any(), anyLong(),
                argThat(ct -> ct.equals("application/pdf")));
    }

    @Test
    void shouldGenerateDifferentUrlsForDifferentClosures() {
        MonthlyClosure c2 = new MonthlyClosure(
                UUID.randomUUID(), 7, 2025, 5, null, null, UUID.randomUUID(), LocalDateTime.now());

        String excel1 = adapter.generateExcelReport(sampleClosure, List.of());
        String excel2 = adapter.generateExcelReport(c2, List.of());

        assertNotEquals(excel1, excel2);
    }

    @Test
    void shouldThrowWhenExcelUploadFails() {
        doThrow(new RuntimeException("upload error"))
                .when(fileStoragePort).uploadFile(anyString(), any(), anyLong(), anyString());

        assertThrows(RuntimeException.class,
                () -> adapter.generateExcelReport(sampleClosure, List.of()));
    }

    @Test
    void shouldThrowWhenPdfUploadFails() {
        doThrow(new RuntimeException("upload error"))
                .when(fileStoragePort).uploadFile(anyString(), any(), anyLong(), anyString());

        assertThrows(RuntimeException.class,
                () -> adapter.generatePdfReport(sampleClosure, List.of()));
    }

    @Test
    void shouldIncludeColumnHeadersInExcel() {
        adapter.generateExcelReport(sampleClosure, List.of(sampleDetail));

        verify(fileStoragePort).uploadFile(
                argThat(key -> key.contains("excel.xlsx")),
                any(), anyLong(), anyString());
    }
}
