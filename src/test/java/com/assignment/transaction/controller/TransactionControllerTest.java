package com.assignment.transaction.controller;

import com.assignment.transaction.response.BatchTransactionResponse;
import com.assignment.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller integration tests using MockMvc.
 *
 * <h3>Why @WebMvcTest?</h3>
 * <p>Loads only the web layer — controllers, exception handlers, and
 * JSON serialization. Does NOT load services, repositories, or the
 * database. The service is mocked via {@code @MockBean}.</p>
 *
 * <p>This tests HTTP-level concerns: status codes, content type,
 * JSON structure, and validation error responses.</p>
 */
@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private static final String API_URL = "/api/v1/transactions/process";

    @Test
    @DisplayName("Should return HTTP 202 with batch ID for valid request")
    void shouldReturn202ForValidRequest() throws Exception {
        // Arrange
        String requestJson = """
                {
                    "transactions": [
                        {
                            "sourceAccountId": 1,
                            "targetAccountId": 2,
                            "amount": 500,
                            "timestamp": "2026-07-20T10:00:00"
                        }
                    ]
                }
                """;

        when(transactionService.processTransactions(any()))
                .thenReturn(BatchTransactionResponse.builder()
                        .batchId("test-batch-id")
                        .status("Processing Started")
                        .build());

        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.batchId").value("test-batch-id"))
                .andExpect(jsonPath("$.status").value("Processing Started"));
    }

    @Test
    @DisplayName("Should return HTTP 400 when transaction list is empty")
    void shouldReturn400ForEmptyList() throws Exception {
        // Arrange
        String requestJson = """
                {
                    "transactions": []
                }
                """;

        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Should return HTTP 400 when source account ID is null")
    void shouldReturn400ForNullSourceAccountId() throws Exception {
        // Arrange — sourceAccountId is missing
        String requestJson = """
                {
                    "transactions": [
                        {
                            "targetAccountId": 2,
                            "amount": 500,
                            "timestamp": "2026-07-20T10:00:00"
                        }
                    ]
                }
                """;

        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Should return HTTP 400 when amount is negative")
    void shouldReturn400ForNegativeAmount() throws Exception {
        // Arrange
        String requestJson = """
                {
                    "transactions": [
                        {
                            "sourceAccountId": 1,
                            "targetAccountId": 2,
                            "amount": -100,
                            "timestamp": "2026-07-20T10:00:00"
                        }
                    ]
                }
                """;

        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Should return HTTP 400 for malformed JSON")
    void shouldReturn400ForMalformedJson() throws Exception {
        // Arrange
        String malformedJson = "{ this is not valid json }";

        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("Should return HTTP 400 when request body is missing")
    void shouldReturn400ForMissingBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should accept multiple transactions in a single batch")
    void shouldAcceptMultipleTransactions() throws Exception {
        // Arrange
        String requestJson = """
                {
                    "transactions": [
                        {
                            "sourceAccountId": 1,
                            "targetAccountId": 2,
                            "amount": 100,
                            "timestamp": "2026-07-20T10:00:00"
                        },
                        {
                            "sourceAccountId": 3,
                            "targetAccountId": 4,
                            "amount": 200,
                            "timestamp": "2026-07-20T10:01:00"
                        }
                    ]
                }
                """;

        when(transactionService.processTransactions(any()))
                .thenReturn(BatchTransactionResponse.builder()
                        .batchId("multi-batch-id")
                        .status("Processing Started")
                        .build());

        // Act & Assert
        mockMvc.perform(post(API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.batchId").value("multi-batch-id"));
    }
}
