package com.assignment.transaction.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response for all API error scenarios.
 *
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li><b>Mirrors Spring's default error structure</b>: Uses the same field names
 *       ({@code timestamp}, {@code status}, {@code error}, {@code message}, {@code path})
 *       that Spring Boot's default error handler produces. This gives clients a consistent
 *       error format whether the error comes from our custom handler or Spring's defaults.</li>
 *   <li><b>@JsonInclude(NON_NULL)</b>: The {@code validationErrors} list is only present
 *       for validation failures (400). For other errors (404, 500), it's null and omitted
 *       from the JSON, keeping responses clean.</li>
 *   <li><b>validationErrors as List&lt;String&gt;</b>: When a {@code @Valid} DTO fails
 *       multiple constraints, each violation is returned as a separate string so the
 *       client can display field-level errors.</li>
 * </ul>
 *
 * <h3>Example — Validation Error</h3>
 * <pre>{@code
 * {
 *   "timestamp": "2026-07-20T10:00:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/v1/transactions/process",
 *   "validationErrors": [
 *     "sourceAccountId: Source account ID must not be null",
 *     "amount: Amount must be positive"
 *   ]
 * }
 * }</pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** When the error occurred. */
    private LocalDateTime timestamp;

    /** HTTP status code (e.g., 400, 404, 500). */
    private int status;

    /** HTTP status reason phrase (e.g., "Bad Request", "Not Found"). */
    private String error;

    /** Human-readable error summary. */
    private String message;

    /** The request URI that triggered the error. */
    private String path;

    /**
     * Per-field validation error messages.
     * Only populated for 400 Bad Request responses from @Valid failures.
     * Null (and omitted from JSON) for all other error types.
     */
    private List<String> validationErrors;
}
