package com.assignment.transaction.repository;

import com.assignment.transaction.entity.Transaction;
import com.assignment.transaction.model.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TransactionRepository}.
 *
 * <p>Tests the derived query methods (findByBatchId, findByStatus, etc.)
 * that Spring Data generates from method names. Verifies correct SQL
 * generation and result filtering.</p>
 */
@DataJpaTest
@DisplayName("TransactionRepository Tests")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String BATCH_ID = "test-batch-001";
    private static final String BATCH_ID_2 = "test-batch-002";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();

        // Seed test transactions across two batches with different statuses
        transactionRepository.saveAll(List.of(
                createTransaction(1L, 2L, "100.00", TransactionStatus.SUCCESS, BATCH_ID, null),
                createTransaction(3L, 4L, "200.00", TransactionStatus.SUCCESS, BATCH_ID, null),
                createTransaction(1L, 3L, "300.00", TransactionStatus.FAILED, BATCH_ID, "Insufficient balance"),
                createTransaction(2L, 5L, "400.00", TransactionStatus.PENDING, BATCH_ID, null),
                createTransaction(1L, 2L, "500.00", TransactionStatus.SUCCESS, BATCH_ID_2, null)
        ));
    }

    @Test
    @DisplayName("Should find all transactions by batch ID")
    void shouldFindByBatchId() {
        // Act
        List<Transaction> transactions = transactionRepository.findByBatchId(BATCH_ID);

        // Assert — 4 transactions in BATCH_ID
        assertThat(transactions).hasSize(4);
        assertThat(transactions).allMatch(t -> t.getBatchId().equals(BATCH_ID));
    }

    @Test
    @DisplayName("Should return empty list for non-existent batch ID")
    void shouldReturnEmptyForNonExistentBatchId() {
        // Act
        List<Transaction> transactions = transactionRepository.findByBatchId("non-existent");

        // Assert
        assertThat(transactions).isEmpty();
    }

    @Test
    @DisplayName("Should find transactions by status")
    void shouldFindByStatus() {
        // Act
        List<Transaction> successTransactions = transactionRepository.findByStatus(TransactionStatus.SUCCESS);
        List<Transaction> failedTransactions = transactionRepository.findByStatus(TransactionStatus.FAILED);
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        // Assert
        assertThat(successTransactions).hasSize(3); // 2 in BATCH_ID + 1 in BATCH_ID_2
        assertThat(failedTransactions).hasSize(1);
        assertThat(pendingTransactions).hasSize(1);
    }

    @Test
    @DisplayName("Should find transactions by batch ID and status")
    void shouldFindByBatchIdAndStatus() {
        // Act
        List<Transaction> result = transactionRepository.findByBatchIdAndStatus(
                BATCH_ID, TransactionStatus.SUCCESS);

        // Assert — only 2 SUCCESS in BATCH_ID (not the one in BATCH_ID_2)
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t ->
                t.getBatchId().equals(BATCH_ID) && t.getStatus() == TransactionStatus.SUCCESS);
    }

    @Test
    @DisplayName("Should save all transactions in bulk")
    void shouldSaveAllTransactions() {
        // Arrange
        transactionRepository.deleteAll();
        List<Transaction> batch = List.of(
                createTransaction(1L, 2L, "10.00", TransactionStatus.PENDING, "bulk-batch", null),
                createTransaction(3L, 4L, "20.00", TransactionStatus.PENDING, "bulk-batch", null),
                createTransaction(5L, 6L, "30.00", TransactionStatus.PENDING, "bulk-batch", null)
        );

        // Act
        List<Transaction> saved = transactionRepository.saveAll(batch);

        // Assert
        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(t -> t.getId() != null); // All IDs auto-generated
    }

    @Test
    @DisplayName("Should persist failure reason for failed transactions")
    void shouldPersistFailureReason() {
        // Act
        List<Transaction> failed = transactionRepository.findByStatus(TransactionStatus.FAILED);

        // Assert
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getFailureReason()).isEqualTo("Insufficient balance");
    }

    // ======================== Helper Methods ========================

    private Transaction createTransaction(Long source, Long target, String amount,
                                          TransactionStatus status, String batchId, String failureReason) {
        return Transaction.builder()
                .sourceAccountId(source)
                .targetAccountId(target)
                .amount(new BigDecimal(amount))
                .status(status)
                .timestamp(LocalDateTime.now())
                .batchId(batchId)
                .failureReason(failureReason)
                .build();
    }
}
