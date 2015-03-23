package com.iobeam.api.client;

import org.json.JSONObject;

/**
 * A bean-style REST error that can be mapped to/from JSON.
 */
public class RestError {

    private final int error;
    private final String message;
    private final String details;

    // Some useful errors. NOTE: must match Sleipnir REST errors.
    public static final int RESOURCE_NOT_CREATED = 31;
    public static final int RESOURCE_NOT_FOUND = 32;
    public static final int RESOURCE_ALREADY_EXISTS = 33;

    public RestError(final int error,
                     final String message,
                     final String details) {
        this.error = error;
        this.message = message;
        this.details = details;
    }

    public int getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public static RestError fromJson(final JSONObject json) {
        return new RestError(json.getInt("code"),
                             json.getString("message"),
                             json.optString("detailed_message", ""));
    }
}
