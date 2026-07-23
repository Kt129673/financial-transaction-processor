package com.assignment.transaction.validator;

import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.model.DeduplicationResult;
import com.assignment.transaction.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TransactionDeduplicator}.
 *
 * <p>Tests the duplicate detection algorithm with various edge cases
 * around the 2-second timestamp threshold.</p>
 */
@DisplayName("TransactionDeduplicator Tests")
class TransactionDeduplicatorTest {

    private TransactionDeduplicator deduplicator;

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 7, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        deduplicator = new TransactionDeduplicator();
    }

    @Test
    @DisplayName("Should keep all unique transactions")
    void shouldKeepAllUniqueTransactions() {
        // Arrange — different amounts, so not duplicates
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("200.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(3L, 4L, new BigDecimal("100.00"), BASE_TIME)
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(3);
        assertThat(result.getDuplicateTransactions()).isEmpty();
    }

    @Test
    @DisplayName("Should detect duplicate with same source, target, amount, and timestamp within 2 seconds")
    void shouldDetectDuplicate() {
        // Arrange — exact same data, 1 second apart
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME.plusSeconds(1))
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(1);
        assertThat(result.getDuplicateTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Should detect duplicate with exact same timestamp")
    void shouldDetectDuplicateWithExactTimestamp() {
        // Arrange — identical timestamps
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("500.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("500.00"), BASE_TIME)
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(1);
        assertThat(result.getDuplicateTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Should detect duplicate at exactly 2-second boundary")
    void shouldDetectDuplicateAtBoundary() {
        // Arrange — exactly 2 seconds apart (inclusive threshold)
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME.plusSeconds(2))
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert — 2 seconds is within threshold (<=2)
        assertThat(result.getUniqueTransactions()).hasSize(1);
        assertThat(result.getDuplicateTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Should NOT detect duplicate when timestamp exceeds 2 seconds")
    void shouldNotDetectDuplicateBeyondThreshold() {
        // Arrange — 3 seconds apart (beyond threshold)
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME.plusSeconds(3))
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert — both are unique
        assertThat(result.getUniqueTransactions()).hasSize(2);
        assertThat(result.getDuplicateTransactions()).isEmpty();
    }

    @Test
    @DisplayName("Should NOT detect duplicate when amounts differ")
    void shouldNotDetectDuplicateWithDifferentAmounts() {
        // Arrange — same everything except amount
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.01"), BASE_TIME)
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(2);
        assertThat(result.getDuplicateTransactions()).isEmpty();
    }

    @Test
    @DisplayName("Should NOT detect duplicate when source account differs")
    void shouldNotDetectDuplicateWithDifferentSource() {
        // Arrange — different source
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(3L, 2L, new BigDecimal("100.00"), BASE_TIME)
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(2);
        assertThat(result.getDuplicateTransactions()).isEmpty();
    }

    @Test
    @DisplayName("Should NOT detect duplicate when target account differs")
    void shouldNotDetectDuplicateWithDifferentTarget() {
        // Arrange — different target
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 3L, new BigDecimal("100.00"), BASE_TIME)
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(2);
        assertThat(result.getDuplicateTransactions()).isEmpty();
    }

    @Test
    @DisplayName("Should detect multiple duplicates in a batch")
    void shouldDetectMultipleDuplicates() {
        // Arrange — 3 identical transactions (first is unique, other 2 are duplicates)
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME.plusSeconds(1)),
                TestDataGenerator.createValidDTO(1L, 2L, new BigDecimal("100.00"), BASE_TIME.plusSeconds(2))
        );

        // Act
        DeduplicationResult result = deduplicator.deduplicate(transactions);

        // Assert
        assertThat(result.getUniqueTransactions()).hasSize(1);
        assertThat(result.getDuplicateTransactions()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle empty list")
    void shouldHandleEmptyList() {
        // Act
        DeduplicationResult result = deduplicator.deduplicate(List.of());

        // Assert
        assertThat(result.getUniqueTransactions()).isEmpty();
        assertThat(result.getDuplicateTransactions()).isEmpty();
    }
}
