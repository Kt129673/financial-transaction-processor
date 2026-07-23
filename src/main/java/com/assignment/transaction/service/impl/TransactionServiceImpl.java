package com.assignment.transaction.service.impl;

import com.assignment.transaction.async.TransactionProcessorAsync;
import com.assignment.transaction.dto.BatchTransactionRequest;
import com.assignment.transaction.response.BatchTransactionResponse;
import com.assignment.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of {@link TransactionService}.
 *
 * <h3>Responsibilities</h3>
 * <p>This class is intentionally thin — it acts as an orchestrator:</p>
 * <ol>
 *   <li>Generates a unique batch ID (UUID)</li>
 *   <li>Delegates async processing to {@link TransactionProcessorAsync}</li>
 *   <li>Returns immediately with a 202-style response</li>
 * </ol>
 *
 * <h3>Why is this class so small?</h3>
 * <p>Single Responsibility Principle: this service is responsible for
 * "accepting a batch request and handing it off." The actual processing
 * logic lives in {@code TransactionProcessorAsync}. This separation
 * also solves Spring's @Async self-invocation problem — if the async
 * method were in this same class, calling it internally would bypass
 * the AOP proxy and execute synchronously.</p>
 *
 * <h3>Constructor Injection</h3>
 * <p>{@code @RequiredArgsConstructor} generates a constructor with all
 * {@code final} fields — satisfying the "No Field Injection" requirement.
 * Spring injects the dependency via this constructor automatically.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionProcessorAsync processorAsync;

    /**
     * {@inheritDoc}
     *
     * <p>This method is synchronous and returns immediately. The batch ID
     * is generated here (not in the async method) so we can return it to
     * the client in the HTTP response before processing begins.</p>
     */
    @Override
    public BatchTransactionResponse processTransactions(BatchTransactionRequest request) {

        // Generate a unique batch identifier
        String batchId = UUID.randomUUID().toString();

        log.info("Received batch request — Batch ID: {}, Transaction Count: {}",
                batchId, request.getTransactions().size());

        // Fire-and-forget: processing happens asynchronously on the transactionExecutor pool.
        // The @Async proxy ensures this call returns immediately.
        processorAsync.processBatch(request.getTransactions(), batchId);

        // Return the batch ID so the client can track processing results
        return BatchTransactionResponse.builder()
                .batchId(batchId)
                .status("Processing Started")
                .build();
    }
}
