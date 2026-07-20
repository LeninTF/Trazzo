package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.TenantMetricsResult;
import trazzo.back.saasglobal.application.dto.result.TenantResult;

public interface SaasTenantUseCase {
    PaginatedResult<TenantResult> listAll(String search, Integer planId, String status, int page, int size);
    TenantResult getById(String id);
    TenantMetricsResult getMetrics();
    TenantResult suspend(String id);
    TenantResult reactivate(String id);
    TenantResult updateBranding(String id, String logoUrl, String slogan, String primaryColor, String secondaryColor);
    void deleteById(String id);
}
