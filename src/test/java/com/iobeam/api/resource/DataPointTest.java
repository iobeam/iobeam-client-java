package com.iobeam.api.resource;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.ByteBuffer;

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
    public void testFromByteArray() throws Exception {
        long now = System.currentTimeMillis();

        DataPoint dataPointInt = new DataPoint(now, 10);
        ByteBuffer buf = ByteBuffer.wrap(dataPointInt.toByteArray());

        DataPoint reverse = DataPoint.fromByteArray(buf.array());
        assertEquals(dataPointInt, reverse);

        DataPoint dataPointDouble = new DataPoint(now, 10.5);
        buf = ByteBuffer.wrap(dataPointDouble.toByteArray());
        reverse = DataPoint.fromByteArray(buf.array());
        assertEquals(dataPointDouble, reverse);

        DataPoint dataPointString = new DataPoint(now, "11.2");
        buf = ByteBuffer.wrap(dataPointString.toByteArray());
        reverse = DataPoint.fromByteArray(buf.array());
        assertEquals(dataPointString, reverse);
    }

    @Test
    public void testToByteArray() throws Exception {
        long now = System.currentTimeMillis();

        DataPoint dataPointInt = new DataPoint(now, 10);
        ByteBuffer buf = ByteBuffer.wrap(dataPointInt.toByteArray());
        assertEquals(now, buf.getLong());
        buf.get();  // skip type
        assertEquals(10, buf.getLong());

        DataPoint dataPointDouble = new DataPoint(now, 10.5);
        buf = ByteBuffer.wrap(dataPointDouble.toByteArray());
        assertEquals(now, buf.getLong());
        buf.get();  // skip type
        assertEquals(10.5, buf.getDouble(), .1);

        DataPoint dataPointString = new DataPoint(now, "11.2");
        buf = ByteBuffer.wrap(dataPointString.toByteArray());
        assertEquals(now, buf.getLong());
        buf.get();  // skip type
        int strSize = buf.getInt();
        byte[] temp = new byte[strSize];
        buf.get(temp, 0, strSize);
        assertEquals("11.2", new String(temp));
    }
}
