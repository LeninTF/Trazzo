package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(@NotNull Integer planId) {}
