package com.adserve.exception;

/**
 * Base unchecked exception for domain and runtime errors within the AdServe application.
 */
public class AdServeException extends RuntimeException {

    public AdServeException(String message) {
        super(message);
    }

    public AdServeException(String message, Throwable cause) {
        super(message, cause);
    }
}
