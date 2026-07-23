package com.assignment.transaction.repository;

import com.assignment.transaction.entity.Transaction;
import com.assignment.transaction.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Transaction} entities.
 *
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li><b>Spring Data query derivation</b>: Methods like {@code findByBatchId}
 *       are automatically implemented by Spring Data from the method name —
 *       no {@code @Query} annotation or JPQL needed. Spring parses
 *       "findBy" + "BatchId" and generates
 *       {@code SELECT * FROM transaction WHERE batch_id = ?}.</li>
 *   <li><b>No custom save methods</b>: The inherited {@code saveAll(List)}
 *       from {@code JpaRepository} handles bulk persistence. Hibernate batches
 *       the INSERTs internally when configured properly.</li>
 *   <li><b>No pagination on findByBatchId</b>: A single batch contains at most
 *       10,000 transactions (enforced by the DTO's {@code @Size} annotation).
 *       This is well within memory limits, so pagination adds unnecessary
 *       complexity here.</li>
 * </ul>
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Retrieves all transactions belonging to a specific processing batch.
     *
     * <p>Uses the {@code idx_transaction_batch_id} index defined on the
     * Transaction entity for efficient lookup.</p>
     *
     * @param batchId the UUID of the batch
     * @return all transactions in the batch (valid, failed, and pending)
     */
    List<Transaction> findByBatchId(String batchId);

    /**
     * Retrieves all transactions with a given processing status.
     *
     * <p>Useful for monitoring: find all FAILED transactions for error
     * reporting, or all PENDING transactions to detect stuck batches.</p>
     *
     * @param status the transaction status to filter by
     * @return all transactions matching the status
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Retrieves all transactions from a batch with a specific status.
     *
     * <p>Combines batch and status filters — for example, finding all
     * FAILED transactions in a specific batch for error reporting.</p>
     *
     * @param batchId the UUID of the batch
     * @param status  the transaction status to filter by
     * @return matching transactions
     */
    List<Transaction> findByBatchIdAndStatus(String batchId, TransactionStatus status);
}
