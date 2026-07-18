package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    List<ValidationDetail> details
) {
    public ErrorResponse(int status, String error, String message) {
        this(LocalDateTime.now(), status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, List<ValidationDetail> details) {
        this(LocalDateTime.now(), status, error, message, details);
    }

    public record ValidationDetail(String field, String message) {}
}
