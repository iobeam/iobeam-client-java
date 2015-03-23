package com.iobeam.api.http;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP status codes returned by API calls.
 */
public enum StatusCode {

    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NO_CONTENT(204),

    MOVED_PERMANENTLY(301),
    FOUND(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NOT_ACCEPTABLE(406),
    REQUEST_TIMEOUT(408),
    CONFLICT(409),
    LENGTH_REQUIRED(411),
    PRECONDITION_FAILED(412),
    REQUEST_ENTITY_TOO_LARGE(413),
    TOO_MANY_REQUESTS(429),

    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    HTTP_VERSION_NOT_SUPPORTED(504);

    private static final EnumMap<StatusCode, String> descriptions =
        new EnumMap<StatusCode, String>(StatusCode.class);

    private static final Map<Integer, StatusCode> reverseLookup =
        new HashMap<Integer, StatusCode>();

    static {
        for (final StatusCode status : StatusCode.values()) {
            descriptions.put(status, status.name().toLowerCase().replace('_', ' '));
            reverseLookup.put(status.getCode(), status);
        }
    }

    private final int status;

    private StatusCode(final int status) {
        this.status = status;
    }

    public int getCode() {
        return status;
    }

    public String getDescription() {
        return descriptions.get(this);
    }

    public static StatusCode fromValue(final int statusCode) {
        return reverseLookup.get(statusCode);
    }
}
