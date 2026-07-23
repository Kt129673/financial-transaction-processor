package com.assignment.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies that the Spring Application Context loads
 * successfully with all beans, configurations, and the H2 datasource.
 */
@SpringBootTest
class FinancialTransactionProcessorApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the entire Spring context — including JPA,
        // async config, and H2 — is wired correctly.
    }
}
