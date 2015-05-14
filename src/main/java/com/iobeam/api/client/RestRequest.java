package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestBuilder;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 *
 */
public abstract class RestRequest<T> {

    private final RestClient client;
    private final RequestBuilder builder;
    private final StatusCode expectedCode;
    private final Class<T> responseClass;
    private final boolean needAuth;

    protected RestRequest(final RestClient client,
                          final RequestMethod method,
                          final String path,
                          final ContentType type,
                          final Object content,
                          final StatusCode expectedCode,
                          final Class<T> responseClass,
                          final boolean needAuth) {
        this.builder = new RequestBuilder(client.getBaseUrl() + path);
        this.client = client;
        this.responseClass = responseClass;
        this.expectedCode = expectedCode;
        this.builder.setRequestMethod(method)
            .setContentType(type)
            .setContent(content);
        this.needAuth = needAuth;
    }

    protected RestRequest(final RestClient client,
                          final RequestMethod method,
                          final String path,
                          final ContentType type,
                          final Object content,
                          final StatusCode expectedCode,
                          final Class<T> responseClass) {
        this.builder = new RequestBuilder(client.getBaseUrl() + path);
        this.client = client;
        this.responseClass = responseClass;
        this.expectedCode = expectedCode;
        this.builder.setRequestMethod(method)
            .setContentType(type)
            .setContent(content);
        this.needAuth = true;
    }

    protected RestRequest(final RestClient client,
                          final RequestMethod method,
                          final String path,
                          final StatusCode expectedCode,
                          final Class<T> responseClass,
                          final boolean needAuth) {
        this(client, method, path, ContentType.NONE,
             null, expectedCode, responseClass, needAuth);
    }

    protected RestRequest(final RestClient client,
                          final RequestMethod method,
                          final String path,
                          final StatusCode expectedCode,
                          final Class<T> responseClass) {
        this(client, method, path, ContentType.NONE,
             null, expectedCode, responseClass, true);
    }

    public RequestBuilder getBuilder() {
        return builder;
    }

    /**
     * Execute the REST request synchronously on the calling thread, returning the result when the
     * operation completes.
     *
     * @return The response result of the REST request.
     */
    public T execute() throws ApiException, IOException {
        return client.executeRequest(this.builder, expectedCode, responseClass, needAuth);
    }

    /**
     * Execute the REST request asynchronously and call the given handler when the operation
     * completes. While the request itself is executed on the client's executor service, the handler
     * runs on the executor service given as a parameter.
     *
     * @param handler  the handler to run once the request completes, or failure occurs.
     * @param executor the executor to run the handler on.
     * @return A future result of the asynchronous operation.
     */
    public Future<T> executeAsync(final RestCallback<T> handler,
                                  final Executor executor) {

        return client.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                try {
                    final T result = execute();
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            handler.completed(result, RestRequest.this);
                        }
                    });
                    return result;
                } catch (final Exception e) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            handler.failed(e, RestRequest.this);
                        }
                    });
                    throw e;
                }
            }
        });
    }

    /**
     * Execute the REST request asynchronously and call the given handler when the operation
     * completes. The handler runs on the REST client's executor service.
     *
     * @param handler the handler to run once the request completes, or failure occurs.
     * @return A future result of the asynchronous operation.
     */
    public Future<T> executeAsync(final RestCallback<T> handler) {

        return client.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                try {
                    final T result = execute();
                    handler.completed(result, RestRequest.this);
                    return result;
                } catch (Exception e) {
                    handler.failed(e, RestRequest.this);
                    throw e;
                }
            }
        });
    }

    /**
     * Execute the REST request asynchronously.
     *
     * @return A future result of the asynchronous operation.
     */
    public Future<T> executeAsync() {

        return client.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return execute();
            }
        });
    }
}
