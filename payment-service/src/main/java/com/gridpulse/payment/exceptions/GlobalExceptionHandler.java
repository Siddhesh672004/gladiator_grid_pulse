package com.gridpulse.payment.exceptions;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.gridpulse.payment.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

// the core's error json for every exception (angular reads err.error.message), only what payments can throw
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(GridPulseException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler({ BookingException.class, IllegalArgumentException.class })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // 503: razorpay or the core failed
    @ExceptionHandler({ GatewayException.class, CoreUnavailableException.class })
    public ResponseEntity<ErrorResponse> handleUnavailable(GridPulseException e, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    // the core's own 4xx, passed on as it came
    @ExceptionHandler(CoreRefusedException.class)
    public ResponseEntity<ErrorResponse> handleCoreRefused(CoreRefusedException e, HttpServletRequest request) {
        HttpStatus status = Optional.ofNullable(HttpStatus.resolve(e.getStatus())).orElse(HttpStatus.BAD_REQUEST);
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), e.getMessage(),
                request.getRequestURI(), e.getFieldErrors());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownUrl(NoResourceFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No message available", request);
    }

    // 400: @Valid failed, one message per field, the top message is the first box of the form
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> formOrder = fieldNames(e.getBindingResult().getTarget());
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparingInt((FieldError error) -> placeOnForm(formOrder, error))
                        .thenComparingInt(this::rank))
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (first, later) -> first, LinkedHashMap::new));
        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse("Invalid request");
        ErrorResponse body = new ErrorResponse(400, HttpStatus.BAD_REQUEST.getReasonPhrase(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(400).body(body);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class })
    public ResponseEntity<ErrorResponse> handleUnreadable(Exception e, HttpServletRequest request) {
        String message = (e instanceof MissingServletRequestParameterException missing)
                ? "Missing request parameter: " + missing.getParameterName()
                : "Request data is invalid or in the wrong format";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // 403: wrong role, or someone else's payment
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException e, HttpServletRequest request) {
        String message = "Access Denied".equals(e.getMessage())
                ? "You do not have permission to do this"
                : e.getMessage();
        return build(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Please send the request body as JSON", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "This request conflicts with existing data", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request);
    }

    private List<String> fieldNames(Object body) {
        return Optional.ofNullable(body)
                .map(Object::getClass)
                .filter(Class::isRecord)
                .map(type -> Arrays.stream(type.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList())
                .orElse(List.of());
    }

    // "booking.carNumber" counts as "booking"; a name we do not know goes last
    private int placeOnForm(List<String> formOrder, FieldError error) {
        int place = formOrder.indexOf(error.getField().split("\\.")[0]);
        return (place < 0) ? formOrder.size() : place;
    }

    // required first, then size / min / max, then the rest (the booking form has no patterns)
    private int rank(FieldError error) {
        return switch (String.valueOf(error.getCode())) {
            case "NotBlank", "NotNull" -> 1;
            case "Size", "Min", "Max" -> 2;
            default -> 3;
        };
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
