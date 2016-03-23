package com.iobeam.api.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.iobeam.api.resource.util.Util;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

public class DeviceTest {

    private static final String DEVICE_ID = "test1234only5678";
    private static final String DEVICE_NAME = "java_test_ex";
    private static final String DEVICE_TYPE = "java_test";
    private static final String TEST_DATE_STRING = "2015-03-01 20:55:21 -0400";
    private static final String TEST_DATE_STRING_8601 = "2015-03-01T20:55:21-04:00";

    private static final String jsonOldDevice = "{\n"
                                             + "     \"device_id\": \"" + DEVICE_ID + "\",\n"
                                             + "     \"project_id\": 1000,\n"
                                             + "     \"device_name\": \"" + DEVICE_NAME + "\",\n"
                                             + "     \"device_type\": \"java_test\",\n"
                                             + "     \"created\": \"" + TEST_DATE_STRING + "\"\n"
                                             + "}";

    private static final String jsonDevice = "{\n"
                                             + "     \"device_id\": \"" + DEVICE_ID + "\",\n"
                                             + "     \"project_id\": 1000,\n"
                                             + "     \"device_name\": \"" + DEVICE_NAME + "\",\n"
                                             + "     \"device_type\": \"java_test\",\n"
                                             + "     \"created\": \"" + TEST_DATE_STRING_8601 + "\"\n"
                                             + "}";

    @Test
    public void testFromJson() throws Exception {
        Device d = Device.fromJson(new JSONObject(jsonDevice));
        assertNotNull(d);
        assertEquals(DEVICE_ID, d.getId());
        assertEquals(1000, d.getProjectId());
        assertEquals(DEVICE_NAME, d.getName());
        assertEquals(DEVICE_TYPE, d.getType());

        Date expected = Util.parseToDate(TEST_DATE_STRING_8601);
        assertEquals(expected, d.getCreated());
    }

    @Test
    public void testFromJsonOld() throws Exception {
        Device d = Device.fromJson(new JSONObject(jsonOldDevice));
        assertNotNull(d);
        assertEquals(DEVICE_ID, d.getId());
        assertEquals(1000, d.getProjectId());
        assertEquals(DEVICE_NAME, d.getName());
        assertEquals(DEVICE_TYPE, d.getType());

        Date expected = Util.parseToDate(TEST_DATE_STRING);
        assertEquals(expected, d.getCreated());
    }

    private void verifyJson(JSONObject json, long pid, Date date) throws Exception {
        assertNotNull(json);
        assertEquals(DEVICE_ID, json.get("device_id"));
        assertEquals(pid, json.getLong("project_id"));
        assertEquals(DEVICE_NAME, json.get("device_name"));
        assertEquals(DEVICE_TYPE, json.get("device_type"));
        assertEquals(date, Util.DATE_FORMAT.parse(json.getString("created")));
    }

    @Test
    public void testSerialization() throws Exception {
        ResourceMapper mapper = new ResourceMapper();
        Date date = Util.parseToDate(TEST_DATE_STRING_8601);
        Device d = new Device(1000, new Device.Spec(DEVICE_ID, DEVICE_NAME, DEVICE_TYPE), date);
        byte[] raw = mapper.toJsonBytes(d);
        assertTrue(raw.length > 0);
        JSONObject json = new JSONObject(new String(raw, "UTF-8"));
        verifyJson(json, 1000, date);
    }

    @Test
    public void testDeprecatedSerialization() throws Exception {
        ResourceMapper mapper = new ResourceMapper();
        Date date = Util.DATE_FORMAT.parse(TEST_DATE_STRING);
        Device d = new Device(DEVICE_ID, 1000, DEVICE_NAME, DEVICE_TYPE, date);
        byte[] raw = mapper.toJsonBytes(d);
        assertTrue(raw.length > 0);
        JSONObject json = new JSONObject(new String(raw, "UTF-8"));
        verifyJson(json, 1000, date);
    }
}
