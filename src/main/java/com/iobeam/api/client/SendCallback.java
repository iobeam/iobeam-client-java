package com.iobeam.api.client;

import com.iobeam.api.resource.DataBatch;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Callback for when data sending is called asynchronously.
 */
public abstract class SendCallback {

    private static Import importFromLegacyBatch(ImportBatch batch) {
        Import ret = new Import(batch.getDeviceId(), batch.getProjectId());
        DataBatch db = batch.getData();
        Map<Long, Map<String, Object>> rows = db.getRows();
        for (Long ts : rows.keySet()) {
            for (String series : rows.get(ts).keySet()) {
                Object val = rows.get(ts).get(series);
                DataPoint dp;
                if (val instanceof String) {
                    dp = new DataPoint(ts, (String) val);
                } else if (val instanceof Long) {
                    dp = new DataPoint(ts, (Long) val);
                } else if (val instanceof Integer) {
                    dp = new DataPoint(ts, (Integer) val);
                } else if (val instanceof Float) {
                    dp = new DataPoint(ts, (Float) val);
                } else if (val instanceof Double) {
                    dp = new DataPoint(ts, (Double) val);
                } else {
                    continue;
                }
                ret.addDataPoint(series, dp);
            }
        }

        return ret;
    }

    final RestCallback<Void> innerCallback = new RestCallback<Void>() {

        // TODO(rrk): Make this work with batches
        private Map<String, Set<DataPoint>> getDataFromRequest(RestRequest req) {
            ImportBatch imp = (ImportBatch) req.getBuilder().getContent();
            if (imp.isFromLegacy()) {
                Map<String, Set<DataPoint>> ret = new HashMap<String, Set<DataPoint>>();
                ret.putAll(importFromLegacyBatch(imp).getSeries());
                return ret;
            }
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
