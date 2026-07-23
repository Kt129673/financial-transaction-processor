package com.assignment.transaction.async;

import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.entity.Account;
import com.assignment.transaction.entity.Transaction;
import com.assignment.transaction.mapper.TransactionMapper;
import com.assignment.transaction.model.DeduplicationResult;
import com.assignment.transaction.model.TransactionStatus;
import com.assignment.transaction.model.ValidationResult;
import com.assignment.transaction.repository.AccountRepository;
import com.assignment.transaction.repository.TransactionRepository;
import com.assignment.transaction.validator.TransactionDeduplicator;
import com.assignment.transaction.validator.TransactionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Asynchronous processor for financial transaction batches.
 *
 * <h3>Why a separate class from TransactionServiceImpl?</h3>
 * <p>Spring's {@code @Async} works via AOP proxying. If a bean calls its own
 * {@code @Async} method, the call bypasses the proxy and executes synchronously.
 * By placing the async method in a separate Spring bean, the proxy is always
 * in the call chain, guaranteeing async execution.</p>
 *
 * <h3>Processing Pipeline</h3>
 * <ol>
 *   <li><b>Validate</b>: Stream-filter invalid records → persist as FAILED</li>
 *   <li><b>Deduplicate</b>: Detect duplicates → persist as FAILED</li>
 *   <li><b>Group</b>: {@code Collectors.groupingBy(sourceAccountId)}</li>
 *   <li><b>Process</b>: For each group, lock accounts and transfer funds</li>
 * </ol>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionProcessorAsync {

    private final TransactionValidator validator;
    private final TransactionDeduplicator deduplicator;
    private final TransactionMapper mapper;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    /**
     * Processes a batch of transactions asynchronously.
     *
     * <p>Runs on the {@code transactionExecutor} thread pool. The calling
     * method returns immediately — this method executes in the background.</p>
     *
     * @param transactions the list of transaction DTOs to process
     * @param batchId      the UUID identifying this batch
     */
    @Async("transactionExecutor")
    public void processBatch(List<TransactionRequestDTO> transactions, String batchId) {

        long startTime = System.currentTimeMillis();
        log.info("========== Batch Started: {} | Total Transactions: {} ==========",
                batchId, transactions.size());

        try {
            // Step 1: Validate — filter out invalid records using Streams
            ValidationResult validationResult = validator.validate(transactions);
            persistFailedValidations(validationResult, batchId);

            // Step 2: Deduplicate — detect and remove duplicates
            DeduplicationResult deduplicationResult =
                    deduplicator.deduplicate(validationResult.getValidTransactions());
            persistDuplicates(deduplicationResult, batchId);

            // Step 3: Group by sourceAccountId using Collectors.groupingBy
            List<TransactionRequestDTO> uniqueTransactions =
                    deduplicationResult.getUniqueTransactions();

            Map<Long, List<TransactionRequestDTO>> groupedBySource = uniqueTransactions.stream()
                    .collect(Collectors.groupingBy(TransactionRequestDTO::getSourceAccountId));

            // Log grouped transaction counts
            log.info("Grouped transactions by source account — {} groups:", groupedBySource.size());
            groupedBySource.forEach((sourceId, txns) ->
                    log.debug("  Account {}: {} transactions", sourceId, txns.size()));

            // Step 4: Process each transaction with account locking
            int successCount = 0;
            int failedCount = 0;

            for (Map.Entry<Long, List<TransactionRequestDTO>> entry : groupedBySource.entrySet()) {
                for (TransactionRequestDTO dto : entry.getValue()) {
                    boolean success = processTransaction(dto, batchId);
                    if (success) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("========== Batch Completed: {} ==========", batchId);
            log.info("Results — Success: {}, Failed (validation): {}, Failed (duplicate): {}, Failed (processing): {}",
                    successCount,
                    validationResult.getInvalidTransactions().size(),
                    deduplicationResult.getDuplicateTransactions().size(),
                    failedCount);
            log.info("Execution Time: {} ms", executionTime);

        } catch (Exception e) {
            log.error("Unexpected error processing batch {}: {}", batchId, e.getMessage(), e);
        }
    }

    /**
     * Persists invalid transactions as FAILED with their validation failure reasons.
     */
    private void persistFailedValidations(ValidationResult result, String batchId) {
        if (result.getInvalidTransactions().isEmpty()) {
            return;
        }

        List<Transaction> failedEntities = result.getInvalidTransactions().entrySet().stream()
                .map(entry -> mapper.toFailedEntity(entry.getKey(), batchId, entry.getValue()))
                .toList();

        transactionRepository.saveAll(failedEntities);
        log.warn("Persisted {} failed validation records for batch {}",
                failedEntities.size(), batchId);
    }

    /**
     * Persists duplicate transactions as FAILED with reason "Duplicate Transaction".
     */
    private void persistDuplicates(DeduplicationResult result, String batchId) {
        if (result.getDuplicateTransactions().isEmpty()) {
            return;
        }

        List<Transaction> duplicateEntities = result.getDuplicateTransactions().stream()
                .map(dto -> mapper.toFailedEntity(dto, batchId, "Duplicate Transaction"))
                .toList();

        transactionRepository.saveAll(duplicateEntities);
        log.warn("Persisted {} duplicate records for batch {}", duplicateEntities.size(), batchId);
    }

    /**
     * Processes a single transaction: locks accounts, validates balance, transfers funds.
     *
     * <h3>Deadlock Prevention</h3>
     * <p>Always acquires locks in ascending order of account ID. If source=5 and
     * target=3, we lock account 3 first, then account 5. This consistent ordering
     * prevents circular wait (the necessary condition for deadlock).</p>
     *
     * @param dto     the transaction to process
     * @param batchId the batch ID for tracking
     * @return true if the transaction succeeded, false if it failed
     */
    public boolean processTransaction(TransactionRequestDTO dto, String batchId) {
        try {
            return transactionTemplate.execute(status -> {
                // Determine lock order — always lock lower ID first to prevent deadlocks
                Long firstLockId = Math.min(dto.getSourceAccountId(), dto.getTargetAccountId());
                Long secondLockId = Math.max(dto.getSourceAccountId(), dto.getTargetAccountId());

                // Acquire pessimistic locks in consistent order
                Optional<Account> firstAccountOpt = accountRepository.findByIdWithLock(firstLockId);
                Optional<Account> secondAccountOpt = accountRepository.findByIdWithLock(secondLockId);

                // Resolve which is source and which is target
                Optional<Account> sourceOpt = dto.getSourceAccountId().equals(firstLockId)
                        ? firstAccountOpt : secondAccountOpt;
                Optional<Account> targetOpt = dto.getTargetAccountId().equals(firstLockId)
                        ? firstAccountOpt : secondAccountOpt;

                // Check source account exists
                if (sourceOpt.isEmpty()) {
                    persistFailedTransaction(dto, batchId,
                            "Source account not found: " + dto.getSourceAccountId());
                    return false;
                }

                // Check target account exists
                if (targetOpt.isEmpty()) {
                    persistFailedTransaction(dto, batchId,
                            "Target account not found: " + dto.getTargetAccountId());
                    return false;
                }

                Account source = sourceOpt.get();
                Account target = targetOpt.get();

                // Check sufficient balance — prevent overdraft
                if (source.getBalance().compareTo(dto.getAmount()) < 0) {
                    persistFailedTransaction(dto, batchId,
                            String.format("Insufficient balance in account %d: current=%.4f, requested=%.4f",
                                    source.getId(), source.getBalance(), dto.getAmount()));
                    return false;
                }

                // Transfer funds — debit source, credit target
                source.setBalance(source.getBalance().subtract(dto.getAmount()));
                target.setBalance(target.getBalance().add(dto.getAmount()));

                accountRepository.save(source);
                accountRepository.save(target);

                // Persist successful transaction
                Transaction successTransaction = mapper.toEntity(dto, batchId);
                successTransaction.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(successTransaction);

                return true;
            });
        } catch (Exception e) {
            log.error("Error processing transaction in batch {}: {}", batchId, e.getMessage());
            persistFailedTransaction(dto, batchId, "Processing error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates and persists a FAILED transaction record.
     */
    private void persistFailedTransaction(TransactionRequestDTO dto, String batchId, String reason) {
        Transaction failedTransaction = mapper.toFailedEntity(dto, batchId, reason);
        transactionRepository.save(failedTransaction);
    }
}
