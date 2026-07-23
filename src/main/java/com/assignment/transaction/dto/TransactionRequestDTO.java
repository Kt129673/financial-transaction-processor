package com.assignment.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a single transaction in the incoming API request.
 *
 * <h3>Why a DTO instead of using the Transaction entity directly?</h3>
 * <ul>
 *   <li><b>Decoupling</b>: The API contract (what the client sends) should never be
 *       coupled to the database schema (what Hibernate persists). If we add a column
 *       to the entity, it shouldn't change the API and vice versa.</li>
 *   <li><b>Security</b>: Clients should not be able to set internal fields like
 *       {@code id}, {@code status}, {@code batchId}, or {@code failureReason}.
 *       The DTO exposes only the fields the client is allowed to provide.</li>
 *   <li><b>Validation scope</b>: Validation annotations here apply to the incoming
 *       request only, not to internal entity state transitions.</li>
 * </ul>
 *
 * <h3>Note on validation strategy</h3>
 * <p>These annotations provide a first line of defense (fail-fast for malformed JSON).
 * The assignment also requires Stream-based validation in the service layer — that
 * handles business rules like account existence and deduplication.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequestDTO {

    /**
     * Account ID from which funds will be debited.
     * Must not be null — enforced by Bean Validation before the request
     * reaches the controller method.
     */
    @NotNull(message = "Source account ID must not be null")
    private Long sourceAccountId;

    /**
     * Account ID to which funds will be credited.
     */
    @NotNull(message = "Target account ID must not be null")
    private Long targetAccountId;

    /**
     * Transfer amount. Must be a positive value.
     * Zero and negative amounts are rejected at the Bean Validation level.
     * The Stream-based validator provides a second check for edge cases.
     */
    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Client-provided timestamp of when the transaction was initiated.
     * Used for deduplication — two transactions with the same source,
     * target, amount, and timestamp within 2 seconds are considered duplicates.
     */
    @NotNull(message = "Timestamp must not be null")
    private LocalDateTime timestamp;
}
