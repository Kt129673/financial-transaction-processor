package com.assignment.transaction.validator;

import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.model.DeduplicationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects and removes duplicate transactions from a batch.
 *
 * <h3>Duplicate Definition (from the assignment)</h3>
 * <p>A transaction is considered a duplicate of another if ALL of
 * these conditions are met:</p>
 * <ul>
 *   <li>Same source account ID</li>
 *   <li>Same target account ID</li>
 *   <li>Same amount</li>
 *   <li>Timestamp within 2 seconds of each other</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * <p>For each transaction, we compare it against all previously seen
 * (unique) transactions. If it matches any, it's marked as a duplicate.
 * This is O(n²) in the worst case, but for practical batch sizes (≤10,000)
 * with grouping by source account, the inner list stays small.</p>
 *
 * <h3>Why not use a HashSet/HashMap?</h3>
 * <p>The "within 2 seconds" timestamp criterion is a range check, not an
 * exact match. Hash-based structures require exact key equality. We'd need
 * to bucket timestamps into windows, but that creates edge cases at window
 * boundaries (e.g., 10:00:00.999 and 10:00:01.001 are within 2 seconds
 * but fall into different buckets). A linear scan is correct and readable.</p>
 */
@Component
@Slf4j
public class TransactionDeduplicator {

    /** Maximum allowed time difference between two transactions to be considered duplicates. */
    private static final long DUPLICATE_THRESHOLD_SECONDS = 2;

    /**
     * Partitions transactions into unique and duplicate groups.
     *
     * <p>The first occurrence of a transaction is always kept as unique.
     * Subsequent duplicates are collected for FAILED persistence.</p>
     *
     * @param transactions validated transactions (should not contain nulls)
     * @return a {@link DeduplicationResult} with unique and duplicate lists
     */
    public DeduplicationResult deduplicate(List<TransactionRequestDTO> transactions) {

        List<TransactionRequestDTO> uniqueTransactions = new ArrayList<>();
        List<TransactionRequestDTO> duplicateTransactions = new ArrayList<>();

        transactions.stream()
                .forEach(dto -> {
                    boolean isDuplicate = uniqueTransactions.stream()
                            .anyMatch(existing -> isDuplicate(existing, dto));

                    if (isDuplicate) {
                        duplicateTransactions.add(dto);
                    } else {
                        uniqueTransactions.add(dto);
                    }
                });

        log.info("Deduplication complete — Unique: {}, Duplicates: {}",
                uniqueTransactions.size(), duplicateTransactions.size());

        return DeduplicationResult.builder()
                .uniqueTransactions(uniqueTransactions)
                .duplicateTransactions(duplicateTransactions)
                .build();
    }

    /**
     * Checks if two transactions are duplicates based on the assignment's criteria.
     *
     * @param existing the previously seen transaction
     * @param candidate the new transaction to check against
     * @return true if candidate is a duplicate of existing
     */
    private boolean isDuplicate(TransactionRequestDTO existing, TransactionRequestDTO candidate) {
        // All four conditions must be true for a duplicate
        return existing.getSourceAccountId().equals(candidate.getSourceAccountId())
                && existing.getTargetAccountId().equals(candidate.getTargetAccountId())
                && existing.getAmount().compareTo(candidate.getAmount()) == 0
                && isWithinThreshold(existing, candidate);
    }

    /**
     * Checks if two transactions' timestamps are within the 2-second threshold.
     *
     * <p>Uses {@code Duration.between().abs()} for absolute difference,
     * so order doesn't matter (A vs B or B vs A).</p>
     *
     * @param existing the previously seen transaction
     * @param candidate the new transaction to check
     * @return true if timestamps are within 2 seconds
     */
    private boolean isWithinThreshold(TransactionRequestDTO existing, TransactionRequestDTO candidate) {
        if (existing.getTimestamp() == null || candidate.getTimestamp() == null) {
            return false;
        }
        long secondsDiff = Math.abs(
                Duration.between(existing.getTimestamp(), candidate.getTimestamp()).getSeconds()
        );
        return secondsDiff <= DUPLICATE_THRESHOLD_SECONDS;
    }
}
