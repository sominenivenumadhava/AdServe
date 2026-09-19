package com.adserve.exception;

/**
 * Exception thrown when a requested resource (campaign, ad, etc.) is not found.
 */
public class ResourceNotFoundException extends AdServeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
