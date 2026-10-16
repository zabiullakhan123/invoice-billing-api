package com.example.billing.exception;

/** Map to HTTP 404 in GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
