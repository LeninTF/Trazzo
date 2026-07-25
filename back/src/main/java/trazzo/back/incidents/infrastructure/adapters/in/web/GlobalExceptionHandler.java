package trazzo.back.incidents.infrastructure.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.incidents.domain.exception.*;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.ErrorResponse;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.ErrorResponse.ValidationDetail;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(IncidentValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IncidentValidationException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidIncidentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidIncidentStateException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid State", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidIncidentEvidenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEvidence(InvalidIncidentEvidenceException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid Evidence", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidIncidentPermissionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPermission(InvalidIncidentPermissionException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid Permission", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InactiveIncidentTypeException.class)
    public ResponseEntity<ErrorResponse> handleInactiveType(InactiveIncidentTypeException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Inactive Type", ex.getMessage());
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        var error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", "No tienes permiso para esta acción");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
