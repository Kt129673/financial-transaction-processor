package com.assignment.transaction.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API response returned immediately (HTTP 202 Accepted) when a batch
 * of transactions is submitted for asynchronous processing.
 *
 * <h3>Why 202 and not 200?</h3>
 * <p>HTTP 202 means "I accepted your request, but haven't finished processing it."
 * This is the correct status for async operations — the client knows the batch was
 * received and can use the {@code batchId} to query results later.</p>
 *
 * <h3>Example Response</h3>
 * <pre>{@code
 * {
 *   "batchId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
 *   "status": "Processing Started"
 * }
 * }</pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchTransactionResponse {

    /**
     * UUID assigned to this batch. Links all transactions in this request.
     * Clients can use this to track or query batch processing results.
     */
    private String batchId;

    /**
     * Human-readable status message.
     * Always "Processing Started" for the initial 202 response.
     */
    private String status;
}
