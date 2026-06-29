package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.corehr.domain.exception.*;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ErrorResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ErrorResponse.ValidationDetail;

@RestControllerAdvice("trazzo.back.corehr")
public class CoreHrGlobalExceptionHandler {

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

    @ExceptionHandler(CoreHrValidationException.class)
    public ResponseEntity<ErrorResponse> handleCoreHrValidation(CoreHrValidationException ex) {
        var error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidAttendanceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAttendance(InvalidAttendanceException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    @ExceptionHandler(InactiveDeviceException.class)
    public ResponseEntity<ErrorResponse> handleInactiveDevice(InactiveDeviceException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    @ExceptionHandler(InvalidScheduleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSchedule(InvalidScheduleException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    @ExceptionHandler(InvalidShiftException.class)
    public ResponseEntity<ErrorResponse> handleInvalidShift(InvalidShiftException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    @ExceptionHandler(InvalidToleranciaException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTolerancia(InvalidToleranciaException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    @ExceptionHandler(InvalidNonWorkingDaysException.class)
    public ResponseEntity<ErrorResponse> handleInvalidNonWorkingDays(InvalidNonWorkingDaysException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
    }

    @ExceptionHandler(InvalidTenantUserException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTenantUser(InvalidTenantUserException ex) {
        var error = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(error);
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
}
