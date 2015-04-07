package com.iobeam.api.client;

/**
 * Callback for when data sending is called asynchronously.
 */
public abstract class DataCallback {

    final RestCallback<Void> innerCallback = new RestCallback<Void>() {
        @Override
        public void completed(Void result, RestRequest req) {
            onSuccess();
        }

        @Override
        public void failed(Throwable exc, RestRequest req) {
            onFailure(exc, req);
        }
    };

    /**
     * Called when the data send request succeeds.
     */
    public abstract void onSuccess();

    /**
     * Called when the data send request fails.
     * @param exc The error that caused the request to fail.
     * @param req The request that failed.
     */
    public abstract void onFailure(Throwable exc, RestRequest req);
}
