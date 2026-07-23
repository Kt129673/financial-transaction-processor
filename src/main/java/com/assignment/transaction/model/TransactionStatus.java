package com.assignment.transaction.model;

/**
 * Represents the lifecycle state of a financial transaction.
 *
 * <p>State transitions:</p>
 * <ul>
 *   <li>{@code PENDING} → {@code SUCCESS} (funds transferred successfully)</li>
 *   <li>{@code PENDING} → {@code FAILED} (validation error, insufficient balance, duplicate, etc.)</li>
 * </ul>
 *
 * <p>Stored as a {@code VARCHAR} in the database via {@code @Enumerated(EnumType.STRING)}
 * rather than ordinal, so adding new statuses won't break existing data.</p>
 */
public enum TransactionStatus {

    /** Transaction completed successfully — funds were debited and credited. */
    SUCCESS,

    /** Transaction failed — see {@code failureReason} on the Transaction entity for details. */
    FAILED,

    /** Transaction is queued for asynchronous processing. */
    PENDING
}
