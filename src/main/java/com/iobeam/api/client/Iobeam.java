package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.auth.AuthHandler;
import com.iobeam.api.auth.DefaultAuthHandler;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.service.Devices;
import com.iobeam.api.service.Imports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * The Iobeam client. An instance of this is initialized with <tt>init()</tt> for a particular
 * project. This instance is passed to services (for example, Imports).
 */
public class Iobeam {

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

    static String path = null;
    static long projectId = -1;
    static String projectToken = null;
    static String deviceId = null;

    private static RestClient client = null;

    private static final Object dataStoreLock = new Object();
    private static Import dataStore;

    /**
     * Tells whether the iobeam client has been initialized.
     *
     * @return True if initialized; otherwise, false.
     */
    public static boolean isInitialized() {
        return path != null && projectId > 0 && projectToken != null && client != null;
    }

    /**
     * Initialize the iobeam client without a specified device ID (either read off disk or a new one
     * will need to be acquired.
     *
     * @param path         Directory where the device ID file, iobeam-device-id, should be written.
     * @param projectId    The numeric project ID to associate with.
     * @param projectToken The token to use when communicating with iobeam cloud.
     * @throws ApiException Thrown if something goes wrong with initializing the device ID.
     */
    public static void init(String path, long projectId, String projectToken) throws ApiException {
        init(path, projectId, projectToken, null);
    }

    /**
     * Initializes the iobeam client.
     *
     * @param path         Directory where the device ID file, iobeam-device-id, should be written.
     * @param projectId    The numeric project ID to associate with.
     * @param projectToken The token to use when communicating with iobeam cloud.
     * @param deviceId     The device ID that should be used by the iobeam client.
     * @throws ApiException Thrown if something goes wrong with initializing the device ID.
     */
    public static void init(String path, long projectId, String projectToken, String deviceId)
        throws ApiException {
        Iobeam.path = path;
        Iobeam.projectId = projectId;
        Iobeam.projectToken = projectToken;
        if (deviceId == null) {
            deviceId = localDeviceIdCheck();
        }
        setDeviceId(deviceId);

        AuthHandler handler = new DefaultAuthHandler(projectId, projectToken);
        client = new RestClient(API_URL, null, Executors.newSingleThreadExecutor());
        client.setAuthenticationHandler(handler);
    }

    /**
     * Resets the iobeam client to an uninitialized state including clearing all added data.
     */
    public static void reset() {
        reset(true);
    }

    /**
     * Resets the iobeam client to uninitialized state, including removing any added data.
     *
     * @param deleteFile Whether or not to delete the on-disk device ID. Tests use false sometimes.
     */
    static void reset(boolean deleteFile) {
        String path = Iobeam.path;
        Iobeam.path = null;
        Iobeam.projectId = -1;
        Iobeam.projectToken = null;
        Iobeam.deviceId = null;

        Iobeam.client = null;

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

    private static void persistDeviceId() throws CouldNotPersistException {
        File f = new File(Iobeam.path, DEVICE_FILENAME);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(Iobeam.deviceId);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CouldNotPersistException();
        }
    }

    private static String localDeviceIdCheck() {
        File f = new File(Iobeam.path, DEVICE_FILENAME);
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

    private static Devices.Add prepareDeviceRequest() throws NotInitializedException {
        if (!isInitialized()) {
            throw new NotInitializedException();
        }
        Devices service = new Devices(client);
        return service.add(projectId, deviceId, null, null, null);
    }

    /**
     * Registers this device and gets its device id. This call is <b>BLOCKING</b> and should not be
     * called on UI threads. It will make a network call and not return until it succeeds.
     *
     * @return The new device id for this device.
     * @throws ApiException Thrown if the iobeam client is not initialized or there are problems
     *                      writing the device ID.
     * @throws IOException  Thrown if network errors occur while trying to register.
     */
    public static String registerDevice() throws ApiException, IOException {
        Devices.Add req = prepareDeviceRequest();
        String id = req.execute().getId();
        Iobeam.deviceId = id;
        persistDeviceId();
        return id;
    }


    /**
     * Registers this device and gets its device id in an asynchronous fashion. This will not block
     * the calling thread.
     *
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public static void registerDeviceAsync() throws ApiException {
        registerDeviceAsync(null);
    }

    /**
     * Registers this device and gets its device id in an asynchronous fashion. This will not block
     * the calling thread. A callback is provided to let the caller know when this operation has
     * completed, but the callback code will be running on the background thread.
     *
     * @param callback Callback to call after device is registered.
     * @throws ApiException Thrown if the iobeam client is not initialized.
     */
    public static void registerDeviceAsync(RegisterCallback callback) throws ApiException {
        Devices.Add req = prepareDeviceRequest();
        if (callback == null) {
            req.executeAsync(RegisterCallback.getEmptyCallback().innerCallback);
        } else {
            req.executeAsync(callback.innerCallback);
        }
    }

    /**
     * Sets the current device id that the iobeam client is associated with.
     *
     * @param deviceId Device id to be associated with the iobeam client.
     * @throws CouldNotPersistException Thrown if there are problems saving the device id to disk.
     */
    public static void setDeviceId(String deviceId) throws CouldNotPersistException {
        Iobeam.deviceId = deviceId;
        if (deviceId != null) {
            persistDeviceId();
        }
    }

    /**
     * Gets the current device id.
     *
     * @return The current device id.
     */
    public static String getDeviceId() {
        return Iobeam.deviceId;
    }

    static Import getDataStore() {
        return dataStore;
    }

    /**
     * Adds a data value to a particular series in the data store.
     *
     * @param seriesName The name of the series that the data belongs to.
     * @param dataPoint  The DataPoint representing a data value at a particular time.
     */
    public static void addData(String seriesName, DataPoint dataPoint) {
        synchronized (dataStoreLock) {
            if (dataStore == null) {
                dataStore = new Import(deviceId, projectId);
            }
            dataStore.addDataPoint(seriesName, dataPoint);
        }
    }

    /**
     * Returns the size of the data set in a particular series.
     *
     * @param series The series to query
     * @return Size of the data set, or 0 if series does not exist.
     */
    public static int getDataSize(String series) {
        synchronized (dataStoreLock) {
            if (dataStore == null) {
                return 0;
            }
            Set<DataPoint> set = dataStore.getDataSeries(series);
            return set == null ? 0 : set.size();
        }
    }

    private static Imports.Submit prepareDataRequest() throws ApiException {
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
        data.setDeviceId(deviceId);

        Imports service = new Imports(client);
        return service.submit(data);
    }

    /**
     * Sends the stored data to the iobeam cloud. This call is <b>BLOCKING</b>, which means the
     * caller thread will not return until the network call is complete. Do not call on UI threads.
     * This call will take a snapshot of the data store at this moment and send it; future data adds
     * will be to a NEW data store.
     *
     * @throws ApiException Thrown is the client is not initialized or if the device id has not been
     *                      set.
     * @throws IOException  Thrown if there are network issues connecting to iobeam cloud.
     */
    public static void send() throws ApiException, IOException {
        Imports.Submit req = prepareDataRequest();
        req.execute();
    }

    /**
     * Asynchronous version of send() that will not block the calling thread. No callback provided,
     * same as calling sendAsync(null).
     *
     * @throws ApiException Thrown is the client is not initialized or if the device id has not been
     *                      set.
     */
    public static void sendAsync() throws ApiException {
        sendAsync(null);
    }

    /**
     * Asynchronous version of send() that will not block the calling thread. Any provided callback
     * will be run on the background thread when the operation completes.
     *
     * @param callback Callback for when the operation completes.
     * @throws ApiException Thrown is the client is not initialized or if the device id has not been
     *                      set.
     */
    public static void sendAsync(DataCallback callback) throws ApiException {
        Imports.Submit req = prepareDataRequest();
        if (callback == null) {
            req.executeAsync();
        } else {
            req.executeAsync(callback.innerCallback);
        }
    }
}
