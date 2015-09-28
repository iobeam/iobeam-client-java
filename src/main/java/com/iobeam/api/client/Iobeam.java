package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.auth.AuthHandler;
import com.iobeam.api.auth.DefaultAuthHandler;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Device;
import com.iobeam.api.resource.Import;
import com.iobeam.api.service.DeviceService;
import com.iobeam.api.service.ImportService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * The Iobeam client. Handles device registration and importing/sending of data to iobeam.
 */
public class Iobeam {

    private static final Logger logger = Logger.getLogger(Iobeam.class.getName());
    public static final String API_URL = "https://api.iobeam.com";
    static final String DEVICE_FILENAME = "iobeam-device-id";

    /**
     * Exception when actions involving the network (registration, sending data) are called before
     * initializing the client.
     */
    public static class NotInitializedException extends ApiException {

        public NotInitializedException() {
            super("iobeam client not initialized.");
        }
    }

    /**
     * Exception for when the device ID could not successfully be saved to disk.
     */
    public static class CouldNotPersistException extends ApiException {

        public CouldNotPersistException() {
            super("iobeam client could not save device id");
        }
    }

    /**
     * SendCallback used when autoRetry is set.
     */
    private static final class ReinsertSendCallback extends SendCallback {

        private final Iobeam client;

        public ReinsertSendCallback(Iobeam iobeam) {
            this.client = iobeam;
        }

        @Override
        public void onSuccess(Map<String, Set<DataPoint>> data) {
        }

        @Override
        public void onFailure(Throwable exc, Map<String, Set<DataPoint>> data) {
            client.addBulkData(data);
        }
    }

    public static class Builder {

        private final long projectId;
        private final String token;
        private String savePath;
        private String deviceId;
        private boolean autoRetry;

        public Builder(long projectId, String projectToken) {
            this.projectId = projectId;
            this.token = projectToken;
            this.savePath = null;
            this.deviceId = null;
            this.autoRetry = false;
        }

        public Builder saveIdToPath(String path) {
            this.savePath = path;

            return this;
        }

        public Builder setDeviceId(String id) {
            this.deviceId = id;

            return this;
        }

        public Builder autoRetry() {
            return this.autoRetry(true);
        }

        public Builder autoRetry(boolean retry) {
            this.autoRetry = retry;

            return this;
        }

        public Iobeam build() {
            try {
                Iobeam  client = new Iobeam(this.projectId, this.token, this.savePath,
                                            this.deviceId);
                client.setAutoRetry(this.autoRetry);

                return client;
            } catch (ApiException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    String path = null;
    long projectId = -1;
    String projectToken = null;
    String deviceId = null;

    private RestClient client = null;
    private final Object dataStoreLock = new Object();
    private Import dataStore;
    private boolean autoRetry = false;

    private Iobeam(long projectId, String projectToken, String path, String deviceId)
        throws ApiException {
        init(path, projectId, projectToken, deviceId);
    }

    /**
     * Constructs an Iobeam object without a device ID.
     *
     * @param path         Directory where the device ID file, iobeam-device-id, should be written.
     *                     If null, the device ID is <b>not</b> persisted.
     * @param projectId    The numeric project ID to associate with.
     * @param projectToken The token to use when communicating with iobeam cloud.
     * @throws ApiException Thrown if something goes wrong with initializing the device ID.
     */
    @Deprecated
    public Iobeam(String path, long projectId, String projectToken)
        throws ApiException {
        this(path, projectId, projectToken, null);
    }

    /**
     * Constructs an Iobeam object with a device ID.
     *
     * @param path         Directory where the device ID file, iobeam-device-id, should be written.
     *                     If null, the device ID is <b>not</b> persisted.
     * @param projectId    The numeric project ID to associate with.
     * @param projectToken The token to use when communicating with iobeam cloud.
     * @param deviceId     Pre-registered device id to be used.
     * @throws ApiException Thrown if something goes wrong with initializing the device ID.
     */
    @Deprecated
    public Iobeam(String path, long projectId, String projectToken, String deviceId)
        throws ApiException {
        init(path, projectId, projectToken, deviceId);
    }

    /**
     * (Re-)initializes the iobeam client.
     *
     * @param path         Directory where the device ID file, iobeam-device-id, should be written.
     *                     If null, the device ID is <b>not</b> persisted.
     * @param projectId    The numeric project ID to associate with.
     * @param projectToken The token to use when communicating with iobeam cloud.
     * @param deviceId     The device ID that should be used by the iobeam client.
     * @throws ApiException Thrown if something goes wrong with initializing the device ID.
     */
    void init(String path, long projectId, String projectToken, String deviceId)
        throws ApiException {
        this.path = path;
        this.projectId = projectId;
        this.projectToken = projectToken;
        if (deviceId == null && path != null) {
            deviceId = localDeviceIdCheck();
        }
        setDeviceId(deviceId);

        client = new RestClient(API_URL, Executors.newSingleThreadExecutor());
        File dir = path != null ? new File(path) : null;
        AuthHandler handler = new DefaultAuthHandler(client, projectId, projectToken, dir);
        client.setAuthenticationHandler(handler);
    }

    /**
     * Tells whether the iobeam client has been initialized.
     *
     * @return True if initialized; otherwise, false.
     */
    public boolean isInitialized() {
        return projectId > 0 && projectToken != null && client != null;
    }

    /**
     * Resets the iobeam client to an uninitialized state including clearing all added data.
     */
    void reset() {
        reset(true);
    }

    /**
     * Resets the iobeam client to uninitialized state, including removing any added data.
     *
     * @param deleteFile Whether or not to delete the on-disk device ID. Tests use false sometimes.
     */
    void reset(boolean deleteFile) {
        String path = this.path;
        this.path = null;
        this.projectId = -1;
        this.projectToken = null;
        this.deviceId = null;

        this.client = null;

        synchronized (dataStoreLock) {
            dataStore = null;
        }

        if (deleteFile) {
            File f = new File(path, DEVICE_FILENAME);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    public boolean getAutoRetry() {
        return this.autoRetry;
    }

    /**
     * Sets whether this Iobeam object automatically tries to resend data that fails.
     *
     * @param retry Whether to retry or not.
     */
    public void setAutoRetry(boolean retry) {
        this.autoRetry = retry;
    }

    private void persistDeviceId() throws CouldNotPersistException {
        File f = new File(this.path, DEVICE_FILENAME);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(this.deviceId);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CouldNotPersistException();
        }
    }

    private String localDeviceIdCheck() {
        File f = new File(this.path, DEVICE_FILENAME);
        try {
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = br.readLine();
                br.close();
                return line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private DeviceService.Add prepareDeviceRequest(String deviceId, String deviceName)
        throws NotInitializedException {
        if (!isInitialized()) {
            throw new NotInitializedException();
        }
        DeviceService service = new DeviceService(client);
        return service.add(projectId, new Device.Spec(deviceId, deviceName, null), null);
    }

    /**
     * Registers this device and assign it a random device ID and name. This call is <b>BLOCKING</b>
     * and should not be called on UI threads. It will make a network call and not return until it
     * completes. If a device ID is already assigned, it will be returned.
     *
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerDevice() throws ApiException, IOException {
        return registerDeviceWithId(null, null);
    }

    /**
     * Registers this device with a provided device ID (and assigns a random device name). This call
     * is <b>BLOCKING</b> and should not be called on UI threads. It will make a network call and
     * not return until it finishes. Device IDs must be at least 16 characters; if null, a random
     * one will be assigned. If a device ID is already assigned, and a non-null ID is not provided,
     * the current one will be returned.
     *
     * @param deviceId The desired id for this device; if null, a random one is assigned.
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerDeviceWithId(String deviceId) throws ApiException, IOException {
        return registerDeviceWithId(deviceId, null);
    }

    /**
     * Registers this device with a provided device ID and device name. This call is <b>BLOCKING</b>
     * and should not be called on UI threads. It will make a network call and not return until it
     * finishes. Device IDs must be at least 16 characters; if null, a random one will be assigned.
     * If a device ID is already assigned, and a non-null ID is not provided, the current one will
     * be returned.
     *
     * @param deviceId   The desired id for this device; if null, a random one is assigned.
     * @param deviceName The desired name for this device; if null, a random one is assigned.
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerDeviceWithId(String deviceId, String deviceName)
        throws ApiException, IOException {
        boolean alreadySet = this.deviceId != null;
        // If device ID is set and not explicitly asking for a different one, return current ID.
        if (alreadySet && (deviceId == null || this.deviceId.equals(deviceId))) {
            return this.deviceId;
        }

        // Make sure to unset before attempting, so as not to reuse old ID if it fails.
        this.deviceId = null;
        DeviceService.Add req = prepareDeviceRequest(deviceId, deviceName);
        String id = req.execute().getId();
        this.deviceId = id;
        if (path != null) {
            persistDeviceId();
        }
        return id;
    }

    /**
     * Registers this device and gets an assigned ID and name in asynchronous fashion. This will not
     * block the calling thread. If successful, the device ID of this object will be set, and if a
     * file path was provided, it will be written to disk. If a device ID is already assigned, no
     * additional operation will be taken.
     *
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceAsync() throws ApiException {
        registerDeviceWithIdAsync(null, null, null);
    }

    /**
     * Registers this device and gets an assigned ID and name in asynchronous fashion. This will not
     * block the calling thread. If successful, the device ID of this object will be set, and if a
     * file path was provided, it will be written to disk. Any provided callback will be run on the
     * background thread. If a device ID is already assigned, the callback will be executed with the
     * current ID before returning.
     *
     * @param callback Callback for result of the registration.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceAsync(RegisterCallback callback) throws ApiException {
        registerDeviceWithIdAsync(null, callback);
    }

    /**
     * Registers this device with the provided device ID. This will not block the calling thread. If
     * successful, the device ID of this object will be set, and if a file path was provided, it
     * will be written to disk. Device ID must be at least 16 characters. If a device ID is already
     * assigned, this action will only occur if a non-null deviceId is provided.
     *
     * @param deviceId Desired device ID.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceWithIdAsync(String deviceId) throws ApiException {
        registerDeviceWithIdAsync(deviceId, null, null);
    }

    /**
     * Registers this device with the provided device ID and name. This will not block the calling
     * thread. If successful, the device ID of this object will be set, and if a file path was
     * provided, it will be written to disk. Device ID must be at least 16 characters. If a device
     * ID is already assigned, this action will only occur if a non-null deviceId is provided.
     *
     * @param deviceId Desired device ID.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceWithIdAsync(String deviceId, String deviceName) throws ApiException {
        registerDeviceWithIdAsync(deviceId, deviceName, null);
    }

    /**
     * Registers this device with the provided device ID. This will not block the calling thread. If
     * successful, the device ID of this object will be set, and if a file path was provided, it
     * will be written to disk. Device ID must be at least 16 characters. Any provided callback will
     * be run on the background thread. If a device ID is already, a new ID will be registered only
     * for a non-null deviceId. Otherwise, the callback will be called with the current ID before
     * returning.
     *
     * @param deviceId Desired device ID.
     * @param callback Callback for result of the registration.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceWithIdAsync(String deviceId, RegisterCallback callback)
        throws ApiException {
        registerDeviceWithIdAsync(deviceId, null, callback);
    }

    /**
     * Registers this device with the provided device ID and name. This will not block the calling
     * thread. If successful, the device ID of this object will be set, and if a file path was
     * provided, it will be written to disk. Device ID must be at least 16 characters. Any provided
     * callback will be run on the background thread. If a device ID is already, a new ID will be
     * registered only for a non-null deviceId. Otherwise, the callback will be called with the
     * current ID before returning.
     *
     * @param deviceId   Desired device ID.
     * @param callback   Callback for result of the registration.
     * @param deviceName Desired device name.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceWithIdAsync(String deviceId, String deviceName,
                                          RegisterCallback callback) throws ApiException {
        RestCallback<Device.Id> cb;
        if (callback == null) {
            cb = RegisterCallback.getEmptyCallback().getInnerCallback(this);
        } else {
            cb = callback.getInnerCallback(this);
        }

        // If device ID is set and not explicitly asking for a different one, return current ID.
        boolean alreadySet = this.deviceId != null;
        if (alreadySet && (deviceId == null || this.deviceId.equals(deviceId))) {
            cb.completed(new Device.Id(this.deviceId), null);
            return;
        }

        // Make sure to unset before attempting, so as not to reuse old ID if it fails.
        this.deviceId = null;
        DeviceService.Add req = prepareDeviceRequest(deviceId, deviceName);
        req.executeAsync(cb);
    }

    /**
     * Gets the current device id.
     *
     * @return The current device id.
     */
    public String getDeviceId() {
        return this.deviceId;
    }

    /**
     * Sets the current device id that the iobeam client is associated with.
     *
     * @param deviceId Device id to be associated with the iobeam client.
     * @throws CouldNotPersistException Thrown if there are problems saving the device id to disk.
     */
    public void setDeviceId(String deviceId) throws CouldNotPersistException {
        this.deviceId = deviceId;
        if (deviceId != null && path != null) {
            persistDeviceId();
        }
    }

    Import getDataStore() {
        return dataStore;
    }

    /**
     * Adds a data value to a particular series in the data store.
     *
     * @param seriesName The name of the series that the data belongs to.
     * @param dataPoint  The DataPoint representing a data value at a particular time.
     */
    public void addData(String seriesName, DataPoint dataPoint) {
        synchronized (dataStoreLock) {
            if (dataStore == null) {
                dataStore = new Import(deviceId, projectId);
            }
            dataStore.addDataPoint(seriesName, dataPoint);
        }
    }

    void addBulkData(Map<String, Set<DataPoint>> data) {
        if (data == null) {
            return;
        }

        synchronized (dataStoreLock) {
            if (dataStore == null) {
                dataStore = new Import(deviceId, projectId);
            }
            for (String series : data.keySet()) {
                dataStore.addDataPointSet(series, data.get(series));
            }
        }
    }

    /**
     * Returns the size of all of the data in all the series.
     *
     * @return Size of the data store, or 0 if it has not been made yet.
     */
    public long getDataSize() {
        synchronized (dataStoreLock) {
            if (dataStore == null) {
                return 0;
            }

            return dataStore.getTotalSize();
        }
    }

    /**
     * Returns the size of the data set in a particular series.
     *
     * @param series The series to query
     * @return Size of the data set, or 0 if series does not exist.
     */
    public int getSeriesSize(String series) {
        synchronized (dataStoreLock) {
            if (dataStore == null) {
                return 0;
            }
            Import.DataSet set = dataStore.getDataSet(series);
            return set == null ? 0 : set.size();
        }
    }

    private List<ImportService.Submit> prepareDataRequests() throws ApiException {
        if (!isInitialized()) {
            throw new NotInitializedException();
        }
        if (deviceId == null) {
            throw new ApiException("Device id not set, cannot send data.");
        }

        // Synchronize so no more data is added to this object while we send.
        Import data;
        synchronized (dataStoreLock) {
            data = dataStore;
            dataStore = null;
        }
        // No data to send, log a warning and return an empty list.
        if (data == null) {
            logger.warning("No data to send.");
            return new ArrayList<ImportService.Submit>();
        }
        data.setDeviceId(deviceId);

        ImportService service = new ImportService(client);
        return service.submit(data);
    }

    /**
     * Sends the stored data to the iobeam cloud. This call is <b>BLOCKING</b>, which means the
     * caller thread will not return until the network call is complete. Do not call on UI threads.
     * This call will take a snapshot of the data store at this moment and send it; future data adds
     * will be to a NEW data store.
     *
     * If `autoRetry` is set, failed requests will add the previous data to the new data store.
     *
     * @throws ApiException Thrown is the client is not initialized or if the device id has not been
     *                      set.
     * @throws IOException  Thrown if there are network issues connecting to iobeam cloud.
     */
    public void send() throws ApiException, IOException {
        List<ImportService.Submit> reqs = prepareDataRequests();
        for (ImportService.Submit req : reqs) {
            try {
                req.execute();
            } catch (Exception e) {
                if (autoRetry) {
                    Import imp = (Import) req.getBuilder().getContent();
                    Map<String, Set<DataPoint>> data = new HashMap<String, Set<DataPoint>>();
                    data.putAll(imp.getSeries());
                    addBulkData(data);
                }

                // TODO: When we target Java7, we can just do a multi-exception catch
                if (e instanceof ApiException) {
                    throw (ApiException) e;
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                }
            }
        }
    }

    /**
     * Asynchronous version of send() that will not block the calling thread. No callback provided,
     * same as calling sendAsync(null).
     *
     * If `autoRetry` is set, failed requests will add the previous data to the new data store.
     *
     * @throws ApiException Thrown is the client is not initialized or if the device id has not been
     *                      set.
     */
    public void sendAsync() throws ApiException {
        sendAsync(null);
    }

    /**
     * Asynchronous version of send() that will not block the calling thread. Any provided callback
     * will be run on the background thread when the operation completes.
     *
     * If `autoRetry` is set, failed requests will add the previous data to the new data store.
     *
     * @param callback Callback for when the operation completes.
     * @throws ApiException Thrown is the client is not initialized or if the device id has not been
     *                      set.
     */
    public void sendAsync(SendCallback callback) throws ApiException {
        List<ImportService.Submit> reqs = prepareDataRequests();
        for (ImportService.Submit req : reqs) {
            if (callback == null && !autoRetry) {
                req.executeAsync();
            } else if (callback == null) {
                req.executeAsync(new ReinsertSendCallback(this).innerCallback);
            } else {
                req.executeAsync(callback.innerCallback);
            }
        }
    }
}
