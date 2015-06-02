package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImportTest {

    private static final String TEST_DEVICE_ID = "test1234only5678";

    private static final String jsonDataPoint = "{\n"
                                                + "     \"time\": 123456789,\n"
                                                + "     \"value\": 100\n"
                                                + "}";

    @Test
    public void testCreate() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        assertNotNull(imp);
        assertEquals(TEST_DEVICE_ID, imp.getDeviceId());
        assertEquals(1000, imp.getProjectId());
        assertNotNull(imp.getSeries());
        assertEquals(0, imp.getSeries().size());
    }

    @Test
    public void testAddDataPoints() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        assertNotNull(imp.getSeries());
        assertEquals(0, imp.getSeries().size());

        DataPoint d1 = new DataPoint(10, 100);
        DataPoint d2 = new DataPoint(20, 110);
        DataPoint d3 = new DataPoint(30, 115);
        imp.addDataPoint("series1", d1);
        assertNotNull(imp.getDataSeries("series1"));
        assertEquals(1, imp.getDataSeries("series1").size());

        imp.addDataPoint("series1", d2);
        imp.addDataPoint("series1", d3);
        assertEquals(3, imp.getDataSeries("series1").size());
        assertEquals(1, imp.getSeries().size());
        assertTrue(imp.getDataSeries("series1").contains(d1));
        assertTrue(imp.getDataSeries("series1").contains(d2));
        assertTrue(imp.getDataSeries("series1").contains(d3));

        DataPoint d4 = new DataPoint(10, 112);
        DataPoint d5 = new DataPoint(20, 114);
        imp.addDataPoint("series2", d4);
        imp.addDataPoint("series2", d5);
        assertEquals(2, imp.getSeries().size());
        assertEquals(2, imp.getDataSeries("series2").size());
        assertTrue(imp.getDataSeries("series2").contains(d4));
        assertTrue(imp.getDataSeries("series2").contains(d5));
    }

    @Test
    public void testAddDataSet() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        assertNotNull(imp.getSeries());
        assertEquals(0, imp.getSeries().size());

        Set<DataPoint> set1 = new HashSet<DataPoint>();
        DataPoint d1 = new DataPoint(10, 100);
        DataPoint d2 = new DataPoint(20, 110);
        DataPoint d3 = new DataPoint(30, 115);
        set1.add(d1);
        set1.add(d2);
        set1.add(d3);
        imp.addDataPointSet("series1", set1);
        assertNotNull(imp.getDataSeries("series1"));
        assertEquals(3, imp.getDataSeries("series1").size());
        assertEquals(set1, imp.getDataSeries("series1"));
    }

    @Test
    public void testToJson() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        assertNotNull(imp.getSeries());
        assertEquals(0, imp.getSeries().size());

        DataPoint d1 = new DataPoint(10, 100);
        DataPoint d2 = new DataPoint(20, 110);
        DataPoint d3 = new DataPoint(30, 115);
        imp.addDataPoint("series1", d1);
        imp.addDataPoint("series1", d2);
        imp.addDataPoint("series1", d3);

        DataPoint d4 = new DataPoint(10, 112);
        DataPoint d5 = new DataPoint(20, 114);
        imp.addDataPoint("series2", d4);
        imp.addDataPoint("series2", d5);

        Map<String, Object> out = new HashMap<String, Object>();
        JSONObject json = imp.serialize(out);
        assertEquals(TEST_DEVICE_ID, json.getString("device_id"));
        assertEquals(1000, json.getLong("project_id"));
        assertTrue(json.get("sources") instanceof JSONArray);

        JSONArray sources = json.getJSONArray("sources");
        assertEquals(2, sources.length());
        assertTrue(sources.get(0) instanceof JSONObject);
        assertTrue(sources.get(1) instanceof JSONObject);

        JSONObject series1 = sources.getJSONObject(0);
        assertEquals("series1", series1.getString("name"));
        assertTrue(series1.get("data") instanceof JSONArray);
        assertEquals(3, series1.getJSONArray("data").length());

        JSONObject series2 = sources.getJSONObject(1);
        assertEquals("series2", series2.getString("name"));
        assertTrue(series2.get("data") instanceof JSONArray);
        assertEquals(2, series2.getJSONArray("data").length());
    }
}
