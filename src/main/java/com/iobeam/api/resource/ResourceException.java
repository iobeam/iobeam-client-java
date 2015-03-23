package com.iobeam.api.resource;

import com.iobeam.api.ApiException;

/**
 * Parse exception for resources.
 */
public class ResourceException extends ApiException {

    public ResourceException(final String message) {
        super(message);
    }
}
