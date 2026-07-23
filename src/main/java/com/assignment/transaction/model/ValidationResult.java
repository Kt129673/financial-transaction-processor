package com.assignment.transaction.model;

import com.assignment.transaction.dto.TransactionRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Holds the result of Stream-based transaction validation.
 *
 * <p>Separates a batch of DTOs into valid and invalid groups.
 * Invalid transactions are paired with their failure reason so the
 * service layer can persist them as FAILED with meaningful messages.</p>
 *
 * <h3>Why a dedicated result class instead of returning a Map?</h3>
 * <ul>
 *   <li><b>Type safety</b>: {@code Map<Boolean, List<...>>} from
 *       {@code partitioningBy} requires callers to remember that
 *       {@code true} = valid and {@code false} = invalid. A named class
 *       eliminates this ambiguity.</li>
 *   <li><b>Extensibility</b>: If we later need to add validation metadata
 *       (e.g., total count, error categories), we add fields here without
 *       changing every caller.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@Builder
public class ValidationResult {

    /** Transactions that passed all validation checks and are ready for processing. */
    private final List<TransactionRequestDTO> validTransactions;

    /**
     * Transactions that failed validation, mapped to their failure reason.
     * Key = the invalid DTO, Value = human-readable reason (e.g., "Negative amount").
     */
    private final Map<TransactionRequestDTO, String> invalidTransactions;
}
