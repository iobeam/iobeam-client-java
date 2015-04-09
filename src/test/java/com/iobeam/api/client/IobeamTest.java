package com.iobeam.api.client;

import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

public class IobeamTest {

    private static final long PROJECT_ID = 1;
    private static final String PROJECT_TOKEN = "fake_token";
    private static final String DEVICE_ID = "fake_device_identifier";
    private static final String FILE_PATH = new File(".").getAbsolutePath();

    @Before
    public void setUp() throws Exception {
        Iobeam.reset();
        removeDeviceId();
        assertFalse(Iobeam.isInitialized());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        removeDeviceId();
    }

    private static void removeDeviceId() {
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    public void testInitNoDevice() throws Exception {
        Iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertEquals(FILE_PATH, Iobeam.path);
        assertEquals(PROJECT_ID, Iobeam.projectId);
        assertEquals(PROJECT_TOKEN, Iobeam.projectToken);
        assertNull(Iobeam.deviceId);
    }

    @Test
    public void testInitGivenDevice() throws Exception {
        Iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        assertEquals(FILE_PATH, Iobeam.path);
        assertEquals(PROJECT_ID, Iobeam.projectId);
        assertEquals(PROJECT_TOKEN, Iobeam.projectToken);
        assertNotNull(Iobeam.deviceId);
        assertEquals(DEVICE_ID, Iobeam.deviceId);
    }

    @Test
    public void testInitDeviceOnDisk() throws Exception {
        // Set a device ID then reset state.
        Iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);
        Iobeam.reset();

        // Test that the persisted device ID is used.
        Iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertEquals(FILE_PATH, Iobeam.path);
        assertEquals(PROJECT_ID, Iobeam.projectId);
        assertEquals(PROJECT_TOKEN, Iobeam.projectToken);
        assertNotNull(Iobeam.deviceId);
        assertEquals(DEVICE_ID, Iobeam.deviceId);

        // Test that the one written to disk is overwritten.
        Iobeam.reset();
        final String DEVICE_ID_NEW = "thisisadifferentid";
        assertNotSame(DEVICE_ID, DEVICE_ID_NEW);
        Iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID_NEW);
        assertNotNull(Iobeam.deviceId);
        assertEquals(DEVICE_ID_NEW, Iobeam.deviceId);
    }

    @Test
    public void testSetDeviceId() throws Exception {
        File f = new File(FILE_PATH, Iobeam.DEVICE_FILENAME);

        Iobeam.init(FILE_PATH, PROJECT_ID, PROJECT_TOKEN);
        assertNull(Iobeam.deviceId);
        assertFalse(f.exists());

        Iobeam.setDeviceId(DEVICE_ID);
        assertNotNull(Iobeam.deviceId);
        assertTrue(f.exists());
        // Check that it is persisted.
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        br.close();
        assertEquals(DEVICE_ID, line);
    }

    @Test
    public void testAddData() throws Exception {
        final String SERIES = "series1";
        DataPoint d1 = new DataPoint(1000, 2000);
        Iobeam.addData(SERIES, d1);
        Import ds = Iobeam.getDataStore();
        Set<DataPoint> data = ds.getDataSeries(SERIES);
        assertEquals(1, data.size());
        assertEquals(data.size(), Iobeam.getDataSize(SERIES));
        assertTrue(data.contains(d1));
    }

    @Test
    public void testGetDataSize() throws Exception {
        final String SERIES = "series1";
        DataPoint d1 = new DataPoint(1000, 2000);
        Iobeam.addData(SERIES, d1);
        assertEquals(1, Iobeam.getDataSize(SERIES));
        DataPoint d2 = new DataPoint(2000, 4000);
        Iobeam.addData(SERIES, d2);
        assertEquals(2, Iobeam.getDataSize(SERIES));

        assertEquals(0, Iobeam.getDataSize("something_else"));
    }

    @Test
    public void testRegisterDevice() throws Exception {
        boolean error = false;
        try {
            Iobeam.registerDevice();
        } catch (Iobeam.NotInitializedException e) {
            error = true;
        }
        assertTrue(error);
    }
}