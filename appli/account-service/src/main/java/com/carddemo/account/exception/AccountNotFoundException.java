package com.carddemo.account.exception;

/**
 * Exception thrown when an account is not found
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long accountId) {
        super("Account not found: " + accountId);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
