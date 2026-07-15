package trazzo.back.incidents.infrastructure.adapters.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.incidents.domain.exception.*;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.ErrorResponse;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.ErrorResponse.ValidationDetail;

@RestControllerAdvice(basePackages = "trazzo.back.incidents")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IncidentValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IncidentValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidIncidentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidIncidentStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid State", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidIncidentEvidenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEvidence(InvalidIncidentEvidenceException ex) {
        log.warn("Invalid evidence: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid Evidence", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidIncidentPermissionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPermission(InvalidIncidentPermissionException ex) {
        log.warn("Invalid permission: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid Permission", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InactiveIncidentTypeException.class)
    public ResponseEntity<ErrorResponse> handleInactiveType(InactiveIncidentTypeException ex) {
        log.warn("Inactive type: {}", ex.getMessage());
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Inactive Type", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Method argument validation failed");
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error",
                "Validation failed for one or more fields", details);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error in incidents module", ex);
        var error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
