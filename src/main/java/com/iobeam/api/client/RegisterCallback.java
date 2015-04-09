package com.iobeam.api.client;

import com.iobeam.api.resource.Device;

/**
 * Callback for registering a device asynchronously.
 */
public abstract class RegisterCallback {

    static RegisterCallback getEmptyCallback() {
        return new RegisterCallback() {
            @Override
            public void onSuccess(String deviceId) {
            }

            @Override
            public void onFailure(Throwable exc, RestRequest req) {
                exc.printStackTrace();
            }
        };
    }

    final RestCallback<Device.Id> innerCallback = new RestCallback<Device.Id>() {
        @Override
        public void completed(Device.Id result, RestRequest req) {
            try {
                Iobeam.setDeviceId(result.toString());
            } catch (Exception e) {
                onFailure(e, req);
            }
            onSuccess(result.getId());
        }

        @Override
        public void failed(Throwable exc, RestRequest req) {
            onFailure(exc, req);
        }
    };

    /**
     * Called when the device registration request succeeds.
     * @param deviceId The new device ID.
     */
    public abstract void onSuccess(String deviceId);

    /**
     * Called when the device registration request fails.
     * @param exc The error that resulted from the request.
     * @param req The request that caused the error.
     */
    public abstract void onFailure(Throwable exc, RestRequest req);
}
