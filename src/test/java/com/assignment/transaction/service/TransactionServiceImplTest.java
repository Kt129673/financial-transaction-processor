package com.assignment.transaction.service;

import com.assignment.transaction.async.TransactionProcessorAsync;
import com.assignment.transaction.dto.BatchTransactionRequest;
import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.response.BatchTransactionResponse;
import com.assignment.transaction.service.impl.TransactionServiceImpl;
import com.assignment.transaction.util.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link TransactionServiceImpl}.
 *
 * <h3>Why @ExtendWith(MockitoExtension) instead of @SpringBootTest?</h3>
 * <p>The service layer should be testable without Spring context. Using
 * Mockito mocks for dependencies makes tests run in milliseconds and
 * isolates the service logic from infrastructure concerns.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl Tests")
class TransactionServiceImplTest {

    @Mock
    private TransactionProcessorAsync processorAsync;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    @DisplayName("Should return batch ID and 'Processing Started' status")
    void shouldReturnBatchResponse() {
        // Arrange
        List<TransactionRequestDTO> dtos = List.of(TestDataGenerator.createDefaultValidDTO());
        BatchTransactionRequest request = TestDataGenerator.createBatchRequest(dtos);

        // Act
        BatchTransactionResponse response = transactionService.processTransactions(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBatchId()).isNotNull().isNotEmpty();
        assertThat(response.getStatus()).isEqualTo("Processing Started");
    }

    @Test
    @DisplayName("Should generate a valid UUID as batch ID")
    void shouldGenerateValidUUID() {
        // Arrange
        List<TransactionRequestDTO> dtos = List.of(TestDataGenerator.createDefaultValidDTO());
        BatchTransactionRequest request = TestDataGenerator.createBatchRequest(dtos);

        // Act
        BatchTransactionResponse response = transactionService.processTransactions(request);

        // Assert — UUID format: 8-4-4-4-12 hex characters
        assertThat(response.getBatchId()).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should delegate to async processor with correct parameters")
    void shouldDelegateToAsyncProcessor() {
        // Arrange
        List<TransactionRequestDTO> dtos = List.of(TestDataGenerator.createDefaultValidDTO());
        BatchTransactionRequest request = TestDataGenerator.createBatchRequest(dtos);

        // Act
        transactionService.processTransactions(request);

        // Assert — verify the async processor was called with the correct arguments
        verify(processorAsync).processBatch(anyList(), anyString());
    }

    @Test
    @DisplayName("Should generate unique batch IDs for different requests")
    void shouldGenerateUniqueBatchIds() {
        // Arrange
        List<TransactionRequestDTO> dtos = List.of(TestDataGenerator.createDefaultValidDTO());
        BatchTransactionRequest request1 = TestDataGenerator.createBatchRequest(dtos);
        BatchTransactionRequest request2 = TestDataGenerator.createBatchRequest(dtos);

        // Act
        BatchTransactionResponse response1 = transactionService.processTransactions(request1);
        BatchTransactionResponse response2 = transactionService.processTransactions(request2);

        // Assert — each call generates a unique UUID
        assertThat(response1.getBatchId()).isNotEqualTo(response2.getBatchId());
    }
}
