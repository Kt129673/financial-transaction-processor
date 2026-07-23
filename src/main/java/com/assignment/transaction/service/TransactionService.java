package com.assignment.transaction.service;

import com.assignment.transaction.dto.BatchTransactionRequest;
import com.assignment.transaction.response.BatchTransactionResponse;

/**
 * Service interface for financial transaction processing.
 *
 * <h3>Why an interface?</h3>
 * <p>Follows the Dependency Inversion Principle (DIP): the controller
 * depends on this abstraction, not on {@code TransactionServiceImpl}.
 * This allows swapping implementations (e.g., for testing, or switching
 * from async to reactive processing) without modifying the controller.</p>
 */
public interface TransactionService {

    /**
     * Accepts a batch of transactions for asynchronous processing.
     *
     * <p>This method is <b>synchronous</b> — it generates a batch ID,
     * triggers async processing, and returns immediately with HTTP 202.</p>
     *
     * <p>The actual transaction processing (validation, deduplication,
     * account updates) happens asynchronously on the
     * {@code transactionExecutor} thread pool.</p>
     *
     * @param request the batch of transactions to process
     * @return a response containing the generated batch ID and status
     */
    BatchTransactionResponse processTransactions(BatchTransactionRequest request);
}
