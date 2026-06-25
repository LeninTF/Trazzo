package trazzo.back.reports.domain.model.closure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureTest {

    @Test
    void shouldCreateMonthlyClosureSuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        MonthlyClosure closure = new MonthlyClosure(
                id,
                6,
                2025,
                100,
                "excel-url",
                "pdf-url",
                "user-1",
                createdAt);

        assertEquals(id, closure.getId());
        assertEquals(6, closure.getMonth());
        assertEquals(2025, closure.getYear());
        assertEquals(100, closure.getTotalEmployees());
        assertEquals("excel-url", closure.getExcelReportUrl());
        assertEquals("pdf-url", closure.getPdfReportUrl());
        assertEquals("user-1", closure.getCreatedByUserId());
        assertEquals(createdAt, closure.getCreatedAt());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 13 })
    void shouldThrowExceptionForInvalidMonth(int month) {

        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> new MonthlyClosure(
                        id,
                        month,
                        2025,
                        10,
                        "excel",
                        "pdf",
                        "user",
                        createdAt));
    }

    @Test
    void shouldThrowExceptionWhenYearIsInvalid() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,
                () -> new MonthlyClosure(
                        id,
                        6,
                        1999,
                        10,
                        "excel",
                        "pdf",
                        "user",
                        createdAt));
    }

    @Test
    void shouldThrowExceptionWhenTotalEmployeesIsNegative() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,
                () -> new MonthlyClosure(
                        id,
                        6,
                        2025,
                        -1,
                        "excel",
                        "pdf",
                        "user",
                        createdAt));
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        LocalDateTime createdAt = LocalDateTime.now();
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosure(
                        null,
                        6,
                        2025,
                        10,
                        "excel",
                        "pdf",
                        "user",
                        createdAt));
    }

    @Test
    void shouldThrowExceptionWhenCreatedByUserIdIsNull() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosure(
                        id,
                        6,
                        2025,
                        10,
                        "excel",
                        "pdf",
                        null,
                        createdAt));
    }

    @Test
    void shouldThrowExceptionWhenCreatedAtIsNull() {
        UUID id = UUID.randomUUID();
        assertThrows(NullPointerException.class,
                () -> new MonthlyClosure(
                        id,
                        6,
                        2025,
                        10,
                        "excel",
                        "pdf",
                        "user",
                        null));
    }
}