package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.port.out.InvoiceRepositoryPort;
import trazzo.back.saasglobal.domain.model.invoice.Invoice;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepositoryPort invoiceRepository;
    @InjectMocks InvoiceService service;

    private static Invoice invoice(String id) {
        return Invoice.restore(id, null, "tenant-1", null, "F001", "001", "01_FACTURA",
                "20111111111", "Trazzo SAC", "Av. Lima 123",
                "20222222222", "Cliente SAC", "Av. Arequipa 456",
                "PEN", null, BigDecimal.valueOf(100), BigDecimal.valueOf(18), BigDecimal.valueOf(118),
                null, "PENDIENTE", null, null, LocalDateTime.now());
    }

    @Test
    void listAll_returnsPaginatedResults() {
        when(invoiceRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(invoice("inv-1")));
        when(invoiceRepository.countByFilters(isNull(), isNull(), isNull(), isNull())).thenReturn(1L);

        PaginatedResult<InvoiceResult> result = service.listAll(null, null, null, null, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals("inv-1", result.content().get(0).id());
    }

    @Test
    void listAll_parsesDateFilters() {
        when(invoiceRepository.findByFilters(eq("PENDIENTE"), eq("tenant-1"),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 6, 1)), eq(0), eq(20)))
                .thenReturn(List.of());
        when(invoiceRepository.countByFilters(any(), any(), any(), any())).thenReturn(0L);

        PaginatedResult<InvoiceResult> result =
                service.listAll("PENDIENTE", "tenant-1", "2026-01-01", "2026-06-01", 0, 20);

        assertTrue(result.content().isEmpty());
    }

    @Test
    void getById_returnsResult() {
        when(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice("inv-1")));

        InvoiceResult result = service.getById("inv-1");

        assertEquals("F001", result.invoiceSeries());
        assertEquals(BigDecimal.valueOf(118), result.total());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(invoiceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById("missing"));
    }

    @Test
    void listAllMatching_returnsAllWithoutPagination() {
        when(invoiceRepository.findAllMatching(any(), any(), any(), any()))
                .thenReturn(List.of(invoice("inv-1"), invoice("inv-2")));

        List<InvoiceResult> results = service.listAllMatching(null, null, null, null);

        assertEquals(2, results.size());
    }
}
