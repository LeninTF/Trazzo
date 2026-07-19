package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import trazzo.back.saasglobal.application.dto.result.SubscribeToPlanResult;

public record SubscribeResponse(String subscriptionId, String initPoint) {
    public static SubscribeResponse from(SubscribeToPlanResult result) {
        return new SubscribeResponse(result.subscriptionId(), result.initPoint());
    }
}
