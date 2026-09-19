package com.adserve.exception;

/**
 * Exception thrown when a client request is malformed or violates business validation rules.
 */
public class BadRequestException extends AdServeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
