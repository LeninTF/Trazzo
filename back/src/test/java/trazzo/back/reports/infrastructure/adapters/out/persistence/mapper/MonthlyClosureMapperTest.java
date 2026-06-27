package trazzo.back.reports.infrastructure.adapters.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;
import trazzo.back.reports.infrastructure.adapters.out.persistence.entity.MonthlyClosureDetailEntity;
import trazzo.back.reports.infrastructure.adapters.out.persistence.entity.MonthlyClosureEntity;

import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureMapperTest {

    private final MonthlyClosureMapper mapper = new MonthlyClosureMapper();

    @Test
    void shouldMapClosureDomainToEntity() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure domain = new MonthlyClosure(id, 6, 2025, 10, "excel", "pdf", "user-1", now);
        MonthlyClosureEntity entity = mapper.toEntity(domain);
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
    void shouldMapClosureEntityToDomain() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureEntity entity = new MonthlyClosureEntity(id, 6, 2025, 10, "excel", "pdf", "user-1", now);
        MonthlyClosure domain = mapper.toDomain(entity);
        assertEquals(id, domain.getId());
        assertEquals(6, domain.getMonth());
        assertEquals(2025, domain.getYear());
        assertEquals(10, domain.getTotalEmployees());
        assertEquals("excel", domain.getExcelReportUrl());
        assertEquals("pdf", domain.getPdfReportUrl());
        assertEquals("user-1", domain.getCreatedByUserId());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldHandleNullClosureDomain() {
        assertNull(mapper.toEntity((MonthlyClosure) null));
        assertNull(mapper.toDomain((MonthlyClosureEntity) null));
    }

    @Test
    void shouldMapDetailDomainToEntity() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail domain = new MonthlyClosureDetail(
                id, closureId, "user-1", "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10.0, 1, 5.0, now);
        MonthlyClosureDetailEntity entity = mapper.toEntity(domain);
        assertEquals(id, entity.getId());
        assertEquals(closureId, entity.getMonthClosureId());
        assertEquals("user-1", entity.getTenantUserId());
        assertEquals("Juan Perez", entity.getTenantUserFullName());
        assertEquals("12345678", entity.getTenantUserDocument());
        assertEquals("TI", entity.getDepartmentName());
        assertEquals("Developer", entity.getRoleName());
        assertEquals(160.0, entity.getTotalWorkedHours());
        assertEquals(10.0, entity.getTotalTardinessMinutes());
        assertEquals(1, entity.getTotalAbsences());
        assertEquals(5.0, entity.getTotalOvertimeHours());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldMapDetailEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailEntity entity = new MonthlyClosureDetailEntity(
                id, closureId, "user-1", "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10.0, 1, 5.0, now);
        MonthlyClosureDetail domain = mapper.toDomain(entity);
        assertEquals(id, domain.getId());
        assertEquals(closureId, domain.getMonthClosureId());
        assertEquals("user-1", domain.getTenantUserId());
        assertEquals("Juan Perez", domain.getTenantUserFullName());
        assertEquals("12345678", domain.getTenantUserDocument());
        assertEquals("TI", domain.getDepartmentName());
        assertEquals("Developer", domain.getRoleName());
        assertEquals(160.0, domain.getTotalWorkedHours());
        assertEquals(10.0, domain.getTotalTardinessMinutes());
        assertEquals(1, domain.getTotalAbsences());
        assertEquals(5.0, domain.getTotalOvertimeHours());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldHandleNullDetailDomain() {
        assertNull(mapper.toEntity((MonthlyClosureDetail) null));
        assertNull(mapper.toDomain((MonthlyClosureDetailEntity) null));
    }
}
