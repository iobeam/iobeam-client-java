package com.iobeam.api.client;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.DataStore;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;
import com.iobeam.api.service.ImportService;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Set;

public class IobeamTest {

    private static final long PROJECT_ID = 1;
    private static final String PROJECT_TOKEN = "fake_token";
    private static final String DEVICE_ID = "fake_device_identifier";
    private static final String FILE_PATH = new File(".").getAbsolutePath();

    private static Iobeam.Builder getBuilder() {
        return new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN);
    }

    @Test
    public void testBuilderNoDeviceSave() throws Exception {
        Iobeam iobeam = getBuilder().saveIdToPath(FILE_PATH).build();
        assertNotNull(iobeam.path);
        assertEquals(FILE_PATH, iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNull(iobeam.deviceId);
        assertFalse(iobeam.getAutoRetry());

        iobeam.reset();
    }

    @Deprecated  // remove when constructors are removed
    @Test
    public void testConstructorNoDeviceSave() throws Exception {
        Iobeam iobeam = new Iobeam(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertNotNull(iobeam.path);
        assertEquals(FILE_PATH, iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNull(iobeam.deviceId);
        assertFalse(iobeam.getAutoRetry());

        iobeam.reset();
    }

    @Test
    public void testBuilderNoDeviceNoSave() throws Exception {
        Iobeam iobeam = getBuilder().build();
        assertNull(iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNull(iobeam.deviceId);
        assertFalse(iobeam.getAutoRetry());
    }

    @Deprecated  // remove when constructors are removed
    @Test
    public void testConstructorNoDeviceNoSave() throws Exception {
        Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN);
        assertNull(iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNull(iobeam.deviceId);
    }

    @Test
    public void testBuilderDeviceNoSave() throws Exception {
        Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
        assertNull(iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID, iobeam.deviceId);

        // Should not be on disk
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);
        assertFalse(f.exists());
    }

    @Deprecated  // remove when constructors are removed
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
    public void testBuilderInitDeviceWithDisk() throws Exception {
        // Set a device ID then reset state.
        Iobeam iobeam = getBuilder().saveIdToPath(FILE_PATH).setDeviceId(DEVICE_ID).build();
        assertNotNull(iobeam.path);
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);
        assertTrue(f.exists());
        iobeam.reset(false);  // simulates app being closed, 'false' keeps ID on disk

        // Test that the persisted device ID is used.
        iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, null, Iobeam.DEFAULT_API_URL);
        assertEquals(FILE_PATH, iobeam.path);
        assertEquals(PROJECT_ID, iobeam.projectId);
        assertEquals(PROJECT_TOKEN, iobeam.projectToken);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID, iobeam.deviceId);

        // New iobeam object uses disk ID
        Iobeam iobeam2 = getBuilder().saveIdToPath(FILE_PATH).build();
        assertNotNull(iobeam2.path);
        assertEquals(FILE_PATH, iobeam2.path);
        assertEquals(DEVICE_ID, iobeam2.deviceId);

        // Test that the one written to disk is overwritten.
        iobeam.reset();
        final String DEVICE_ID_NEW = "thisisadifferentid";
        assertNotEquals(DEVICE_ID, DEVICE_ID_NEW);
        iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID_NEW, Iobeam.DEFAULT_API_URL);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID_NEW, iobeam.deviceId);

        iobeam2.reset();
    }

    @Deprecated  // remove when constructors are removed
    @Test
    public void testInitDeviceWithDisk() throws Exception {
        // Set a device ID then reset state.
        Iobeam iobeam = new Iobeam(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        assertNotNull(iobeam.path);
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);
        assertTrue(f.exists());
        iobeam.reset(false);  // simulates app being closed, 'false' keeps ID on disk

        // Test that the persisted device ID is used.
        iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, null, Iobeam.DEFAULT_API_URL);
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
        iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID_NEW, Iobeam.DEFAULT_API_URL);
        assertNotNull(iobeam.deviceId);
        assertEquals(DEVICE_ID_NEW, iobeam.deviceId);

        iobeam2.reset();
    }

    @Test
    public void testSetAutoRetry() throws Exception {
        Iobeam iobeam = getBuilder().build();
        assertFalse(iobeam.getAutoRetry());
        iobeam.setAutoRetry(true);
        assertTrue(iobeam.getAutoRetry());
        iobeam.setAutoRetry(false);
        assertFalse(iobeam.getAutoRetry());

        iobeam = getBuilder().autoRetry().build();
        assertTrue(iobeam.getAutoRetry());
        iobeam.setAutoRetry(false);
        assertFalse(iobeam.getAutoRetry());
        iobeam.setAutoRetry(true);
        assertTrue(iobeam.getAutoRetry());
    }

    @Test
    public void testSetDeviceIdNoPersist() throws Exception {
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);

        // Note useDisk = false
        Iobeam iobeam = getBuilder().build();
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
        Iobeam iobeam = getBuilder().saveIdToPath(FILE_PATH).build();
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

        Iobeam iobeam = getBuilder().build();
        iobeam.addData(SERIES, d1);
        Import ds = iobeam.getDataStore();
        Set<DataPoint> data = ds.getDataSet(SERIES);
        assertEquals(1, data.size());
        assertEquals(data.size(), iobeam.getDataSize(SERIES));
        assertTrue(data.contains(d1));
    }

    @Test
    public void testAddDataToSeries() throws Exception {
        String[] seriesNames = {"series1", "series2", "series3"};
        DataPoint[] datapoints = {
            new DataPoint(1000, 2000),
            new DataPoint(2000, 3000),
            new DataPoint(3000, 4000)
        };
        Iobeam iobeam = getBuilder().build();
        assertTrue(iobeam.addDataMapToSeries(seriesNames, datapoints));

        Import ds = iobeam.getDataStore();
        for (int i = 0; i < seriesNames.length; i++ ) {
            Set<DataPoint> data = ds.getDataSet(seriesNames[i]);
            assertEquals(1, data.size());
            assertEquals(data.size(), iobeam.getDataSize(seriesNames[i]));
            assertTrue(data.contains(datapoints[i]));
        }
    }

    @Test
    public void testAddDataToSeriesFail() throws Exception {
        String[] seriesNames = {"series1", "series2"};
        DataPoint[] datapoints = {
            new DataPoint(1000, 2000),
            new DataPoint(2000, 3000),
            new DataPoint(3000, 4000)
        };
        Iobeam iobeam = getBuilder().build();
        // both null
        assertFalse(iobeam.addDataMapToSeries(null, null));
        // one null
        assertFalse(iobeam.addDataMapToSeries(null, datapoints));
        assertFalse(iobeam.addDataMapToSeries(seriesNames, null));
        // unequal size
        assertFalse(iobeam.addDataMapToSeries(seriesNames, datapoints));
    }

    @Test
    public void testGetTotalSize() throws Exception {
        final String SERIES = "series1";
        final String SERIES2 = "series2";
        Iobeam iobeam = getBuilder().build();

        iobeam.addData(SERIES, new DataPoint(0l, 0));
        iobeam.addData(SERIES, new DataPoint(1l, 1000));
        assertEquals(2, iobeam.getDataSize(SERIES));
        iobeam.addData(SERIES2, new DataPoint(2l, 2000));
        assertEquals(1, iobeam.getDataSize(SERIES2));
        assertEquals(3, iobeam.getDataSize());
    }

    @Test
    public void testGetDataSize() throws Exception {
        final String SERIES = "series1";
        Iobeam iobeam = getBuilder().build();

        // first point
        DataPoint d1 = new DataPoint(1000, 2000);
        iobeam.addData(SERIES, d1);
        assertEquals(1, iobeam.getDataSize(SERIES));
        // same point, no change
        iobeam.addData(SERIES, d1);
        assertEquals(1, iobeam.getDataSize(SERIES));
        // new point, +1
        DataPoint d2 = new DataPoint(2000, 4000);
        iobeam.addData(SERIES, d2);
        assertEquals(2, iobeam.getDataSize(SERIES));

        // series doesn't exist
        assertEquals(0, iobeam.getDataSize("something_else"));
    }

    @Test
    public void testRegisterDeviceError() throws Exception {
        boolean error = false;
        Iobeam iobeam = new Iobeam.Builder(0, null).build();

        try {
            iobeam.registerDevice();
        } catch (Iobeam.NotInitializedException e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public synchronized void testRegisterSameIdSync() throws Exception {
        Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
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
        Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
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
        Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
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
        Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
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
        Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
        iobeam.send();
        iobeam.send();
        assertTrue(true);
    }

    @Test
    public void testPrepareDataRequests() throws Exception {
        final Iobeam iobeam = getBuilder().setDeviceId(DEVICE_ID).build();
        final DataStore ds = iobeam.createDataStore("col1", "col2");
        ds.add("col1", 5);

        List<ImportService.Submit> batches = iobeam.prepareDataRequests();
        assertEquals(1, batches.size());
        ImportBatch b = (ImportBatch) batches.get(0).getBuilder().getContent();
        assertFalse(b.isFromLegacy());

        iobeam.addData("test", new DataPoint(10));
        batches = iobeam.prepareDataRequests();
        assertEquals(1, batches.size());
        boolean legacy = false;
        for (final ImportService.Submit s : batches) {
            b = (ImportBatch) s.getBuilder().getContent();
            if (b.getData().getColumns().size() == 1) {
                assertTrue(b.isFromLegacy());
                legacy = true;
            } else {
                assertFalse(b.isFromLegacy());
            }
        }
        assertTrue(legacy);
    }
}
