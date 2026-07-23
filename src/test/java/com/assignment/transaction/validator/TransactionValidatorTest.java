package com.assignment.transaction.validator;

import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.model.ValidationResult;
import com.assignment.transaction.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TransactionValidator}.
 *
 * <p>No Spring context needed — the validator is a plain POJO with no
 * dependencies. Pure JUnit 5 + AssertJ. Fastest possible test execution.</p>
 */
@DisplayName("TransactionValidator Tests")
class TransactionValidatorTest {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
    }

    @Test
    @DisplayName("Should pass valid transaction")
    void shouldPassValidTransaction() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDefaultValidDTO());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).hasSize(1);
        assertThat(result.getInvalidTransactions()).isEmpty();
    }

    @Test
    @DisplayName("Should reject null source account ID")
    void shouldRejectNullSourceAccountId() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithNullSource());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).hasSize(1);
        assertThat(result.getInvalidTransactions().values())
                .anyMatch(reason -> reason.contains("Source account ID is null"));
    }

    @Test
    @DisplayName("Should reject null target account ID")
    void shouldRejectNullTargetAccountId() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithNullTarget());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).hasSize(1);
        assertThat(result.getInvalidTransactions().values())
                .anyMatch(reason -> reason.contains("Target account ID is null"));
    }

    @Test
    @DisplayName("Should reject null amount")
    void shouldRejectNullAmount() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithNullAmount());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).hasSize(1);
        assertThat(result.getInvalidTransactions().values())
                .anyMatch(reason -> reason.contains("Amount is null"));
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithNegativeAmount());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).hasSize(1);
        assertThat(result.getInvalidTransactions().values())
                .anyMatch(reason -> reason.contains("Negative or zero amount"));
    }

    @Test
    @DisplayName("Should reject zero amount")
    void shouldRejectZeroAmount() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithZeroAmount());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject same source and target")
    void shouldRejectSameSourceAndTarget() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithSameSourceAndTarget());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions().values())
                .anyMatch(reason -> reason.contains("Source and target accounts are the same"));
    }

    @Test
    @DisplayName("Should reject null timestamp")
    void shouldRejectNullTimestamp() {
        // Arrange
        List<TransactionRequestDTO> transactions = List.of(TestDataGenerator.createDTOWithNullTimestamp());

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Should partition mixed valid and invalid transactions")
    void shouldPartitionMixedTransactions() {
        // Arrange — 2 valid, 3 invalid
        List<TransactionRequestDTO> transactions = List.of(
                TestDataGenerator.createDefaultValidDTO(),
                TestDataGenerator.createDTOWithNullSource(),
                TestDataGenerator.createDTOWithNegativeAmount(),
                TestDataGenerator.createValidDTO(3L, 4L, new java.math.BigDecimal("200.00"),
                        java.time.LocalDateTime.now()),
                TestDataGenerator.createDTOWithNullAmount()
        );

        // Act
        ValidationResult result = validator.validate(transactions);

        // Assert
        assertThat(result.getValidTransactions()).hasSize(2);
        assertThat(result.getInvalidTransactions()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle empty transaction list")
    void shouldHandleEmptyList() {
        // Act
        ValidationResult result = validator.validate(List.of());

        // Assert
        assertThat(result.getValidTransactions()).isEmpty();
        assertThat(result.getInvalidTransactions()).isEmpty();
    }
}
