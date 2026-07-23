package com.assignment.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Financial Transaction Processor application.
 *
 * <p>{@code @SpringBootApplication} enables component scanning, auto-configuration,
 * and property support — all scoped to the {@code com.assignment.transaction} package
 * and its sub-packages.</p>
 *
 * <p>{@code @EnableAsync} activates Spring's asynchronous method execution capability,
 * which is required for processing transaction batches in the background using
 * our custom {@code ThreadPoolTaskExecutor}.</p>
 */
@SpringBootApplication
@EnableAsync
public class FinancialTransactionProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialTransactionProcessorApplication.class, args);
    }
}
