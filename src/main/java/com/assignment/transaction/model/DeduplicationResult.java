package com.assignment.transaction.model;

import com.assignment.transaction.dto.TransactionRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Holds the result of transaction deduplication.
 *
 * <p>Separates transactions into unique (to be processed) and
 * duplicates (to be marked as FAILED with reason "Duplicate Transaction").</p>
 *
 * <h3>Duplicate definition (from the assignment)</h3>
 * <p>A transaction is a duplicate if another transaction exists with:</p>
 * <ul>
 *   <li>Same source account ID</li>
 *   <li>Same target account ID</li>
 *   <li>Same amount</li>
 *   <li>Timestamp within 2 seconds</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@Builder
public class DeduplicationResult {

    /** Transactions that are unique — no duplicates detected. */
    private final List<TransactionRequestDTO> uniqueTransactions;

    /** Transactions identified as duplicates of an earlier transaction in the batch. */
    private final List<TransactionRequestDTO> duplicateTransactions;
}
