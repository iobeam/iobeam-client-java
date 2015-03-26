package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ResourceMapperTest {

    private ResourceMapper mapper = new ResourceMapper();

    private static class Bean {

        private String type;
        private boolean valid;
        private List<Integer> numbers = new ArrayList<Integer>();

        public Bean(String type, boolean valid, int[] numbers) {
            this.type = type;
            this.valid = valid;

            for (Integer i : numbers) {
                this.numbers.add(i);
            }
        }

        public String getType() {
            return type;
        }

        public boolean getValid() {
            return valid;
        }

        public List<Integer> getNumbers() {
            return numbers;
        }

        private String getNoinclude() {
            return "doNotInclude";
        }
    }

    @Test
    public void testBeanSerialization() throws Exception {
        final Bean b = new Bean("foo", false, new int[]{1, 3, 4});

        final byte[] res = mapper.toJsonBytes(b);

        final JSONObject json = new JSONObject(new String(res, "UTF-8"));

        assertFalse(json.getBoolean("valid"));
        assertEquals("foo", json.getString("type"));
        assertNull(json.optString("noinclude", null));

        JSONArray arr = json.getJSONArray("numbers");

        assertEquals(1, arr.getInt(0));
        assertEquals(3, arr.getInt(1));
        assertEquals(4, arr.getInt(2));
    }

    @Test
    public void testDataPointSerialization() throws Exception {
        DataPoint dp1 = new DataPoint(100, 10);
        DataPoint dp2 = new DataPoint(110, 10.51);
        DataPoint dp3 = new DataPoint(200, "11");

        byte[] res = mapper.toJsonBytes(dp1);
        JSONObject json = new JSONObject(new String(res, "UTF-8"));
        assertEquals(100, json.getLong("time"));
        assertEquals(10, json.getLong("value"));

        res = mapper.toJsonBytes(dp2);
        json = new JSONObject(new String(res, "UTF-8"));
        assertEquals(110, json.getLong("time"));
        assertEquals(10.51, json.getDouble("value"), .01);

        res = mapper.toJsonBytes(dp3);
        json = new JSONObject(new String(res, "UTF-8"));
        assertEquals(200, json.getLong("time"));
        assertEquals("11", json.getString("value"));
    }
}