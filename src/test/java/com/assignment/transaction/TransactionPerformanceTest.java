package com.assignment.transaction;

import com.assignment.transaction.dto.BatchTransactionRequest;
import com.assignment.transaction.dto.TransactionRequestDTO;
import com.assignment.transaction.response.BatchTransactionResponse;
import com.assignment.transaction.service.TransactionService;
import com.assignment.transaction.util.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the transaction processing pipeline.
 *
 * <h3>Why @SpringBootTest?</h3>
 * <p>These tests require the full application context — database, async executor,
 * validators, and the complete processing pipeline. Unlike unit tests, performance
 * tests measure end-to-end throughput including database I/O.</p>
 *
 * <h3>Note on Async Processing</h3>
 * <p>Since processing is asynchronous, these tests verify that:</p>
 * <ol>
 *   <li>The service accepts the batch and returns immediately (response time)</li>
 *   <li>The batch ID is generated correctly</li>
 *   <li>No exceptions are thrown during batch submission</li>
 * </ol>
 * <p>Full end-to-end verification (checking final transaction statuses) would
 * require polling or callbacks, which is beyond the scope of this assignment.</p>
 */
@SpringBootTest
@DisplayName("Performance Tests")
class TransactionPerformanceTest {

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("Should process 1,000 transactions without errors")
    void shouldProcess1000Transactions() {
        // Arrange
        List<TransactionRequestDTO> transactions = TestDataGenerator.generate1000Transactions();
        BatchTransactionRequest request = TestDataGenerator.createBatchRequest(transactions);

        // Act
        long startTime = System.currentTimeMillis();
        BatchTransactionResponse response = transactionService.processTransactions(request);
        long acceptanceTime = System.currentTimeMillis() - startTime;

        // Assert — service should accept the batch quickly (< 1 second)
        assertThat(response).isNotNull();
        assertThat(response.getBatchId()).isNotNull().isNotEmpty();
        assertThat(response.getStatus()).isEqualTo("Processing Started");

        System.out.println("========== Performance Test: 1,000 Transactions ==========");
        System.out.println("Batch ID: " + response.getBatchId());
        System.out.println("Acceptance Time: " + acceptanceTime + " ms");
        System.out.println("Transaction Count: " + transactions.size());
        System.out.println("===========================================================");

        // Allow some time for async processing to start
        // (not waiting for completion — just ensuring no immediate crash)
        assertThat(acceptanceTime).isLessThan(1000L);
    }

    @Test
    @DisplayName("Should process 5,000 transactions without errors")
    void shouldProcess5000Transactions() {
        // Arrange
        List<TransactionRequestDTO> transactions = TestDataGenerator.generate5000Transactions();
        BatchTransactionRequest request = TestDataGenerator.createBatchRequest(transactions);

        // Act
        long startTime = System.currentTimeMillis();
        BatchTransactionResponse response = transactionService.processTransactions(request);
        long acceptanceTime = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBatchId()).isNotNull().isNotEmpty();
        assertThat(response.getStatus()).isEqualTo("Processing Started");

        System.out.println("========== Performance Test: 5,000 Transactions ==========");
        System.out.println("Batch ID: " + response.getBatchId());
        System.out.println("Acceptance Time: " + acceptanceTime + " ms");
        System.out.println("Transaction Count: " + transactions.size());
        System.out.println("===========================================================");

        assertThat(acceptanceTime).isLessThan(1000L);
    }
}
