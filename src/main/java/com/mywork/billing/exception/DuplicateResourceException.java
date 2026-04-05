package com.mywork.billing.exception;

// Thrown when a unique constraint would be violated - maps to HTTP 409 Conflict
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
