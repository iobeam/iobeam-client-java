package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.IobeamException;
import com.iobeam.api.RestException;
import com.iobeam.api.auth.AuthHandler;
import com.iobeam.api.auth.DefaultAuthHandler;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.DataStore;
import com.iobeam.api.resource.Device;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;
import com.iobeam.api.service.DeviceService;
import com.iobeam.api.service.ImportService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * The Iobeam client. Handles device registration and importing/sending of data to iobeam.
 */
public class Iobeam {

    private static final Logger logger = Logger.getLogger(Iobeam.class.getName());
    public static final String DEFAULT_API_URL = "https://api.iobeam.com";
    @Deprecated
    public static final String API_URL = DEFAULT_API_URL;
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
    static final class ReinsertSendCallback extends SendCallback {

        private final SendCallback userCB;
        private final Iobeam client;

        public ReinsertSendCallback(Iobeam iobeam, SendCallback userCB) {
            this.client = iobeam;
            this.userCB = userCB;
        }

        @Override
        public void onSuccess(ImportBatch data) {
            if (userCB != null) {
                userCB.onSuccess(data);
            }
        }

        @Override
        public void onFailure(Throwable exc, ImportBatch data) {
            client.addBulkData(data);

            if (userCB != null) {
                userCB.onFailure(exc, data);
            }
        }
    }

    private static final class IgnoreDupeRegisterCallback extends RegisterCallback {

        private final RegisterCallback userCB;
        private final Iobeam client;

        public IgnoreDupeRegisterCallback(Iobeam client, RegisterCallback userCB) {
            this.client = client;
            this.userCB = userCB;
        }

        @Override
        public void onSuccess(String deviceId) {
            if (userCB != null) {
                userCB.onSuccess(deviceId);
            }
        }

        @Override
        public void onFailure(Throwable exc, RestRequest req) {
            if (exc instanceof RestException) {
                if (((RestException) exc).getError() == DeviceService.ERR_DUPLICATE_ID) {
                    Device d = (Device) req.getBuilder().getContent();
                    this.getInnerCallback(client).completed(d, req);
                    return;
                }
            }

            if (userCB != null) {
                userCB.getInnerCallback(client).failed(exc, req);
            }
        }
    }

    public static class Builder {

        private final long projectId;
        private final String token;
        private String savePath;
        private String backendUrl;
        private String deviceId;
        private boolean autoRetry;

        public Builder(long projectId, String projectToken) {
            this.projectId = projectId;
            this.token = projectToken;
            this.savePath = null;
            this.backendUrl = DEFAULT_API_URL;
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

        @Deprecated
        public Builder setBackend(String url) {
            return this.backend(url);
        }

        public Builder backend(String url) {
            this.backendUrl = url;
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
                Iobeam client = new Iobeam(this.projectId, this.token, this.savePath,
                                           this.deviceId, this.backendUrl);
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
    @Deprecated
    private Import dataStore;
    private final List<DataStore> dataBatches = new ArrayList<DataStore>();
    private Map<String, DataStore> seriesToBatch = new HashMap<String, DataStore>();
    private boolean autoRetry = false;

    private Iobeam(long projectId, String projectToken, String path, String deviceId, String url)
        throws ApiException {
        init(path, projectId, projectToken, deviceId, url);
    }

    /**
     * (Re-)initializes the iobeam client.
     *
     * @param path         Directory where the device ID file, iobeam-device-id, should be written.
     *                     If null, the device ID is <b>not</b> persisted.
     * @param projectId    The numeric project ID to associate with.
     * @param projectToken The token to use when communicating with iobeam cloud.
     * @param deviceId     The device ID that should be used by the iobeam client.
     * @param backendUrl   The base URL of API services to talk to
     * @throws ApiException Thrown if something goes wrong with initializing the device ID.
     */
    void init(String path, long projectId, String projectToken, String deviceId, String backendUrl)
        throws ApiException {
        this.path = path;
        this.projectId = projectId;
        this.projectToken = projectToken;
        if (deviceId == null && path != null) {
            deviceId = localDeviceIdCheck();
        }
        setDeviceId(deviceId);

        client = new RestClient(backendUrl, Executors.newSingleThreadExecutor());
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
            dataBatches.clear();
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

    private DeviceService.Add prepareDeviceRequest(Device device)
        throws NotInitializedException {
        if (!isInitialized()) {
            throw new NotInitializedException();
        }
        DeviceService service = new DeviceService(client);
        final Device d;
        if (device != null) {
            // Copy to make sure project id is set right.
            d = new Device.Builder(projectId)
                .id(device.getId())
                .name(device.getName())
                .type(device.getType())
                .created(device.getCreated())
                .build();
        } else {
            d = new Device.Builder(projectId).build();
        }
        return service.add(d);
    }

    /**
     * Registers a device and assigns it a random device ID and name. This call is <b>BLOCKING</b>
     * and should not be called on UI threads.
     *
     * See {@link #registerDevice(Device)} for more details.
     *
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerDevice() throws ApiException, IOException {
        return registerDevice((Device) null);
    }

    /**
     * Registers a device with the provided device ID. This call is <b>BLOCKING</b> and should not
     * be called on UI threads.
     *
     * See {@link #registerDevice(Device)} for more details.
     *
     * @param deviceId The desired id for this device.
     * @return The device id provided
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     * @deprecated Use {@link #registerDevice(String)} instead. Will be removed after 0.6.x
     */
    @Deprecated
    public String registerDeviceWithId(String deviceId) throws ApiException, IOException {
        return registerDevice(deviceId);
    }

    /**
     * Registers a device with the provided device ID. This call is <b>BLOCKING</b> and should not
     * be called on UI threads.
     *
     * See {@link #registerDevice(Device)} for more details.
     *
     * @param deviceId The desired id for this device; if null, a random one is assigned.
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerDevice(String deviceId) throws ApiException, IOException {
        return registerDevice(new Device.Builder(projectId).id(deviceId).build());
    }

    /**
     * Registers a device with the provided device ID or sets it if already registered. This call is
     * <b>BLOCKING</b> and should not be called on UI threads.
     *
     * See {@link #registerDevice(Device)} for more details.
     *
     * @param deviceId The desired id for this device.
     * @return The device id provided
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerOrSetDevice(final String deviceId) throws ApiException, IOException {
        return registerOrSetDevice(new Device.Builder(projectId).id(deviceId).build());
    }

    /**
     * Registers a device with the provided device ID or sets it if already registered. This call is
     * <b>BLOCKING</b> and should not be called on UI threads.
     *
     * See {@link #registerDevice(Device)} for more details.
     *
     * @param device The desired id for this device.
     * @return The device id provided by the device
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerOrSetDevice(final Device device) throws ApiException, IOException {
        try {
            return registerDevice(device);
        } catch (RestException e) {
            if (e.getError() == DeviceService.ERR_DUPLICATE_ID) {
                setDevice(device);
                return device.getId();
            }
            throw e;
        }
    }

    /**
     * Registers a device with the provided device ID and device name. This call is <b>BLOCKING</b>
     * and should not be called on UI threads.
     *
     * See {@link #registerDevice(Device)} for more details.
     *
     * @param deviceId   The desired id for this device; if null, a random one is assigned.
     * @param deviceName The desired name for this device; if null, a random one is assigned.
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     * @deprecated Use {@link #registerDevice(Device)} instead. Will be removed after 0.6.x
     */
    @Deprecated
    public String registerDeviceWithId(String deviceId, String deviceName)
        throws ApiException, IOException {
        final Device d = new Device.Builder(projectId).id(deviceId).name(deviceName).build();
        return registerDevice(d);
    }

    /**
     * Registers a device with the same parameters as the provided {@link Device}. This call is
     * <b>BLOCKING</b> and should not be called on UI threads. It will make a network call and not
     * return until it finishes. If device is null, a new {@link Device} with a random ID and name
     * will be generated and its ID returned. If the client already has a device ID set, a null
     * device parameter will return the current ID.
     *
     * @param device A {@link Device} to be registered with iobeam. If ID is not set, a random one
     *               will be assigned. Its name will also be randomly generated if unassigned, or
     *               set to the ID if ID is provided but not name.
     * @return The device ID associated with this client.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public String registerDevice(Device device) throws ApiException, IOException {
        boolean alreadySet = this.deviceId != null;
        // If device ID is set and not explicitly asking for a different one, return current ID.
        if (alreadySet && (device == null || this.deviceId.equals(device.getId()))) {
            setDeviceId(this.deviceId);
            return this.deviceId;
        }

        // Make sure to unset before attempting, so as not to reuse old ID if it fails.
        this.deviceId = null;

        DeviceService.Add req = prepareDeviceRequest(device);
        String id = req.execute().getId();
        setDeviceId(id);
        return this.deviceId;
    }

    /**
     * Registers a device asynchronously with a randomly assigned ID.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @throws IobeamException Thrown if the iobeam client is not initialized AND a callback is not
     *                         provided. If a callback is provided, the exception is passed to the
     *                         callback.
     */
    public void registerDeviceAsync() {
        registerDeviceAsync((Device) null, null);
    }

    /**
     * Registers a device asynchronously with a randomly assigned ID.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param callback Callback for result of the registration.
     * @throws IobeamException Thrown if the iobeam client is not initialized AND a callback is not
     *                         provided. If a callback is provided, the exception is passed to the
     *                         callback.
     */
    public void registerDeviceAsync(RegisterCallback callback) {
        registerDeviceAsync((Device) null, callback);
    }

    /**
     * Registers a device asynchronously with the provided device ID.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param deviceId Desired device ID.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     * @deprecated Use {@link #registerDeviceAsync(String)} instead.
     */
    @Deprecated
    public void registerDeviceWithIdAsync(String deviceId) {
        registerDeviceAsync(deviceId, null);
    }

    /**
     * Registers a device asynchronously with the provided device ID.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param deviceId Desired device ID.
     * @throws IobeamException Thrown if the iobeam client is not initialized AND a callback is not
     *                         provided. If a callback is provided, the exception is passed to the
     *                         callback.
     */
    public void registerDeviceAsync(String deviceId) {
        registerDeviceAsync(deviceId, null);
    }

    /**
     * Registers a device asynchronously with the provided device ID and name.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param deviceId Desired device ID.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     * @deprecated Use {@link #registerDeviceAsync(Device)} instead. Will be removed after 0.6.x
     */
    @Deprecated
    public void registerDeviceWithIdAsync(String deviceId, String deviceName) {
        final Device d = new Device.Builder(projectId).id(deviceId).name(deviceName).build();
        registerDeviceAsync(d, null);
    }

    /**
     * Registers a device asynchronously with the provided device ID.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param deviceId Desired device ID.
     * @param callback Callback for result of the registration.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     * @deprecated Use {@link #registerDeviceAsync(String, RegisterCallback)} instead.
     */
    @Deprecated
    public void registerDeviceWithIdAsync(String deviceId, RegisterCallback callback) {
        registerDeviceAsync(deviceId, callback);
    }

    /**
     * Registers a device asynchronously with the provided device ID.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param deviceId Desired device ID.
     * @param callback Callback for result of the registration.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceAsync(String deviceId, RegisterCallback callback) {
        final Device d = new Device.Builder(projectId).id(deviceId).build();
        registerDeviceAsync(d, callback);
    }

    /**
     * Registers a device asynchronously with the provided device ID and name.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param deviceId   Desired device ID.
     * @param callback   Callback for result of the registration.
     * @param deviceName Desired device name.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     * @deprecated Use {@link #registerDeviceAsync(Device, RegisterCallback)} instead. Will be
     * removed after 0.6.x
     */
    @Deprecated
    public void registerDeviceWithIdAsync(String deviceId, String deviceName,
                                          RegisterCallback callback) {
        final Device d = new Device.Builder(projectId).id(deviceId).name(deviceName).build();
        registerDeviceAsync(d, callback);
    }

    /**
     * Registers a device asynchronously with parameters of the provided {@link Device}.
     *
     * See {@link #registerDeviceAsync(Device, RegisterCallback)} for more details.
     *
     * @param device Desired device parameters to register.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceAsync(Device device) {
        registerDeviceAsync(device, null);
    }

    public void registerOrSetDeviceAsync(final Device device) {
        registerDeviceAsync(device, new IgnoreDupeRegisterCallback(this, null));
    }

    public void registerOrSetDeviceAsync(final Device device, RegisterCallback callback) {
        registerDeviceAsync(device, new IgnoreDupeRegisterCallback(this, callback));
    }

    public void registerOrSetDeviceAsync(final String id) {
        registerOrSetDeviceAsync(new Device.Builder(projectId).id(id).build());
    }

    public void registerOrSetDeviceAsync(final String id, RegisterCallback callback) {
        registerOrSetDeviceAsync(new Device.Builder(projectId).id(id).build(), callback);
    }

    /**
     * Registers a device asynchronously with parameters of the provided {@link Device}. This will
     * not block the calling thread. If successful, the device ID of this client will be set (either
     * to the id provided, or if not provided, a randomly generated one). Any provided callback will
     * be run on a background thread. If the client already has a device ID set, registration will
     * only happen for a non-null device. Otherwise, the callback will be called with a {@link
     * Device} with the current ID.
     *
     * @param device   Desired device parameters to register.
     * @param callback Callback for result of the registration.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public void registerDeviceAsync(Device device, RegisterCallback callback) {
        RestCallback<Device> cb;
        if (callback == null) {
            cb = RegisterCallback.getEmptyCallback().getInnerCallback(this);
        } else {
            cb = callback.getInnerCallback(this);
        }

        // If device ID is set and not explicitly asking for a different one, return current ID.
        boolean alreadySet = this.deviceId != null;
        if (alreadySet && (device == null || this.deviceId.equals(device.getId()))) {
            cb.completed(device, null);
            return;
        }

        // Make sure to unset before attempting, so as not to reuse old ID if it fails.
        this.deviceId = null;

        final DeviceService.Add req;
        try {
            req = prepareDeviceRequest(device);
        } catch (ApiException e) {
            IobeamException ie = new IobeamException(e);
            if (callback == null) {
                throw ie;
            } else {
                cb.failed(ie, null);
                return;
            }
        }
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

    public void setDevice(Device device) throws CouldNotPersistException {
        setDeviceId(device.getId());
    }

    @Deprecated
    Import getDataStore() {
        return dataStore;
    }

    /**
     * Get the DataStore associated with a collection of column names.
     *
     * @param columns Collection of columns to find a corresponding DataStore for.
     * @return DataStore corresponding to the columns, or null if not found.
     */
    public DataStore getDataStore(final Collection<String> columns) {
        for (DataStore ds : dataBatches) {
            if (ds.hasColumns(columns)) {
                return ds;
            }
        }
        return null;
    }

    /**
     * Get the DataStore associated with a collection of column names, adding a new one if
     * necessary.
     *
     * @param columns Collection of columns to find a corresponding DataStore for.
     * @return DataStore corresponding to the columns.
     */
    public DataStore getOrAddDataStore(final Collection<String> columns) {
        DataStore ret = getDataStore(columns);
        if (ret == null) {
            ret = this.createDataStore(columns);
        }
        return ret;
    }

    /* A lock should always be acquired before calling this method! */
    private void _addDataWithoutLock(String seriesName, DataPoint dataPoint) {
        if (dataStore == null) {
            dataStore = new Import(deviceId, projectId);
        }
        dataStore.addDataPoint(seriesName, dataPoint);

        DataStore store = seriesToBatch.get(seriesName);
        if (store == null) {
            store = new DataStore(seriesName);
            seriesToBatch.put(seriesName, store);
            dataBatches.add(store);
        }
        store.add(dataPoint.getTime(), seriesName, dataPoint.getValue());
    }

    /**
     * Adds a data value to a particular series in the data store.
     *
     * @param seriesName The name of the series that the data belongs to.
     * @param dataPoint  The DataPoint representing a data value at a particular time.
     * @deprecated Use DataStore and `trackDataStore()` instead.
     */
    @Deprecated
    public void addData(String seriesName, DataPoint dataPoint) {
        synchronized (dataStoreLock) {
            _addDataWithoutLock(seriesName, dataPoint);
        }
    }

    /**
     * Adds a list of data points to a list of series in the data store. This is essentially a 'zip'
     * operation on the points and series names, where the first point is put in the first series,
     * the second point in the second series, etc. Both lists MUST be the same size.
     *
     * @param points      List of DataPoints to be added
     * @param seriesNames List of corresponding series for the datapoints.
     * @return True if the points are added; false if the lists are not the same size, or adding
     * fails.
     * @deprecated Use {@link DataStore} and {@link #createDataStore(Collection)} instead.
     */
    @Deprecated
    public boolean addDataMapToSeries(String[] seriesNames, DataPoint[] points) {
        if (seriesNames == null || points == null || points.length != seriesNames.length) {
            return false;
        }

        synchronized (dataStoreLock) {
            for (int i = 0; i < seriesNames.length; i++) {
                _addDataWithoutLock(seriesNames[i], points[i]);
            }
        }
        return true;
    }

    private void addBulkData(final ImportBatch data) {
        if (data == null) {
            return;
        }

        if (data.isFromLegacy()) {
            synchronized (dataStoreLock) {
                if (dataStore == null) {
                    dataStore = new Import(deviceId, projectId);
                }

                String key = data.getData().getColumns().get(0);
                DataStore db = seriesToBatch.get(key);
                db.merge(data.getData());
            }
        } else {
            final DataStore ds = getDataStore(data.getData().getColumns());
            ds.merge(data.getData());
        }
    }

    /**
     * Creates a DataStore with a given set of columns, and tracks it so that any data added will be
     * sent on a subsequent send calls.
     *
     * @param columns Columns in the DataStore
     * @return DataStore for storing data for a given set of columns.
     */
    public DataStore createDataStore(Collection<String> columns) {
        DataStore b = new DataStore(columns);
        trackDataStore(b);

        return b;
    }

    /**
     * Creates a DataStore with a given set of columns, and tracks it so that any data added will be
     * sent on a subsequent send calls.
     *
     * @param columns Columns in the DataStore
     * @return DataStore for storing data for a given set of columns.
     */
    public DataStore createDataStore(String... columns) {
        return createDataStore(Arrays.asList(columns));
    }

    /**
     * Track a DataStore so that any data stored in it will be sent on subsequent send calls.
     *
     * @param batch Data stored in a batch/table format.
     * @deprecated Use {@link #trackDataStore(DataStore)} instead.
     */
    @Deprecated
    public void trackDataBatch(DataStore batch) {
        trackDataStore(batch);
    }

    /**
     * Track a DataStore so that any data stored in it will be sent on subsequent send calls.
     *
     * @param store DataStore to be tracked by this client.
     */
    public void trackDataStore(DataStore store) {
        synchronized (dataStoreLock) {
            dataBatches.add(store);
        }
    }

    /**
     * Returns the size of all of the data in all the series.
     *
     * @return Size of the data store, or 0 if it has not been made yet.
     */
    public long getDataSize() {
        long size = 0;
        synchronized (dataStoreLock) {
            for (DataStore b : dataBatches) {
                size += b.getDataSize();
            }
        }
        return size;
    }

    /**
     * Returns the size of the data set in a particular series.
     *
     * @param series The series to query
     * @return Size of the data set, or 0 if series does not exist.
     * @deprecated Use DataStore methods instead.
     */
    @Deprecated
    public int getDataSize(String series) {
        return getSeriesSize(series);
    }

    /**
     * Returns the size of the data set in a particular series.
     *
     * @param series The series to query
     * @return Size of the data set, or 0 if series does not exist.
     */
    private int getSeriesSize(String series) {
        synchronized (dataStoreLock) {
            if (dataStore == null) {
                return 0;
            }
            if (seriesToBatch.containsKey(series)) {
                return (int) seriesToBatch.get(series).getDataSize();
            } else {
                return 0;
            }
        }
    }


    List<ImportService.Submit> prepareDataRequests() throws ApiException {
        if (!isInitialized()) {
            throw new NotInitializedException();
        }
        if (deviceId == null) {
            throw new ApiException("Device id not set, cannot send data.");
        }

        // Synchronize so no more data is added to this object while we send.
        final List<DataStore> stores;
        synchronized (dataStoreLock) {
            dataStore = null;

            if (dataBatches != null) {
                stores = new ArrayList<DataStore>(dataBatches.size());
                for (DataStore b : dataBatches) {
                    if (b.getRows().size() > 0) {
                        stores.add(DataStore.snapshot(b));
                        b.reset();
                    }
                }
            } else {
                stores = Collections.<DataStore>emptyList();
            }
        }
        // No data to send, log a warning and return an empty list.
        if (stores.size() == 0) {
            logger.warning("No data to send.");
            return new ArrayList<ImportService.Submit>();
        }

        List<ImportBatch> impBatches = new ArrayList<ImportBatch>();
        for (final DataStore store : stores) {
            boolean legacy = store.getColumns().size() == 1 &&
                             seriesToBatch.containsKey(store.getColumns().get(0));
            if (legacy) {
                impBatches.add(ImportBatch.createLegacy(projectId, deviceId, store));
            } else {
                impBatches.add(new ImportBatch(projectId, deviceId, store));
            }
        }

        ImportService service = new ImportService(client);
        return service.submit(impBatches);
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
                    ReinsertSendCallback cb = new ReinsertSendCallback(this, null);
                    cb.innerCallback.failed(e, req);
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
     * @throws IobeamException Thrown is the client is not initialized or if the device id has not
     *                         been set AND no callback set. Otherwise, the exception is passed to
     *                         the callback.
     */
    public void sendAsync() {
        sendAsync(null);
    }

    /**
     * Asynchronous version of send() that will not block the calling thread. Any provided callback
     * will be run on the background thread when the operation completes.
     *
     * If `autoRetry` is set, failed requests will add the previous data to the new data store.
     *
     * @param callback Callback for when the operation completes.
     * @throws IobeamException Thrown is the client is not initialized or if the device id has not
     *                         been set AND no callback set. Otherwise, the exception is passed to
     *                         the callback.
     */
    public void sendAsync(SendCallback callback) {
        List<ImportService.Submit> reqs;
        try {
            reqs = prepareDataRequests();
        } catch (ApiException e) {
            IobeamException ie = new IobeamException(e);
            if (callback == null) {
                throw ie;
            } else {
                callback.innerCallback.failed(ie, null);
                return;
            }
        }

        for (ImportService.Submit req : reqs) {
            if (callback == null && !autoRetry) {
                req.executeAsync();
            } else if (callback != null && !autoRetry) {
                req.executeAsync(callback.innerCallback);
            } else {
                req.executeAsync(new ReinsertSendCallback(this, callback).innerCallback);
            }
        }
    }
}
