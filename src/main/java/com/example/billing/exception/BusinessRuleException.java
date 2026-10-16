package com.example.billing.exception;

/** A valid request that breaks a billing rule. Map to HTTP 409 or 422 in GlobalExceptionHandler. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
