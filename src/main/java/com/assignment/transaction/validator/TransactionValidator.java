package com.assignment.transaction.validator;

import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates transaction DTOs using Java Streams before persistence.
 *
 * <h3>Validation Rules</h3>
 * <ol>
 *   <li>Amount must not be null</li>
 *   <li>Amount must be positive (greater than zero)</li>
 *   <li>Source account ID must not be null</li>
 *   <li>Target account ID must not be null</li>
 *   <li>Source and target must be different accounts</li>
 *   <li>Timestamp must not be null</li>
 * </ol>
 *
 * <h3>Why Stream-based validation instead of Bean Validation?</h3>
 * <p>The assignment explicitly requires "Validation Using Stream API".
 * Bean Validation ({@code @Valid}) on the DTO provides a first-line defense
 * at the controller level. This Stream-based validator is the second layer
 * that runs during async processing, catching edge cases and providing
 * batch-level filtering with detailed per-transaction failure reasons.</p>
 *
 * <h3>Why not throw exceptions for invalid records?</h3>
 * <p>In a batch of 10,000 transactions, we don't want one bad record to
 * abort the entire batch. Instead, we partition the batch into valid and
 * invalid, persist the invalid ones as FAILED (for audit), and continue
 * processing the valid ones.</p>
 */
@Component
@Slf4j
public class TransactionValidator {

    /**
     * Validates a list of transaction DTOs using Java Streams.
     *
     * <p>Each DTO is checked against all validation rules. If any rule fails,
     * the DTO is added to the invalid map with the first failure reason found.
     * Valid DTOs are collected into a separate list.</p>
     *
     * @param transactions the list of DTOs to validate
     * @return a {@link ValidationResult} containing valid and invalid partitions
     */
    public ValidationResult validate(List<TransactionRequestDTO> transactions) {

        List<TransactionRequestDTO> validTransactions = new ArrayList<>();
        Map<TransactionRequestDTO, String> invalidTransactions = new LinkedHashMap<>();

        transactions.stream()
                .forEach(dto -> {
                    String failureReason = findFailureReason(dto);
                    if (failureReason != null) {
                        invalidTransactions.put(dto, failureReason);
                    } else {
                        validTransactions.add(dto);
                    }
                });

        log.info("Validation complete — Valid: {}, Invalid: {}",
                validTransactions.size(), invalidTransactions.size());

        return ValidationResult.builder()
                .validTransactions(validTransactions)
                .invalidTransactions(invalidTransactions)
                .build();
    }

    /**
     * Checks a single DTO against all validation rules and returns
     * the first failure reason found, or null if valid.
     *
     * <p>Rules are checked in order of severity — null checks first,
     * then business rules (positive amount, different accounts).</p>
     *
     * @param dto the transaction to validate
     * @return failure reason string, or null if the DTO is valid
     */
    private String findFailureReason(TransactionRequestDTO dto) {
        if (dto == null) {
            return "Transaction is null";
        }
        if (dto.getSourceAccountId() == null) {
            return "Source account ID is null";
        }
        if (dto.getTargetAccountId() == null) {
            return "Target account ID is null";
        }
        if (dto.getAmount() == null) {
            return "Amount is null";
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Negative or zero amount";
        }
        if (dto.getSourceAccountId().equals(dto.getTargetAccountId())) {
            return "Source and target accounts are the same";
        }
        if (dto.getTimestamp() == null) {
            return "Timestamp is null";
        }
        return null;
    }
}
