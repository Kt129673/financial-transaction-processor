package com.assignment.transaction.exception;

/**
 * Thrown when a transaction references an account ID that does not exist.
 *
 * <p>This is a business-level exception (not a JPA exception), so it extends
 * {@code RuntimeException} rather than a checked exception. The
 * {@code GlobalExceptionHandler} catches this and returns a proper API response.</p>
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super("Account not found with ID: " + accountId);
    }
}
