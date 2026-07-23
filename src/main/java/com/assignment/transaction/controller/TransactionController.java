package com.assignment.transaction.controller;

import com.assignment.transaction.dto.BatchTransactionRequest;
import com.assignment.transaction.response.BatchTransactionResponse;
import com.assignment.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for financial transaction processing.
 *
 * <h3>API Design</h3>
 * <ul>
 *   <li><b>Versioned URL</b>: {@code /api/v1/} prefix allows future breaking changes
 *       to be released as {@code /api/v2/} without affecting existing clients.</li>
 *   <li><b>HTTP 202 Accepted</b>: The correct status for async operations. 200 OK
 *       would imply processing is complete, which is misleading when we're just
 *       queuing work for the thread pool.</li>
 *   <li><b>@Valid</b>: Triggers Bean Validation on the request body before the
 *       method executes. If any constraint fails, Spring throws
 *       {@code MethodArgumentNotValidException}, which our
 *       {@code GlobalExceptionHandler} catches and formats.</li>
 * </ul>
 *
 * <h3>Why the controller is thin</h3>
 * <p>The controller's only job is HTTP-level concerns: route mapping, request
 * deserialization, validation triggering, and response status. All business
 * logic is delegated to the service layer. This follows the "Thin Controller,
 * Fat Service" pattern — controllers should never contain business rules.</p>
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Accepts a batch of financial transactions for asynchronous processing.
     *
     * <h3>Request Flow</h3>
     * <ol>
     *   <li>Spring deserializes JSON → {@link BatchTransactionRequest}</li>
     *   <li>{@code @Valid} triggers Bean Validation on the DTO</li>
     *   <li>Service generates a batch UUID and queues async processing</li>
     *   <li>Returns HTTP 202 with the batch ID immediately</li>
     * </ol>
     *
     * <h3>Example Request</h3>
     * <pre>{@code
     * POST /api/v1/transactions/process
     * Content-Type: application/json
     *
     * {
     *   "transactions": [
     *     {
     *       "sourceAccountId": 1,
     *       "targetAccountId": 2,
     *       "amount": 500,
     *       "timestamp": "2026-07-20T10:00:00"
     *     }
     *   ]
     * }
     * }</pre>
     *
     * <h3>Example Response (HTTP 202)</h3>
     * <pre>{@code
     * {
     *   "batchId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     *   "status": "Processing Started"
     * }
     * }</pre>
     *
     * @param request the batch of transactions (validated via @Valid)
     * @return HTTP 202 Accepted with the batch ID
     */
    @PostMapping("/process")
    public ResponseEntity<BatchTransactionResponse> processTransactions(
            @Valid @RequestBody BatchTransactionRequest request) {

        log.info("Received transaction processing request with {} transactions",
                request.getTransactions().size());

        BatchTransactionResponse response = transactionService.processTransactions(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
