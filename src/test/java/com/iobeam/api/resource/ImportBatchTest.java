package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;


public class ImportBatchTest {

    private static final long TEST_PROJECT_ID = 1000;
    private static final String TEST_DEVICE_ID = "test1234only5678";


    @Test
    public void testCreate() throws Exception {
        DataStore ds = new DataStore("col1", "col2");
        ImportBatch ib = new ImportBatch(TEST_PROJECT_ID, TEST_DEVICE_ID, ds);
        assertEquals(TEST_PROJECT_ID, ib.getProjectId());
        assertEquals(TEST_DEVICE_ID, ib.getDeviceId());
        assertNotNull(ib.getData());
        assertEquals(0, ib.getData().getDataSize());
        assertFalse(ib.isFromLegacy());
    }

    @Test
    public void testCreateLegacy() throws Exception {
        DataStore ds = new DataStore("col1", "col2");
        ImportBatch ib = ImportBatch.createLegacy(TEST_PROJECT_ID, TEST_DEVICE_ID, ds);
        assertEquals(TEST_PROJECT_ID, ib.getProjectId());
        assertEquals(TEST_DEVICE_ID, ib.getDeviceId());
        assertNotNull(ib.getData());
        assertEquals(0, ib.getData().getDataSize());
        assertTrue(ib.isFromLegacy());
    }

    @Test
    public void testToJson() throws Exception {
        String[] cols = {"col1", "col2"};
        DataStore ds = new DataStore(cols);
        ImportBatch ib = new ImportBatch(TEST_PROJECT_ID, TEST_DEVICE_ID, ds);
        assertEquals(TEST_PROJECT_ID, ib.getProjectId());
        assertEquals(TEST_DEVICE_ID, ib.getDeviceId());
        assertNotNull(ib.getData());
        assertEquals(0, ib.getData().getDataSize());

        ds.add(10, cols, new Object[]{100, 112});
        ds.add(20, cols, new Object[]{110, 114});
        ds.add(30, new String[]{"col1"}, new Object[]{115});
        assertEquals(6 /* 3 rows x 2 cols */, ib.getData().getDataSize());

        Map<String, Object> out = new HashMap<String, Object>();
        JSONObject json = ib.serialize(out);
        assertEquals(TEST_DEVICE_ID, json.getString("device_id"));
        assertEquals(TEST_PROJECT_ID, json.getLong("project_id"));
        assertTrue(json.get("sources") instanceof JSONObject);

        JSONObject sources = json.getJSONObject("sources");
        assertEquals(2, sources.keySet().size());
        assertTrue(sources.keySet().contains("fields"));
        assertTrue(sources.get("fields") instanceof JSONArray);
        assertTrue(sources.keySet().contains("data"));

        JSONArray fields = sources.getJSONArray("fields");
        assertEquals("time", fields.get(0));
        assertEquals("col1", fields.get(1));
        assertEquals("col2", fields.get(2));

        JSONArray data = sources.getJSONArray("data");
        assertEquals(3, data.length());
    }
}
