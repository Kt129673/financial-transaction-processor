package com.assignment.transaction.exception;

import java.math.BigDecimal;

/**
 * Thrown when a debit transaction would cause the source account's
 * balance to go below zero (overdraft).
 *
 * <p>The assignment requires: "If balance insufficient → Mark FAILED.
 * Do not update balances." This exception carries the context needed
 * for the failure reason message.</p>
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long accountId, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient balance in account %d: current=%.4f, requested=%.4f",
                accountId, currentBalance, requestedAmount));
    }
}
