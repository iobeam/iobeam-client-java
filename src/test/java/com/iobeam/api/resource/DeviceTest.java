package com.iobeam.api.resource;

import com.iobeam.api.resource.util.Util;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DeviceTest {

    private static final String TEST_DEVICE_ID = "test1234only5678";
    private static final String TEST_DATE_STRING = "2015-03-01 20:55:21-0400";

    private static final String jsonDevice = "{\n"
                                             + "     \"device_id\": \"" + TEST_DEVICE_ID + "\",\n"
                                             + "     \"project_id\": 1000,\n"
                                             + "     \"device_name\": \"java_test_ex\",\n"
                                             + "     \"device_type\": \"java_test\",\n"
                                             + "     \"created\": \"" + TEST_DATE_STRING + "\"\n"
                                             + "}";

    @Test
    public void testFromJson() throws Exception {
        Device d = Device.fromJson(new JSONObject(jsonDevice));
        assertNotNull(d);
        assertEquals(TEST_DEVICE_ID, d.getId());
        assertEquals(1000, d.getProjectId());
        assertEquals("java_test_ex", d.getName());
        assertEquals("java_test", d.getType());
        Date expected = Util.DATE_FORMAT.parse(TEST_DATE_STRING);
        assertEquals(expected, d.getCreated());
    }

    @Test
    public void testSerialization() throws Exception {
        ResourceMapper mapper = new ResourceMapper();
        Date date = Util.DATE_FORMAT.parse(TEST_DATE_STRING);
        Device d = new Device(TEST_DEVICE_ID, 1000, "java_test_ex", "java_test", date);
        byte[] raw = mapper.toJsonBytes(d);
        assertTrue(raw.length > 0);
        JSONObject json = new JSONObject(new String(raw, "UTF-8"));
        assertNotNull(json);
        assertEquals(TEST_DEVICE_ID, json.get("device_id"));
        assertEquals(1000, json.get("project_id"));
        assertEquals("java_test_ex", json.get("device_name"));
        assertEquals("java_test", json.get("device_type"));
        assertEquals(date, Util.DATE_FORMAT.parse(json.getString("created")));
    }
}