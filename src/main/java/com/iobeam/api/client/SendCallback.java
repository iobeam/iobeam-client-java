package com.iobeam.api.client;

import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.ImportBatch;

import java.util.Map;
import java.util.Set;

/**
 * Callback for when data sending is called asynchronously.
 */
public abstract class SendCallback {

    final RestCallback<Void> innerCallback = new RestCallback<Void>() {

        // TODO(rrk): Make this work with batches
        private Map<String, Set<DataPoint>> getDataFromRequest(RestRequest req) {
            ImportBatch imp = (ImportBatch) req.getBuilder().getContent();
            //Map<String, Set<DataPoint>> ret = new HashMap<String, Set<DataPoint>>();
            //ret.putAll(imp.getSeries());

            return null;
        }

        @Override
        public void completed(Void result, RestRequest req) {
            onSuccess();
        }

        @Override
        public void failed(Throwable exc, RestRequest req) {
            onFailure(exc, getDataFromRequest(req));
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
    public abstract void onFailure(Throwable exc, Map<String, Set<DataPoint>> data);
}
