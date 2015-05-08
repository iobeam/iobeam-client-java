package com.iobeam.api.resource;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataPointTest {

    private static final String jsonDataPoint = "{\n"
                                                + "     \"time\": 123456789,\n"
                                                + "     \"value\": 100\n"
                                                + "}";


    private static final String jsonDataPoint2 = "{\n"
                                                 + "     \"time\": 123456789,\n"
                                                 + "     \"value\": 100.51\n"
                                                 + "}";


    private static final String jsonDataPoint3 = "{\n"
                                                 + "     \"time\": 123456789,\n"
                                                 + "     \"value\": \"110\"\n"
                                                 + "}";

    private static final String csvInts = "1,10,100,1000,";
    private static final String otherReals = "1.0&2&5.0&10.";

    @Test
    public void testFromJson() throws Exception {
        DataPoint dataPoint = DataPoint.fromJson(new JSONObject(jsonDataPoint));
        assertEquals(123456789, dataPoint.getTime());
        Object val = dataPoint.getValue();
        assertTrue(val instanceof Long);
        assertEquals(100, ((Long) val).longValue());

        dataPoint = DataPoint.fromJson(new JSONObject(jsonDataPoint2));
        assertEquals(123456789, dataPoint.getTime());
        val = dataPoint.getValue();
        assertTrue(val instanceof Double);
        assertEquals(100.51, ((Double) val).doubleValue(), .05);

        dataPoint = DataPoint.fromJson(new JSONObject(jsonDataPoint3));
        assertEquals(123456789, dataPoint.getTime());
        val = dataPoint.getValue();
        assertTrue(val instanceof String);
        assertEquals("110", val);

    }

    @Test
    public void testToJson() throws Exception {
        long now = System.currentTimeMillis();

        DataPoint dataPointInt = new DataPoint(now, 10);
        JSONObject json = dataPointInt.toJson();
        assertEquals(now, json.getLong("time"));
        assertEquals(10, json.getInt("value"));

        DataPoint dataPointDouble = new DataPoint(now, 10.5);
        json = dataPointDouble.toJson();
        assertEquals(now, json.getLong("time"));
        assertEquals(10.5, json.getDouble("value"), .1);

        DataPoint dataPointString = new DataPoint(now, "11.2");
        json = dataPointString.toJson();
        assertEquals(now, json.getLong("time"));
        assertEquals("11.2", json.getString("value"));
    }

    @Test
    public void testCsvIntParse() throws Exception {
        long ts = 1001;
        List<DataPoint> list = DataPoint.parseDataPoints(csvInts, ",", Long.class, ts);
        assertEquals(4, list.size());
        for (DataPoint d : list) {
            assertEquals(ts, d.getTime());
        }
        assertEquals(1l, list.get(0).getValue());
        assertEquals(10l, list.get(1).getValue());
        assertEquals(100l, list.get(2).getValue());
        assertEquals(1000l, list.get(3).getValue());
    }

    @Test
    public void testRealParse() throws Exception {
        long ts = 2001;
        List<DataPoint> list = DataPoint.parseDataPoints(otherReals, "&", Double.class, ts);
        assertEquals(4, list.size());
        for (DataPoint d : list) {
            assertEquals(ts, d.getTime());
        }
        assertEquals(1d, list.get(0).getValue());
        assertEquals(2d, list.get(1).getValue());
        assertEquals(5d, list.get(2).getValue());
        assertEquals(10d, list.get(3).getValue());
    }

    @Test
    public void testInvalidTypeParse() throws Exception {
        // Ideally, this call would be rejected as List is not an appropriate type, but we'll
        // leave that as a TODO.
        List<DataPoint> list = DataPoint.parseDataPoints(csvInts, ",", List.class);
        assertEquals(0, list.size());
    }
}