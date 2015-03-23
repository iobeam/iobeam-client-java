package com.iobeam.api;

/**
 * Typed exception for general API errors.
 */
public class ApiException extends Exception {

    public ApiException(final String message) {
        super(message);
    }
}
