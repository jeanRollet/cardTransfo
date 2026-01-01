package com.carddemo.account.exception;

/**
 * Exception thrown when a customer is not found
 */
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Integer customerId) {
        super("Customer not found: " + customerId);
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
