package trazzo.back.reports.infrastructure.adapters.out.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class MonthlyClosureDetailEntity {

    private UUID id;
    private UUID monthClosureId;
    private Integer tenantUserId;
    private String tenantUserFullName;
    private String tenantUserDocument;
    private String departmentName;
    private String roleName;
    private Double totalWorkedHours;
    private Integer totalTardinessMinutes;
    private int totalAbsences;
    private Double totalOvertimeHours;
    private LocalDateTime createdAt;

    public MonthlyClosureDetailEntity() {
    }

    public MonthlyClosureDetailEntity(UUID id, UUID monthClosureId, Integer tenantUserId,
                                       String tenantUserFullName, String tenantUserDocument,
                                       String departmentName, String roleName,
                                       Double totalWorkedHours, Integer totalTardinessMinutes,
                                       int totalAbsences, Double totalOvertimeHours,
                                       LocalDateTime createdAt) {
        this.id = id;
        this.monthClosureId = monthClosureId;
        this.tenantUserId = tenantUserId;
        this.tenantUserFullName = tenantUserFullName;
        this.tenantUserDocument = tenantUserDocument;
        this.departmentName = departmentName;
        this.roleName = roleName;
        this.totalWorkedHours = totalWorkedHours;
        this.totalTardinessMinutes = totalTardinessMinutes;
        this.totalAbsences = totalAbsences;
        this.totalOvertimeHours = totalOvertimeHours;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMonthClosureId() {
        return monthClosureId;
    }

    public void setMonthClosureId(UUID monthClosureId) {
        this.monthClosureId = monthClosureId;
    }

    public Integer getTenantUserId() {
        return tenantUserId;
    }

    public void setTenantUserId(Integer tenantUserId) {
        this.tenantUserId = tenantUserId;
    }

    public String getTenantUserFullName() {
        return tenantUserFullName;
    }

    public void setTenantUserFullName(String tenantUserFullName) {
        this.tenantUserFullName = tenantUserFullName;
    }

    public String getTenantUserDocument() {
        return tenantUserDocument;
    }

    public void setTenantUserDocument(String tenantUserDocument) {
        this.tenantUserDocument = tenantUserDocument;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Double getTotalWorkedHours() {
        return totalWorkedHours;
    }

    public void setTotalWorkedHours(Double totalWorkedHours) {
        this.totalWorkedHours = totalWorkedHours;
    }

    public Integer getTotalTardinessMinutes() {
        return totalTardinessMinutes;
    }

    public void setTotalTardinessMinutes(Integer totalTardinessMinutes) {
        this.totalTardinessMinutes = totalTardinessMinutes;
    }

    public int getTotalAbsences() {
        return totalAbsences;
    }

    public void setTotalAbsences(int totalAbsences) {
        this.totalAbsences = totalAbsences;
    }

    public Double getTotalOvertimeHours() {
        return totalOvertimeHours;
    }

    public void setTotalOvertimeHours(Double totalOvertimeHours) {
        this.totalOvertimeHours = totalOvertimeHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
