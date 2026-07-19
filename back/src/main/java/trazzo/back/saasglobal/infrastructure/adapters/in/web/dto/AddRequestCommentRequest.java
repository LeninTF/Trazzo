package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddRequestCommentRequest(
        @NotBlank String comment
) {}
