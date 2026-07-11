package trazzo.back.organization.infrastructure.adapters.in.web.dto;

import java.util.List;

public record ErrorResponse(int status, String error, String message, List<ValidationDetail> details) {

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public record ValidationDetail(String field, String message) {}
}
