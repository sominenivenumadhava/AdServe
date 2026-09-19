package com.adserve.exception;

/**
 * Exception thrown when the ad server cannot find any active, valid advertisement
 * matching the requested targeting criteria (country, device, category).
 * Maps to HTTP 404 Not Found.
 */
public class NoEligibleAdException extends AdServeException {

    public NoEligibleAdException(String message) {
        super(message);
    }

    public NoEligibleAdException(String country, String device, String category) {
        super(String.format("No eligible advertisement found for targeting criteria - Country: '%s', Device: '%s', Category: '%s'",
                country, device, category));
    }
}
