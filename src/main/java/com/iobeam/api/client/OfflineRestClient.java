package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.http.RequestBuilder;
import com.iobeam.api.http.StatusCode;

import java.io.IOException;

/**
 * A RestClient that automatically resends request that have been logged during offline periods.
 */
public class OfflineRestClient extends RestClient {

    public OfflineRestClient(final OfflineManager om) {
        super(om);
    }

    @Override
    public <T> T executeRequest(RequestBuilder builder, StatusCode expectedStatusCode,
                                Class<T> responseClass) throws IOException, ApiException {

        // Don't allow old requests to throw exceptions.
        try {
            for (OfflineManager.RequestRecord rr : getOfflineManager().getOfflineRequests()) {
                super.executeRequest(rr.getBuilder(), rr.getExpectedCode(), rr.getResponseClass());
            }
        } catch (IOException e) {
            // Ignore
            logger.warning("Could not send offline requests: " + e);
        } catch (ApiException e) {
            // Ignore
            logger.warning("Could not send offline requests: " + e);
        }
        return super.executeRequest(builder, expectedStatusCode, responseClass);
    }
}
