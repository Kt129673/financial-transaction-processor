package com.assignment.transaction.util;

import com.assignment.transaction.dto.BatchTransactionRequest;
import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory methods for generating test data.
 *
 * <h3>Usage</h3>
 * <p>Provides consistent, reusable test data across all test classes.
 * Avoids duplicating builder calls in every test method.</p>
 *
 * <h3>Why static factory methods?</h3>
 * <p>Test data generators are stateless utilities. There's no reason
 * to instantiate this class — static methods are the natural fit.
 * Unlike production code, test utilities don't need to be mockable.</p>
 */
public final class TestDataGenerator {

    private static final Random RANDOM = new Random(42); // Fixed seed for reproducible tests

    private TestDataGenerator() {
        // Utility class — prevent instantiation
    }

    // ======================== Single DTO Builders ========================

    /**
     * Creates a valid TransactionRequestDTO with the given parameters.
     */
    public static TransactionRequestDTO createValidDTO(
            Long sourceId, Long targetId, BigDecimal amount, LocalDateTime timestamp) {
        return TransactionRequestDTO.builder()
                .sourceAccountId(sourceId)
                .targetAccountId(targetId)
                .amount(amount)
                .timestamp(timestamp)
                .build();
    }

    /**
     * Creates a valid TransactionRequestDTO with sensible defaults.
     * Source=1, Target=2, Amount=100, Timestamp=now.
     */
    public static TransactionRequestDTO createDefaultValidDTO() {
        return createValidDTO(1L, 2L, new BigDecimal("100.00"), LocalDateTime.now());
    }

    /**
     * Creates a DTO with a null source account ID (invalid).
     */
    public static TransactionRequestDTO createDTOWithNullSource() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(null)
                .targetAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a DTO with a null target account ID (invalid).
     */
    public static TransactionRequestDTO createDTOWithNullTarget() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(1L)
                .targetAccountId(null)
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a DTO with a negative amount (invalid).
     */
    public static TransactionRequestDTO createDTOWithNegativeAmount() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(1L)
                .targetAccountId(2L)
                .amount(new BigDecimal("-50.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a DTO with a zero amount (invalid).
     */
    public static TransactionRequestDTO createDTOWithZeroAmount() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(1L)
                .targetAccountId(2L)
                .amount(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a DTO with a null amount (invalid).
     */
    public static TransactionRequestDTO createDTOWithNullAmount() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(1L)
                .targetAccountId(2L)
                .amount(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a DTO with a null timestamp (invalid).
     */
    public static TransactionRequestDTO createDTOWithNullTimestamp() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(1L)
                .targetAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .timestamp(null)
                .build();
    }

    /**
     * Creates a DTO where source equals target (invalid — self-transfer).
     */
    public static TransactionRequestDTO createDTOWithSameSourceAndTarget() {
        return TransactionRequestDTO.builder()
                .sourceAccountId(1L)
                .targetAccountId(1L)
                .amount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ======================== Account Builders ========================

    /**
     * Creates a test Account entity with the given parameters.
     */
    public static Account createAccount(Long id, String ownerName, BigDecimal balance, String currency) {
        return Account.builder()
                .id(id)
                .ownerName(ownerName)
                .balance(balance)
                .currency(currency)
                .build();
    }

    // ======================== Batch Generators ========================

    /**
     * Generates a batch of N valid transactions using random account pairs.
     *
     * <p>Uses accounts 1-10 (matching data.sql seed data). Each transaction
     * has a small random amount (1-100) to avoid overdraft during testing.</p>
     *
     * @param count number of transactions to generate
     * @return list of valid TransactionRequestDTOs
     */
    public static List<TransactionRequestDTO> generateTransactions(int count) {
        List<TransactionRequestDTO> transactions = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.of(2026, 7, 20, 10, 0, 0);

        for (int i = 0; i < count; i++) {
            long sourceId = (RANDOM.nextInt(10) + 1);
            long targetId;
            do {
                targetId = (RANDOM.nextInt(10) + 1);
            } while (targetId == sourceId); // Ensure source != target

            BigDecimal amount = BigDecimal.valueOf(RANDOM.nextInt(100) + 1);

            transactions.add(TransactionRequestDTO.builder()
                    .sourceAccountId(sourceId)
                    .targetAccountId(targetId)
                    .amount(amount)
                    // Spread timestamps by 10 seconds each to avoid deduplication
                    .timestamp(baseTime.plusSeconds((long) i * 10))
                    .build());
        }

        return transactions;
    }

    /**
     * Generates 1,000 transactions for performance testing.
     */
    public static List<TransactionRequestDTO> generate1000Transactions() {
        return generateTransactions(1000);
    }

    /**
     * Generates 5,000 transactions for performance testing.
     */
    public static List<TransactionRequestDTO> generate5000Transactions() {
        return generateTransactions(5000);
    }

    /**
     * Wraps a list of DTOs into a BatchTransactionRequest.
     */
    public static BatchTransactionRequest createBatchRequest(List<TransactionRequestDTO> transactions) {
        return BatchTransactionRequest.builder()
                .transactions(transactions)
                .build();
    }
}
