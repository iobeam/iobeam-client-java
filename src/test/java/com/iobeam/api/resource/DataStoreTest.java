package com.iobeam.api.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataStoreTest {

    @Test
    public void testDefensiveConstructorSet() throws Exception {
        Set<String> fields = new HashSet<String>();
        fields.add("a");
        fields.add("b");
        fields.add("c");
        DataStore batch = new DataStore(fields);
        List<String> batchFields = batch.getColumns();
        assertEquals(fields.size(), batchFields.size());
        fields.remove("a");
        assertEquals(fields.size() + 1, batch.getColumns().size());
    }

    @Test
    public void testDefensiveConstructorList() throws Exception {
        List<String> fields = new ArrayList<String>();
        fields.add("a");
        fields.add("b");
        fields.add("c");
        DataStore batch = new DataStore(fields);
        List<String> batchFields = batch.getColumns();
        assertEquals(fields.size(), batchFields.size());
        for (int i = 0; i < fields.size(); i++) {
            assertEquals(fields.get(i), batchFields.get(i));
        }
        fields.set(0, "aa");
        assertEquals("a", batch.getColumns().get(0));
    }

    @Test
    public void testDefensiveGetterFields() throws Exception {
        Set<String> fields = new HashSet<String>();
        fields.add("a");
        fields.add("b");
        fields.add("c");
        DataStore batch = new DataStore(fields);
        List<String> batchFields = batch.getColumns();
        batchFields.set(0, "aa");
        assertEquals("a", batch.getColumns().get(0));
    }

    @Test
    public void testFromJson() throws Exception {
        JSONObject obj = new JSONObject();
        JSONArray fields = new JSONArray();
        fields.put("time");
        fields.put("a");
        fields.put("b");
        obj.put("fields", fields);

        JSONArray data = new JSONArray();
        for (int i = 0; i < 3; i++) {
            JSONArray row = new JSONArray();
            row.put(i);
            row.put(i);
            row.put(i * 10);
            data.put(row);
        }
        obj.put("data", data);

        DataStore ds = DataStore.fromJson(obj);
        List<String> cols = ds.getColumns();
        assertEquals(2, cols.size());
        assertTrue(cols.contains("a"));
        assertTrue(cols.contains("b"));

        Map<Long, Map<String, Object>> rows = ds.getRows();
        assertEquals(3, rows.size());
        long i = 0;
        for (Long l : rows.keySet()) {
            assertEquals(i, l.longValue());
            if (rows.get(l).get("a") instanceof Integer) {
                assertEquals(i, ((Integer) rows.get(l).get("a")).longValue());
            } else if (rows.get(l).get("a") instanceof Integer) {
                assertEquals(i, ((Long) rows.get(l).get("a")).longValue());
            } else {
                assertEquals(i, rows.get(l).get("a"));
            }

            if (rows.get(l).get("b") instanceof Integer) {
                assertEquals(i * 10, ((Integer) rows.get(l).get("b")).longValue());
            } else if (rows.get(l).get("b") instanceof Long) {
                assertEquals(i * 10, ((Long) rows.get(l).get("b")).longValue());
            } else {
                assertEquals(i, rows.get(l).get("b"));
            }

            i++;
        }
    }

    @Test(expected = JSONException.class)
    public void testFromJsonInvalidCols() throws Exception {
        JSONObject obj = new JSONObject();
        JSONArray fields = new JSONArray();
        fields.put("a");
        fields.put("b");
        obj.put("fields", fields);

        JSONArray data = new JSONArray();
        for (int i = 0; i < 3; i++) {
            JSONArray row = new JSONArray();
            row.put(i);
            row.put(i);
            row.put(i * 10);
            data.put(row);
        }
        obj.put("data", data);

        DataStore.fromJson(obj);
    }

    @Test(expected = DataStore.ReservedColumnException.class)
    public void testReservedColumn() throws Exception {
        DataStore ds = new DataStore("time", "colA");
        ds.clear();
    }

    @Test(expected = DataStore.ReservedColumnException.class)
    public void testReservedColumn2() throws Exception {
        DataStore ds = new DataStore("time_offset", "colA");
        ds.clear();
    }

    @Test(expected = DataStore.ReservedColumnException.class)
    public void testReservedColumn3() throws Exception {
        DataStore ds = new DataStore("colA", "all");
        ds.clear();
    }

    @Test(expected = DataStore.ReservedColumnException.class)
    public void testReservedColumn4() throws Exception {
        Set<String> cols = new HashSet<String>();
        cols.add("AlL");
        cols.add("colB");
        DataStore ds = new DataStore(cols);
        ds.clear();
    }

    @Test(expected = DataStore.ReservedColumnException.class)
    public void testReservedColumnDifferentCase() throws Exception {
        DataStore ds = new DataStore("tIMe", "colA");
        ds.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullColumn() throws Exception {
        DataStore ds = new DataStore("good", null);
        ds.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyColumn() throws Exception {
        DataStore ds = new DataStore("good", "");
        ds.clear();
    }

    @Test
    public void testDuplicateAddTime() throws Exception {
        DataStore ds = new DataStore("col1", "col2");
        ds.add(0, "col1", 5);
        Map<String, Object> data = ds.getRows().get(0L);
        assertEquals(1, data.size());
        assertEquals(5, data.get("col1"));
        ds.add(0, "col2", 10);
        data = ds.getRows().get(0L);
        assertEquals(2, data.size());
        assertEquals(5, data.get("col1"));
        assertEquals(10, data.get("col2"));
    }

    @Test
    public void testAddSingleValue() throws Exception {
        DataStore ds = new DataStore("col1");
        ds.add("col1", 1);
        ds.add("col1", 100L);
        ds.add("col1", 1.0F);
        ds.add("col1", 5.0D);
        ds.add("col1", true);
        ds.add("col1", "string");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddBadSingleValue() throws Exception {
        DataStore ds = new DataStore("col1");
        ds.add("col1", new Object[]{1.0});
    }

    @Test
    public void testToJson() throws Exception {
        Set<String> fields = new HashSet<String>();
        fields.add("b");
        fields.add("a");
        DataStore batch = new DataStore(fields);

        List<Object[]> want = new ArrayList<Object[]>();
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("a", 100);
        row.put("b", 200);
        batch.add(0, row);
        want.add(new Object[]{0, 100, 200});

        row.put("a", 300);
        row.remove("b");
        batch.add(100, row);
        want.add(new Object[]{100, 300, null});

        JSONObject json = batch.toJson();

        assertTrue(json.has("fields"));
        JSONArray jsonFields = json.getJSONArray("fields");
        // "time" field + those passed in = 1 + fields.length
        assertEquals(1 + fields.size(), jsonFields.length());
        assertEquals("time", jsonFields.get(0));
        for (int i = 0; i < fields.size(); i++) {
            assertTrue(fields.contains(jsonFields.getString(i + 1)));
        }

        assertTrue(json.has("data"));
        JSONArray data = json.getJSONArray("data");
        assertEquals(want.size(), data.length());
        for (int i = 0; i < data.length(); i++) {
            JSONArray r = data.getJSONArray(i);
            Object[] w = want.get(i);
            assertEquals(w.length, r.length());
            for (int j = 0; j < w.length; j++) {
                // The JSON lib assumes that a 'null' return value for a JSONArray.get() call means
                // it is an index out of bounds. So we have to do this weird hack to handle nulls
                // correctly (we already know the arrays are the same size.
                Integer have;
                try {
                    have = r.getInt(j);
                } catch (JSONException e) {
                    if (w[j] == null) {
                        assertTrue(true);
                    } else {
                        assertTrue(false);
                    }
                    continue;
                }
                if (w[j] == null) {
                    assertTrue(false);
                } else {
                    assertEquals(Integer.valueOf(w[j].toString()).intValue(), have.intValue());
                }
            }
        }
    }

    @Test
    public void testDataSize() throws Exception {
        DataStore b = new DataStore("a", "b", "c");
        assertEquals(0, b.getDataSize());
        b.add(0, new String[]{"a", "b", "c"}, new Object[]{1, 2, 3});
        assertEquals(3, b.getDataSize());
        b.add(1, new String[]{"a", "b"}, new Object[]{4, 5});
        assertEquals(6, b.getDataSize());
        b.reset();
        assertEquals(0, b.getDataSize());
    }

    @Test
    public void testHasSameColumns() throws Exception {
        DataStore b1 = new DataStore("a", "b", "c");
        assertTrue(b1.hasSameColumns(b1));
        assertFalse(b1.hasSameColumns(null));

        DataStore b2 = new DataStore("a", "c");
        assertFalse(b1.hasSameColumns(b2));
        assertFalse(b2.hasSameColumns(b1));

        DataStore b3 = new DataStore("a", "c", "b");
        assertTrue(b1.hasSameColumns(b3));
        assertTrue(b3.hasSameColumns(b1));
    }

    @Test
    public void testHasColumns() throws Exception {
        DataStore b1 = new DataStore("a", "b", "c");
        assertFalse(b1.hasColumns(null));
        assertFalse(b1.hasColumns(Collections.<String>emptyList()));

        String[] cols = {"a", "b", "c"};
        assertTrue(b1.hasColumns(Arrays.asList(cols)));

        cols = new String[]{"c", "a", "b"};
        assertTrue(b1.hasColumns(Arrays.asList(cols)));

        cols = new String[]{"a", "b"};
        assertFalse(b1.hasColumns(Arrays.asList(cols)));

        cols = new String[]{"a", "b", "c", "d"};
        assertFalse(b1.hasColumns(Arrays.asList(cols)));
    }

    @Test
    public void testMerge() throws Exception {
        DataStore b1 = new DataStore("a");
        b1.add(1, new String[]{"a"}, new Object[]{1});
        b1.add(2, new String[]{"a"}, new Object[]{2});
        b1.add(3, new String[]{"a"}, new Object[]{3});

        DataStore b2 = new DataStore("a");
        b2.add(4, new String[]{"a"}, new Object[]{4});
        b2.add(5, new String[]{"a"}, new Object[]{5});
        b2.add(6, new String[]{"a"}, new Object[]{6});

        b1.merge(b2);
        assertEquals(6, b1.getDataSize());

        DataStore wrong = new DataStore("b");
        wrong.add(7, new String[]{"b"}, new Object[]{7});
        try {
            b1.merge(wrong);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    @Test
    public void testMergeWrong() throws Exception {
        DataStore b1 = new DataStore("a");

        DataStore wrong = new DataStore("b");
        wrong.add(7, new String[]{"b"}, new Object[]{7});
        try {
            b1.merge(wrong);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            // do nothing
        }

        wrong = new DataStore("a", "b");
        wrong.add(7, new String[]{"a"}, new Object[]{7});
        try {
            b1.merge(wrong);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    @Test
    public void testSplit() throws Exception {
        List<String> fields = new ArrayList<String>();
        fields.add("a");
        DataStore batch = new DataStore(fields);

        Map<String, Object> row = new HashMap<String, Object>();
        row.put("a", 100);
        batch.add(0, row);
        batch.add(1, row);
        batch.add(2, row);
        batch.add(3, row);

        List<DataStore> splits = DataStore.split(batch, 2);
        assertEquals(2, splits.size());

        splits = DataStore.split(batch, 3);
        assertEquals(2, splits.size());

        splits = DataStore.split(batch, 4);
        assertEquals(1, splits.size());
    }

}
