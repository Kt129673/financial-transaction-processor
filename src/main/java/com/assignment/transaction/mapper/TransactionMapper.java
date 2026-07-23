package com.assignment.transaction.mapper;

import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.entity.Transaction;
import com.assignment.transaction.model.TransactionStatus;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link TransactionRequestDTO} and {@link Transaction} entity.
 *
 * <h3>Why a dedicated mapper class?</h3>
 * <ul>
 *   <li><b>Single Responsibility</b>: Conversion logic lives in one place. If the DTO
 *       or entity schema changes, only this class needs updating — not every service
 *       method that creates entities.</li>
 *   <li><b>Testability</b>: The mapper is a Spring {@code @Component} — it can be
 *       mocked in service tests to isolate business logic from mapping logic.</li>
 *   <li><b>No MapStruct/ModelMapper dependency</b>: For a project with 2 entities,
 *       a mapping framework is overkill. Manual mapping is explicit, debuggable,
 *       and has zero reflection overhead.</li>
 * </ul>
 *
 * <h3>Why @Component instead of static methods?</h3>
 * <p>Making this a Spring bean allows constructor injection into services (following
 * the assignment's "No Field Injection" rule) and makes it mockable in unit tests.
 * Static utility methods cannot be mocked without PowerMock.</p>
 */
@Component
public class TransactionMapper {

    /**
     * Converts a client-submitted DTO into a Transaction entity ready for persistence.
     *
     * <p>Sets initial state:</p>
     * <ul>
     *   <li>{@code status = PENDING} — all transactions start as pending</li>
     *   <li>{@code batchId} — links this transaction to its processing batch</li>
     *   <li>{@code id = null} — JPA will auto-generate on persist</li>
     *   <li>{@code failureReason = null} — only set if processing fails</li>
     * </ul>
     *
     * @param dto     the incoming transaction request
     * @param batchId the UUID of the batch this transaction belongs to
     * @return a new Transaction entity in PENDING status
     */
    public Transaction toEntity(TransactionRequestDTO dto, String batchId) {
        return Transaction.builder()
                .sourceAccountId(dto.getSourceAccountId())
                .targetAccountId(dto.getTargetAccountId())
                .amount(dto.getAmount())
                .timestamp(dto.getTimestamp())
                .status(TransactionStatus.PENDING)
                .batchId(batchId)
                .build();
    }

    /**
     * Creates a FAILED Transaction entity from a DTO that did not pass validation.
     *
     * <p>Failed transactions are persisted for audit purposes — the business needs
     * a complete record of every transaction attempt, including those that were
     * rejected before processing.</p>
     *
     * @param dto           the invalid transaction request
     * @param batchId       the UUID of the batch
     * @param failureReason human-readable explanation of why validation failed
     * @return a new Transaction entity in FAILED status with the failure reason
     */
    public Transaction toFailedEntity(TransactionRequestDTO dto, String batchId, String failureReason) {
        return Transaction.builder()
                .sourceAccountId(dto.getSourceAccountId())
                .targetAccountId(dto.getTargetAccountId())
                .amount(dto.getAmount())
                .timestamp(dto.getTimestamp())
                .status(TransactionStatus.FAILED)
                .batchId(batchId)
                .failureReason(failureReason)
                .build();
    }
}
