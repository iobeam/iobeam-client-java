package com.iobeam.api.resource.util;

import com.iobeam.api.resource.DataPoint;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataPointParserTest {

    private static final String csvInts = "1,10,100,1000,";
    private static final String otherReals = "1.0&2&5.0&10.";
    private static final float[] floats = {1.1f, 1.2f, 1.3f};
    private static final double[] doubles = {2.1, 2.2, 2.3};
    private static final long[] longs = {1, 2, 3};
    private static final int[] ints = {4, 5, 6};

    @Test
    public void testCsvIntParse() throws Exception {
        long ts = 1001;
        List<DataPoint> list = DataPointParser.parse(csvInts, ",", Long.class, ts);
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
        List<DataPoint> list = DataPointParser.parse(otherReals, "&", Double.class, ts);
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
        List<DataPoint> list = DataPointParser.parse(csvInts, ",", List.class);
        assertEquals(0, list.size());
    }

    @Test
    public void testFloatsParse() throws Exception {
        long ts = 2001;
        List<DataPoint> list = DataPointParser.parse(floats, ts);
        assertEquals(floats.length, list.size());
        for (int i = 0; i < floats.length; i++) {
            assertEquals(ts, list.get(i).getTime());
            assertEquals(floats[i], (Double) list.get(i).getValue(), .01);
        }
    }

    @Test
    public void testDoublesParse() throws Exception {
        long ts = 2001;
        List<DataPoint> list = DataPointParser.parse(doubles, ts);
        assertEquals(doubles.length, list.size());
        for (int i = 0; i < doubles.length; i++) {
            assertEquals(ts, list.get(i).getTime());
            assertEquals(doubles[i], (Double) list.get(i).getValue(), .01);
        }
    }

    @Test
    public void testLongsParse() throws Exception {
        long ts = 2001;
        List<DataPoint> list = DataPointParser.parse(longs, ts);
        assertEquals(longs.length, list.size());
        for (int i = 0; i < longs.length; i++) {
            assertEquals(ts, list.get(i).getTime());
            assertEquals(longs[i], ((Long) list.get(i).getValue()).longValue());
        }
    }

    @Test
    public void testIntsParse() throws Exception {
        long ts = 2001;
        List<DataPoint> list = DataPointParser.parse(ints, ts);
        assertEquals(ints.length, list.size());
        for (int i = 0; i < ints.length; i++) {
            assertEquals(ts, list.get(i).getTime());
            assertEquals(ints[i], ((Long) list.get(i).getValue()).longValue());
        }
    }
}
