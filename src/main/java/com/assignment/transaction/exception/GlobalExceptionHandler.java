package com.assignment.transaction.exception;

import com.assignment.transaction.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Global exception handler for all REST controllers.
 *
 * <h3>Why @RestControllerAdvice?</h3>
 * <p>Without this, Spring Boot returns its default "Whitelabel Error Page" HTML
 * for browser requests or a generic JSON with limited detail for API requests.
 * This handler ensures every error — validation, business, or unexpected —
 * returns a consistent {@link ApiErrorResponse} JSON structure.</p>
 *
 * <h3>Handler Priority</h3>
 * <p>Handlers are matched from most-specific to least-specific exception type.
 * Spring tries each {@code @ExceptionHandler} method until it finds one whose
 * exception class matches the thrown exception.</p>
 *
 * <h3>Handled Exceptions</h3>
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} → 400 (Bean Validation failures)</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (malformed JSON)</li>
 *   <li>{@link AccountNotFoundException} → 404</li>
 *   <li>{@link InsufficientBalanceException} → 422 (Unprocessable Entity)</li>
 *   <li>{@link Exception} → 500 (catch-all for unexpected errors)</li>
 * </ol>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures from {@code @Valid} on request bodies.
     *
     * <p>Extracts each field error into a human-readable string like
     * "sourceAccountId: Source account ID must not be null" and returns
     * them all in the {@code validationErrors} list.</p>
     *
     * @param ex      the validation exception containing field errors
     * @param request the web request (for extracting the request path)
     * @return 400 Bad Request with per-field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        log.warn("Validation failed for request {}: {}", getPath(request), errors);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Validation failed")
                .path(getPath(request))
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles malformed JSON in the request body.
     *
     * <p>Triggered when Jackson cannot deserialize the JSON — e.g., invalid
     * date format, missing quotes, or completely unparseable content.</p>
     *
     * @param ex      the deserialization exception
     * @param request the web request
     * @return 400 Bad Request with a user-friendly message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("Malformed JSON in request {}: {}", getPath(request), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Malformed JSON request body")
                .path(getPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles requests referencing a non-existent account.
     *
     * @param ex      the exception with the missing account ID
     * @param request the web request
     * @return 404 Not Found
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex, WebRequest request) {

        log.warn("Account not found: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles overdraft attempts.
     *
     * <p>Returns HTTP 422 Unprocessable Entity — the request was syntactically
     * valid but semantically incorrect (not enough funds). 400 would be wrong
     * because the request format is correct; 409 Conflict is also acceptable.</p>
     *
     * @param ex      the exception with balance details
     * @param request the web request
     * @return 422 Unprocessable Entity
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex, WebRequest request) {

        log.warn("Insufficient balance: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Catch-all handler for any unexpected exception.
     *
     * <p>Logs the full stack trace (critical for debugging) but returns
     * a generic message to the client (never expose internal details
     * like class names, SQL queries, or stack traces in API responses).</p>
     *
     * @param ex      the unexpected exception
     * @param request the web request
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error on request {}: {}", getPath(request), ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(getPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extracts the request URI path from the WebRequest.
     *
     * @param request the web request
     * @return the URI path, or "unknown" if extraction fails
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
