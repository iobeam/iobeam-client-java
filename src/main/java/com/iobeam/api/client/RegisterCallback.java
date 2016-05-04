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


    RestCallback<Device> getInnerCallback(final Iobeam iobeam) {
        return new RestCallback<Device>() {
            @Override
            public void completed(Device result, RestRequest req) {
                try {
                    iobeam.setDeviceId(result.getId());
                } catch (Exception e) {
                    onFailure(e, req);
                }
                onSuccess(result);
            }

            @Override
            public void failed(Throwable exc, RestRequest req) {
                onFailure(exc, req);
            }
        };
    }

    /**
     * Called when the device registration request succeeds.
     *
     * @param deviceId The new device ID.
     * @deprecated Override onSuccess(Device) instead.
     */
    @Deprecated
    public abstract void onSuccess(String deviceId);

    /**
     * Called when the device registration request succeeds
     *
     * @param device Device object that was created.
     */
    public void onSuccess(Device device) {
        onSuccess(device.getId());
    }

    /**
     * Called when the device registration request fails.
     *
     * @param exc The error that resulted from the request.
     * @param req The request that caused the error.
     */
    public abstract void onFailure(Throwable exc, RestRequest req);
}
