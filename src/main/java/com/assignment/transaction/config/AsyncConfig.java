package com.assignment.transaction.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the thread pool used for asynchronous transaction batch processing.
 *
 * <h3>Thread Pool Sizing Rationale</h3>
 * <ul>
 *   <li><b>Core Pool = 10</b>: Keeps 10 threads alive at all times. For a batch of 10,000
 *       transactions grouped by sourceAccountId, we'll typically have dozens of groups —
 *       10 concurrent threads provides good parallelism without overwhelming the database
 *       connection pool (HikariCP defaults to 10 connections).</li>
 *   <li><b>Max Pool = 20</b>: Allows burst capacity when the queue fills up. The pool
 *       scales from 10 to 20 threads only when the queue (1000 tasks) is full. This
 *       prevents unbounded thread creation under extreme load.</li>
 *   <li><b>Queue Capacity = 1000</b>: Buffers tasks when all core threads are busy.
 *       New tasks are queued instead of spawning new threads, which is more efficient
 *       for short-lived tasks. Only when the queue is full do we scale to max pool.</li>
 *   <li><b>Thread Name Prefix</b>: All async threads are named "txn-processor-N",
 *       making them immediately identifiable in logs and thread dumps.</li>
 * </ul>
 *
 * <h3>Why not virtual threads?</h3>
 * <p>Java 21 supports virtual threads, but our workload is I/O-bound (database locks).
 * Virtual threads with PESSIMISTIC_WRITE locks could create thousands of threads
 * all waiting on the same row lock — the thread count balloons but throughput doesn't
 * improve. A bounded pool with 10-20 threads keeps database contention manageable.</p>
 */
@Configuration
@Slf4j
public class AsyncConfig {

    /**
     * Creates the executor bean used by {@code @Async("transactionExecutor")} methods.
     *
     * <p>Spring's {@code @Async} annotation looks up an {@code Executor} bean by name.
     * Using a named bean (rather than the default {@code SimpleAsyncTaskExecutor})
     * gives us full control over pool sizing, queue behavior, and rejection policies.</p>
     *
     * @return a configured ThreadPoolTaskExecutor
     */
    @Bean(name = "transactionExecutor")
    public Executor transactionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("txn-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Transaction Executor initialized — Core: {}, Max: {}, Queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
