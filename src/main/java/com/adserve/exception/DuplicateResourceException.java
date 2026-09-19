package com.adserve.exception;

/**
 * Exception thrown when a resource already exists with a unique attribute (e.g. duplicate email).
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends AdServeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
