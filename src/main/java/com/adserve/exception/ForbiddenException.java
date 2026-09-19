package com.adserve.exception;

/**
 * Exception thrown when an authenticated user attempts to access a resource they do not own.
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends AdServeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String resourceName, Object resourceId) {
        super(String.format("Access denied: You do not have permission to access %s with id '%s'", resourceName, resourceId));
    }
}
