package com.iobeam.api.auth;

import com.iobeam.api.RestException;
import com.iobeam.api.client.RestError;
import com.iobeam.api.http.StatusCode;

/**
 * A specific RestException for authentication failures.
 */
public class AuthException extends RestException {

    public AuthException(final int error, final String message) {
        super(StatusCode.UNAUTHORIZED, error, message);
    }

    public AuthException(final RestError error) {
        super(StatusCode.UNAUTHORIZED, error);
    }
}
