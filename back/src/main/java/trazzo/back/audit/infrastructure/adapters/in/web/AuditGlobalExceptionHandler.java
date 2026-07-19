package trazzo.back.audit.infrastructure.adapters.in.web;

import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.exception.AuditValidationException;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.ErrorResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.ErrorResponse.ValidationDetail;

@RestControllerAdvice("trazzo.back.audit")
public class AuditGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditGlobalExceptionHandler.class);

    @ExceptionHandler(AuditNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuditNotFound(AuditNotFoundException ex) {
        log.warn("Audit not found: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AuditValidationException.class)
    public ResponseEntity<ErrorResponse> handleAuditValidation(AuditValidationException ex) {
        log.warn("Audit validation error: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error",
                "Validation failed for one or more fields", details);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException ex) {
        log.warn("Invalid date format: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Invalid date format");
        return ResponseEntity.badRequest().body(error);
    }

    // See trazzo.back.saasglobal...SaasGlobalExceptionHandler for why this is needed: @PreAuthorize
    // failures throw inside the controller call and would otherwise be swallowed into a 500 by
    // the Exception.class catch-all below instead of surfacing as the correct 403.
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", "No tienes permiso para esta acción");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        var error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "Unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
