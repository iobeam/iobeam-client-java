package com.iobeam.api.http;

/**
 * HTTP content types supported by the API.
 */
public enum ContentType {
    NONE(""),
    JSON("application/json"),
    URLENCODED("application/x-www-form-urlencoded");

    private final String value;

    private ContentType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
