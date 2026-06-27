package trazzo.back.reports.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;
import trazzo.back.reports.infrastructure.adapters.out.persistence.entity.MonthlyClosureDetailEntity;
import trazzo.back.reports.infrastructure.adapters.out.persistence.entity.MonthlyClosureEntity;

import java.util.UUID;

public class MonthlyClosureMapper {

    public MonthlyClosureEntity toEntity(MonthlyClosure domain) {
        if (domain == null) return null;
        return new MonthlyClosureEntity(
                domain.getId(), domain.getMonth(), domain.getYear(),
                domain.getTotalEmployees(), domain.getExcelReportUrl(),
                domain.getPdfReportUrl(), domain.getCreatedByUserId(),
                domain.getCreatedAt());
    }

    public MonthlyClosure toDomain(MonthlyClosureEntity entity) {
        if (entity == null) return null;
        return new MonthlyClosure(
                entity.getId(), entity.getMonth(), entity.getYear(),
                entity.getTotalEmployees(), entity.getExcelReportUrl(),
                entity.getPdfReportUrl(), entity.getCreatedByUserId(),
                entity.getCreatedAt());
    }

    public MonthlyClosureDetailEntity toEntity(MonthlyClosureDetail domain) {
        if (domain == null) return null;
        return new MonthlyClosureDetailEntity(
                domain.getId(), domain.getMonthClosureId(),
                domain.getTenantUserId(), domain.getTenantUserFullName(),
                domain.getTenantUserDocument(), domain.getDepartmentName(),
                domain.getRoleName(), domain.getTotalWorkedHours(),
                domain.getTotalTardinessMinutes(), domain.getTotalAbsences(),
                domain.getTotalOvertimeHours(), domain.getCreatedAt());
    }

    public MonthlyClosureDetail toDomain(MonthlyClosureDetailEntity entity) {
        if (entity == null) return null;
        return new MonthlyClosureDetail(
                entity.getId(), entity.getMonthClosureId(),
                entity.getTenantUserId(), entity.getTenantUserFullName(),
                entity.getTenantUserDocument(), entity.getDepartmentName(),
                entity.getRoleName(), entity.getTotalWorkedHours(),
                entity.getTotalTardinessMinutes(), entity.getTotalAbsences(),
                entity.getTotalOvertimeHours(), entity.getCreatedAt());
    }
}
