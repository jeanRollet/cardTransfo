package com.carddemo.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Transaction Exception
 *
 * Custom exception for transaction-related errors.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TransactionException extends RuntimeException {

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TransactionException transactionNotFound(Long transactionId) {
        return new TransactionException("Transaction not found: " + transactionId);
    }

    public static TransactionException accountNotFound(Long accountId) {
        return new TransactionException("Account not found: " + accountId);
    }

    public static TransactionException invalidDateRange(String message) {
        return new TransactionException("Invalid date range: " + message);
    }

    public static TransactionException invalidOperation(String message) {
        return new TransactionException("Invalid operation: " + message);
    }
}
