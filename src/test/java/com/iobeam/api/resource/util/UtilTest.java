package com.iobeam.api.resource.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilTest {

    @Test
    public void testNegativeTimezone() throws Exception {
        final String without = "2016-06-02T17:51:39-0100";
        final String with = "2016-06-02T17:51:39-01:00";
        assertEquals(Util.parseToDate(with), Util.parseToDate(without));
    }

    @Test
    public void testPositiveTimezone() throws Exception {
        final String without = "2016-06-02T17:51:39+0200";
        final String with = "2016-06-02T17:51:39+02:00";
        assertEquals(Util.parseToDate(with), Util.parseToDate(without));
    }

    @Test
    public void testUTCTimezone() throws Exception {
        final String withoutP = "2016-06-02T17:51:39+0000";
        final String withP = "2016-06-02T17:51:39+00:00";
        final String withoutN = "2016-06-02T17:51:39-0000";
        final String withN = "2016-06-02T17:51:39-00:00";
        final String withZ = "2016-06-02T17:51:39Z";
        assertEquals(Util.parseToDate(withP), Util.parseToDate(withoutP));
        assertEquals(Util.parseToDate(withN), Util.parseToDate(withoutN));
        assertEquals(Util.parseToDate(withP), Util.parseToDate(withN));
        assertEquals(Util.parseToDate(withP), Util.parseToDate(withZ));
    }
}
