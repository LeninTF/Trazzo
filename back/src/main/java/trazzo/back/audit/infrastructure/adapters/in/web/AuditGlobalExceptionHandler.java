package trazzo.back.audit.infrastructure.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.exception.AuditValidationException;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.ErrorResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.ErrorResponse.ValidationDetail;

@RestControllerAdvice("trazzo.back.audit")
public class AuditGlobalExceptionHandler {

    @ExceptionHandler(AuditNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuditNotFound(AuditNotFoundException ex) {
        var error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AuditValidationException.class)
    public ResponseEntity<ErrorResponse> handleAuditValidation(AuditValidationException ex) {
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        var error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
