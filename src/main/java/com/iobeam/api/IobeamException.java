package com.iobeam.api;


public class IobeamException extends RuntimeException {

    public IobeamException(final String message) {
        super(message);
    }

    public IobeamException(final ApiException e) {
        this(e.getMessage());
    }
}
