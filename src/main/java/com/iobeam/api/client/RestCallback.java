package com.iobeam.api.client;

/**
 * Callback for asynchronous REST API calls.
 */
public interface RestCallback<T> {

    void completed(T result, RestRequest req);

    void failed(Throwable exc, RestRequest req);
}
