package com.iobeam.api.client;

import com.iobeam.api.resource.ImportBatch;

/**
 * Callback for when data sending is called asynchronously.
 */
public abstract class SendCallback {

    final RestCallback<Void> innerCallback = new RestCallback<Void>() {

        @Override
        public void completed(Void result, RestRequest req) {
            onSuccess();
        }

        @Override
        public void failed(Throwable exc, RestRequest req) {
            onFailure(exc, (ImportBatch) req.getBuilder().getContent());
        }
    };

    /**
     * Called when the data send request succeeds.
     */
    public abstract void onSuccess();

    /**
     * Called when the data send request fails.
     *
     * @param exc  The error that caused the request to fail.
     * @param data The data that failed to be imported, as a map from series name to a set of
     *             DataPoints.
     */
    public abstract void onFailure(Throwable exc, ImportBatch data);
}
