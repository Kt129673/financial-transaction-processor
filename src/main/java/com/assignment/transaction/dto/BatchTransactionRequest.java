package com.assignment.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Wrapper DTO for the batch transaction processing endpoint.
 *
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li><b>@Valid on the list</b>: Triggers cascading validation — Spring will validate
 *       each {@link TransactionRequestDTO} in the list individually. Without {@code @Valid},
 *       only the list itself is checked (not empty, size), but the contents are not validated.</li>
 *   <li><b>@Size(max = 10000)</b>: The assignment specifies "up to 10,000 transactions".
 *       This guard prevents memory exhaustion from oversized payloads before any business
 *       logic runs.</li>
 *   <li><b>@NotEmpty vs @NotNull</b>: {@code @NotEmpty} rejects both null and empty lists.
 *       Submitting an empty batch is meaningless and should fail fast with a clear error.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchTransactionRequest {

    /**
     * List of transactions to process in this batch.
     * Minimum 1, maximum 10,000 transactions per request.
     * Each transaction is individually validated via {@code @Valid}.
     */
    @NotEmpty(message = "Transaction list must not be empty")
    @Size(max = 10000, message = "Maximum 10,000 transactions allowed per batch")
    @Valid
    private List<TransactionRequestDTO> transactions;
}
