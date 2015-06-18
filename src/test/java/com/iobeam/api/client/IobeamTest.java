package com.iobeam.api.client;

import com.iobeam.api.auth.AbstractAuthHandler;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

public class IobeamTest {

    private static final long PROJECT_ID = 1;
    private static final String PROJECT_TOKEN = "fake_token";
    private static final String DEVICE_ID = "fake_device_identifier";
    private static final String FILE_PATH = new File(".").getAbsolutePath();

    private static Iobeam iobeam;

    @BeforeClass
    public static void setUpIobeam() throws Exception {
        iobeam = new Iobeam(null, 0, null);
        Logger.getLogger(AbstractAuthHandler.class.getName()).setLevel(Level.OFF);
        Logger.getLogger(RestClient.class.getName()).setLevel(Level.SEVERE);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        iobeam.reset();
        iobeam = null;
    }

    @Before
    public void setUp() throws Exception {
        iobeam.reset();
        assertFalse(iobeam.isInitialized());
    }

    @Test
    public void testConstructorNoDeviceSave() throws Exception {
        Iobeam iobeam = new Iobeam(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertNotNull(iobeam.path);
        assertEquals(FILE_PATH, iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNull(iobeam.deviceId);

        iobeam.reset();
    }

    @Test
    public void testConstructorNoDeviceNoSave() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN);
        assertNull(iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNull(iobeam.deviceId);
    }

    @Test
    public void testConstructorDeviceNoSave() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        assertNull(iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID, iobeam.deviceId);

        // Should not be on disk
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);
        assertFalse(f.exists());
    }

    @Test
    public void testInitDeviceWithDisk() throws Exception {
        // Set a device ID then reset state.
        Iobeam iobeam = new Iobeam(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        assertNotNull(iobeam.path);
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);
        assertTrue(f.exists());
        iobeam.reset(false);  // simulates app being closed, 'false' keeps ID on disk

        // Test that the persisted device ID is used.
        iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, null);
        assertEquals(FILE_PATH, iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID, iobeam.deviceId);

        // New iobeam object uses disk ID
        Iobeam iobeam2 = new Iobeam(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertNotNull(iobeam2.path);
        assertEquals(FILE_PATH, iobeam2.path);
        assertEquals(DEVICE_ID, iobeam2.deviceId);

        // Test that the one written to disk is overwritten.
        iobeam.reset();
        final String DEVICE_ID_NEW = "thisisadifferentid";
        assertNotEquals(DEVICE_ID, DEVICE_ID_NEW);
        iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID_NEW);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID_NEW, iobeam.deviceId);

        iobeam2.reset();
    }

    @Test
    public void testSetAutoRetry() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN);
        assertFalse(iobeam.getAutoRetry());
        iobeam.setAutoRetry(true);
        assertTrue(iobeam.getAutoRetry());
        iobeam.setAutoRetry(false);
        assertFalse(iobeam.getAutoRetry());
    }

    @Test
    public void testSetDeviceIdNoPersist() throws Exception {
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);

        // Note useDisk = false
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN);
        assertNull(iobeam.path);
        assertNull(iobeam.deviceId);
        assertFalse(f.exists());

        // Since useDisk = false in constructor, deviceId should NOT be on disk.
        iobeam.setDeviceId(DEVICE_ID);
        assertNotNull(iobeam.deviceId);
        assertFalse(f.exists());
    }

    @Test
    public void testSetDeviceIdPersist() throws Exception {
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);

        // Note useDisk = true
        Iobeam iobeam = new Iobeam(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertNull(iobeam.deviceId);
        assertFalse(f.exists());

        iobeam.setDeviceId(DEVICE_ID);
        assertNotNull(iobeam.deviceId);
        assertTrue(f.exists());

        // Check that it is persisted.
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        br.close();
        assertEquals(DEVICE_ID, line);

        iobeam.reset();
    }

    @Test
    public void testAddData() throws Exception {
        final String SERIES = "series1";
        DataPoint d1 = new DataPoint(1000, 2000);
        iobeam.addData(SERIES, d1);
        Import ds = iobeam.getDataStore();
        Set<DataPoint> data = ds.getDataSeries(SERIES);
        assertEquals(1, data.size());
        assertEquals(data.size(), iobeam.getDataSize(SERIES));
        assertTrue(data.contains(d1));
    }

    @Test
    public void testGetDataSize() throws Exception {
        final String SERIES = "series1";
        DataPoint d1 = new DataPoint(1000, 2000);
        iobeam.addData(SERIES, d1);
        assertEquals(1, iobeam.getDataSize(SERIES));
        DataPoint d2 = new DataPoint(2000, 4000);
        iobeam.addData(SERIES, d2);
        assertEquals(2, iobeam.getDataSize(SERIES));

        assertEquals(0, iobeam.getDataSize("something_else"));
    }

    @Test
    public void testRegisterDeviceError() throws Exception {
        boolean error = false;
        try {
            iobeam.registerDevice();
        } catch (Iobeam.NotInitializedException e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public synchronized void testRegisterSameIdSync() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        String prev = iobeam.getDeviceId();
        assertNotNull(prev);
        assertEquals(prev, DEVICE_ID);
        long start = System.currentTimeMillis();
        String now = iobeam.registerDeviceWithId(DEVICE_ID);
        long timed = System.currentTimeMillis() - start;
        assertNotNull(now);
        assertEquals(prev, now);
        assertTrue(timed < 10);  // Should not hit the network; heuristic.
    }

    @Test
    public synchronized void testRegisterSameIdAsync() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        String prev = iobeam.getDeviceId();
        assertNotNull(prev);
        assertEquals(DEVICE_ID, prev);
        long start = System.currentTimeMillis();
        iobeam.registerDeviceWithIdAsync(DEVICE_ID);
        long timed = System.currentTimeMillis() - start;
        String now = iobeam.getDeviceId();
        assertNotNull(now);
        assertEquals(DEVICE_ID, now);
        assertTrue(timed < 10);  // Should not hit the network; heuristic.

        final long restart = System.currentTimeMillis();
        RegisterCallback cb = new RegisterCallback() {
            @Override
            public void onSuccess(String deviceId) {
                long timed = System.currentTimeMillis() - restart;
                assertEquals(DEVICE_ID, deviceId);
                assertTrue(timed < 10);  // Should not hit the network; heuristic.
            }

            @Override
            public void onFailure(Throwable exc, RestRequest req) {
                assertTrue(false);
            }
        };
        iobeam.registerDeviceWithIdAsync(DEVICE_ID, cb);
        assertEquals(prev, iobeam.getDeviceId());
    }

    @Test
    public synchronized void testRegisterDifferentIdSync() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        final String prev = iobeam.getDeviceId();
        assertNotNull(prev);
        assertEquals(prev, DEVICE_ID);

        // Will hit the network, but credentials are invalid anyway.
        try {
            iobeam.registerDeviceWithId("new_device_id");
        } catch (Exception e) {
            // expected, ignore.
        }
        assertNull(iobeam.getDeviceId());
    }

    @Test
    public synchronized void testRegisterDifferentIdAsync() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        final String prev = iobeam.getDeviceId();
        assertNotNull(prev);
        assertEquals(prev, DEVICE_ID);


        iobeam.registerDeviceWithIdAsync("new_device_id");
        // Should be reset before request goes out anyway.
        assertNull(iobeam.getDeviceId());
    }

    // Added to make sure empty data sets are handled correctly; previously threw a NullPointer
    @Test
    public synchronized void testEmptySend() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        iobeam.send();
        iobeam.send();
        assertTrue(true);
    }
}
