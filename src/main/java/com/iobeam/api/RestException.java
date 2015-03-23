package com.iobeam.api;

import com.iobeam.api.client.RestError;
import com.iobeam.api.http.StatusCode;

/**
 * Exception representing a JSON error returned by the RESTful API.
 */
public class RestException extends ApiException {

    private final StatusCode statusCode;
    private final int error;
    private final String details;

    public RestException(final StatusCode statusCode,
                         final int error,
                         final String message) {
        super(message);
        this.statusCode = statusCode;
        this.error = error;
        this.details = "";
    }

    public RestException(final StatusCode statusCode,
                         final RestError error) {
        super(error.getMessage());
        this.statusCode = statusCode;
        this.error = error.getError();
        this.details = error.getDetails();
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public int getError() {
        return error;
    }

    public String getDetails() {
        return details;
    }
}
