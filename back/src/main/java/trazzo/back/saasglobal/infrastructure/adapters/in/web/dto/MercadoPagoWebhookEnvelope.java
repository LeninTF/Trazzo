package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal shape of a Mercado Pago webhook notification body:
 * {id, live_mode, type, date_created, user_id, api_version, action, data:{id}}.
 * Only the fields needed to dispatch and validate the signature are mapped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookEnvelope(String id, String type, String action, DataRef data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataRef(String id) {}
}
