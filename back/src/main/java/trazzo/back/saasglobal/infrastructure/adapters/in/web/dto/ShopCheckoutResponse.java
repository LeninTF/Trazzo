package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import trazzo.back.saasglobal.application.dto.result.ShopCheckoutResult;

public record ShopCheckoutResponse(String tenantId, String subDomain, String initPoint) {
    public static ShopCheckoutResponse from(ShopCheckoutResult result) {
        return new ShopCheckoutResponse(result.tenantId(), result.subDomain(), result.initPoint());
    }
}
