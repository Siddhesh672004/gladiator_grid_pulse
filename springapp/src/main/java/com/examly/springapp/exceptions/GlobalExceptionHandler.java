package com.examly.springapp.exceptions;

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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.examly.springapp.dto.response.ErrorResponse;
import com.examly.springapp.validation.ValidationRules;

import jakarta.servlet.http.HttpServletRequest;

// every exception from the controllers ends up here and becomes the error json (logged by GlobalLoggingAspect)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 409: duplicate track name, car number taken, session overlaps / is full
    @ExceptionHandler({ DuplicateTrackException.class, TrackDeletionException.class, DuplicateCarNumberException.class,
            ConflictException.class })
    public ResponseEntity<ErrorResponse> handleConflict(GridPulseException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    // 400: bad booking / race control action, failed sign up, wrong or expired code
    @ExceptionHandler({ BookingException.class, RegistrationException.class, OtpException.class,
            IllegalArgumentException.class })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // 503: cloudinary / payment-service failed
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(GridPulseException e, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    // 400: picture upload without a file, or bigger than 5 MB
    @ExceptionHandler({ MultipartException.class, MissingServletRequestPartException.class })
    public ResponseEntity<ErrorResponse> handleUpload(Exception e, HttpServletRequest request) {
        String message = (e instanceof MaxUploadSizeExceededException)
                ? ValidationRules.PICTURE_TOO_BIG
                : ValidationRules.PICTURE_REQUIRED;
        return build(HttpStatus.BAD_REQUEST, message, request);
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
        // a field can break two rules at once: sorted by rank, the first message of each field is kept
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparingInt((FieldError error) -> placeOnForm(formOrder, error))
                        .thenComparingInt(this::rank))
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (first, later) -> first, LinkedHashMap::new));
        // the top message belongs to the first box of the form
        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse("Invalid request");
        ErrorResponse body = new ErrorResponse(400, HttpStatus.BAD_REQUEST.getReasonPhrase(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(400).body(body);
    }

    // 400: broken json, wrong type in the url, missing request parameter
    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class })
    public ResponseEntity<ErrorResponse> handleUnreadable(Exception e, HttpServletRequest request) {
        String message = (e instanceof MissingServletRequestParameterException missing)
                ? "Missing request parameter: " + missing.getParameterName()
                : "Request data is invalid or in the wrong format";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadLogin(BadCredentialsException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid Email or Password", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    // 403: wrong role, or someone else's booking / feedback
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

    // the browser closed the connection before the answer was written (a page that refreshes itself was left)
    // nobody is there to answer and it is not an error of ours, so no 500 and no log line
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientGone() {
    }

    // 409: a unique key the service checks missed
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "This request conflicts with existing data", request);
    }

    // 500: anything unexpected, the user gets a friendly text
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request);
    }

    // the request records list their fields in the order of the form
    private List<String> fieldNames(Object body) {
        // anything that is not a record has no form order
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

    // which message a field shows first: required, then size / min / max, then the patterns in
    // ValidationRules.PATTERN_ORDER, then the rest (same order as errorText in validation.ts)
    private int rank(FieldError error) {
        return switch (String.valueOf(error.getCode())) {
            case "NotBlank", "NotNull" -> 1;
            case "Size", "Min", "Max" -> 2;
            case "Pattern" -> 3 + Math.max(0, ValidationRules.PATTERN_ORDER.indexOf(error.getDefaultMessage()));
            // @Past reads the clock of the server, @Adult checks the same in India time and goes first
            case "Past" -> 4 + ValidationRules.PATTERN_ORDER.size();
            default -> 3 + ValidationRules.PATTERN_ORDER.size();
        };
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
