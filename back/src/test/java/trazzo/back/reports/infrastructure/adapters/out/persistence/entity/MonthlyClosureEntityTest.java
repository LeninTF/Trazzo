package trazzo.back.reports.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureEntityTest {

    @Test
    void shouldCreateEntitySuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureEntity entity = new MonthlyClosureEntity(id, 6, 2025, 10, "excel", "pdf", "user-1", now);
        assertEquals(id, entity.getId());
        assertEquals(6, entity.getMonth());
        assertEquals(2025, entity.getYear());
        assertEquals(10, entity.getTotalEmployees());
        assertEquals("excel", entity.getExcelReportUrl());
        assertEquals("pdf", entity.getPdfReportUrl());
        assertEquals("user-1", entity.getCreatedByUserId());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldSupportDefaultConstructor() {
        MonthlyClosureEntity entity = new MonthlyClosureEntity();
        assertNull(entity.getId());
    }

    @Test
    void shouldSupportSetters() {
        MonthlyClosureEntity entity = new MonthlyClosureEntity();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        entity.setId(id);
        entity.setMonth(6);
        entity.setYear(2025);
        entity.setTotalEmployees(10);
        entity.setExcelReportUrl("excel");
        entity.setPdfReportUrl("pdf");
        entity.setCreatedByUserId("user-1");
        entity.setCreatedAt(now);
        assertEquals(id, entity.getId());
        assertEquals(6, entity.getMonth());
        assertEquals(2025, entity.getYear());
        assertEquals(10, entity.getTotalEmployees());
        assertEquals("excel", entity.getExcelReportUrl());
        assertEquals("pdf", entity.getPdfReportUrl());
        assertEquals("user-1", entity.getCreatedByUserId());
        assertEquals(now, entity.getCreatedAt());
    }
}
