package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Represents a batch of data streams in column format. That is, columns are names for
 * series/streams, and each row is a timestamp plus values for (some of) the series/streams.
 *
 * When converted to JSON, a "time" column is prepended to the other columns which are listed in
 * alphabetical order. Rows are represented as a list with the first value being the timestamp,
 * followed by corresponding values for each column. If a row does not have a value for a column,
 * then it is filled in with null.
 */
public class DataStore implements Serializable {

    public static final class MismatchedLengthException extends RuntimeException {

        public MismatchedLengthException() {
            super("Number of columns and values are not the same.");
        }
    }

    public static final class UnknownFieldException extends RuntimeException {

        public UnknownFieldException(String field) {
            super("Unknown field '" + field + "' trying to be added.");
        }
    }

    public static final class ReservedColumnException extends IllegalArgumentException {

        public ReservedColumnException(String column) {
            super("'" + column + "' is a reserved column name.");
        }
    }

    private static final Logger logger = Logger.getLogger(DataStore.class.getName());
    private static final String KEY_COLUMNS = "fields";
    private static final String KEY_ROWS = "data";
    private static final String[] RESERVED_COLS = {"time", "time_offset", "all"};


    private final TreeSet<String> columns;
    private final TreeMap<Long, Map<String, Object>> rows =
        new TreeMap<Long, Map<String, Object>>();

    /**
     * Constructs a DataStore, using a collection to construct a _set_ of columns. Note: Duplicates
     * will be removed and a warning will be logged.
     *
     * @param columns Set of field names to track in this batch.
     */
    public DataStore(Collection<String> columns) {
        checkColumns(columns);
        this.columns = new TreeSet<String>(columns);
        if (columns.size() != this.columns.size()) {
            logger.warning("Size mismatch in provided list of columns and resulting set of " +
                           "columns; list may have contained duplicates.");
        }
    }

    public DataStore(String... columns) {
        this(Arrays.asList(columns));
    }

    private void checkColumns(Collection<String> columns) {
        for (String c : columns) {
            if (c == null || c.isEmpty()) {
                throw new IllegalArgumentException("Column cannot be null or empty string.");
            }
            if (Arrays.asList(RESERVED_COLS).contains(c.toLowerCase())) {
                throw new ReservedColumnException(c);
            }
        }
    }

    /**
     * Add a data row, consisting of one column, to the store at a particular time.
     *
     * @param timestamp Timestamp for the data point.
     * @param column    The column for a field to value mapping
     * @param value     The value for a field to value mapping
     */
    public void add(long timestamp, String column, Object value) {
        if (!(value instanceof Long) && !(value instanceof Integer) && !(value instanceof Double) &&
            !(value instanceof Float) && !(value instanceof Boolean) &&
            !(value instanceof String)) {
            throw new IllegalArgumentException(
                "value must be of type: Long, Integer, Double, Float, Boolean, or String");
        }
        Map<String, Object> temp = new HashMap<String, Object>();
        temp.put(column, value);
        add(timestamp, temp);
    }

    /**
     * Add a data row, consisting of one column, to the store at the current time.
     *
     * See {@link #add(long, Map)} for more information.
     *
     * @param column The column for a field to value mapping
     * @param value  The value for a field to value mapping
     */
    public void add(String column, Object value) {
        add(System.currentTimeMillis(), column, value);
    }

    /**
     * Add a data row to the batch at a particular timestamp.
     *
     * This method will throw a `MismatchedLengthException` if the length of `columns` and `values`
     * are not the same.
     *
     * See {@link #add(long, Map)} for more information.
     *
     * @param timestamp Timestamp for all data points
     * @param columns   The list of columns for a field to value mapping
     * @param values    The list of values for a field to value mapping
     */
    public void add(long timestamp, String[] columns, Object[] values) {
        add(timestamp, Arrays.asList(columns), Arrays.asList(values));
    }

    /**
     * Add a data row to the batch with the current time.
     *
     * This method will throw a `MismatchedLengthException` if the length of `columns` and `values`
     * are not the same.
     *
     * See {@link #add(long, Map)} for more information.
     *
     * @param columns The list of columns for a field to value mapping
     * @param values  The list of values for a field to value mapping
     */
    public void add(String[] columns, Object[] values) {
        add(System.currentTimeMillis(), columns, values);
    }

    /**
     * Add a data row to the batch at a particular timestamp.
     *
     * This method will throw a `MismatchedLengthException` if the size of `columns` and `values`
     * are not the same.
     *
     * See {@link #add(long, Map)} for more information.
     *
     * @param timestamp Timestamp for all data points
     * @param columns   The list of columns for a field to value mapping
     * @param values    The list of values for a field to value mapping
     */
    public void add(long timestamp, List<String> columns, List<Object> values) {
        if (columns.size() != values.size()) {
            throw new MismatchedLengthException();
        }
        Map<String, Object> temp = new HashMap<String, Object>();
        for (int i = 0; i < columns.size(); i++) {
            temp.put(columns.get(i), values.get(i));
        }
        add(timestamp, temp);
    }

    /**
     * Add a data row to the batch with the current time.
     *
     * This method will throw a `MismatchedLengthException` if the size of `columns` and `values`
     * are not the same.
     *
     * See {@link #add(long, Map)} for more information.
     *
     * @param columns The list of columns for a field to value mapping
     * @param values  The list of values for a field to value mapping
     */
    public void add(List<String> columns, List<Object> values) {
        add(System.currentTimeMillis(), columns, values);
    }

    /**
     * Add a data row to the batch at a particular timestamp, merging with previous values if
     * needed.
     *
     * This method will throw an `UnknownFieldException` if `data` contains a key that is not in the
     * set of columns this batch was constructed with.
     *
     * @param timestamp Timestamp for all data points
     * @param data      Map that has field names as keys and the data value as values.
     */
    public void add(long timestamp, Map<String, Object> data) {
        for (String k : data.keySet()) {
            if (!columns.contains(k)) {
                throw new UnknownFieldException(k);
            }
        }

        Map<String, Object> curr = this.rows.get(timestamp);
        if (curr == null) {
            this.rows.put(timestamp, new HashMap<String, Object>(data));
        } else {
            curr.putAll(data);
        }
    }

    /**
     * Add a data row to the batch with the current time.
     *
     * This method will throw an `UnknownFieldException` if `data` contains a key that is not in the
     * set of columns this batch was constructed with.
     *
     * @param data Map that has field names as keys and the data value as values.
     */
    public void add(Map<String, Object> data) {
        add(System.currentTimeMillis(), data);
    }

    /**
     * Add the entirety of another DataStore into this one.
     *
     * @param other The other DataStore to merge in
     */
    public void merge(DataStore other) {
        if (!this.hasSameColumns(other)) {
            throw new IllegalArgumentException("DataStore must have the same columns to merge");
        }

        this.rows.putAll(other.rows);
    }

    /**
     * Return a list of the field names tracked by this batch.
     *
     * @return List that is a copy of the columns tracked by this batch.
     */
    public List<String> getColumns() {
        return new ArrayList<String>(this.columns);
    }

    /**
     * Return the rows of this batch as a Map from time to a Map from column to value.
     *
     * @return Map from a time to a Map from column o value.
     */
    public TreeMap<Long, Map<String, Object>> getRows() {
        return new TreeMap<Long, Map<String, Object>>(this.rows);
    }

    /**
     * The number of data values currently stored in this batch, i.e., the product of the number of
     * rows times the number of columns. (Empty data values are counted).
     *
     * @return Size of this DataStore
     */
    public long getDataSize() {
        return this.rows.size() * this.columns.size();
    }

    /**
     * Check if another DataStore has the same columns as this one (weak equality check).
     *
     * @param other Other DataStore to compare
     * @return True if the set of columns are the same
     */
    public boolean hasSameColumns(DataStore other) {
        return other != null && this.columns.equals(other.columns);
    }

    /**
     * Check if this DataStore has the given columns.
     *
     * @param columns Columns to check for
     * @return True if the column sets are equal
     */
    public boolean hasColumns(Collection<String> columns) {
        return columns != null && this.columns.equals(new TreeSet<String>(columns));
    }

    /**
     * Create a DataStore object from JSON.
     *
     * @param json JSON of the DataStore
     * @return DataStore object corresponding to the given JSON
     * @throws ParseException If the JSON is invalid
     */
    public static DataStore fromJson(final JSONObject json) throws ParseException {
        DataStore ret;

        JSONArray jsonCols = json.getJSONArray("fields");
        if (!"time".equals(jsonCols.get(0))) {
            throw new JSONException("time must be the first item in 'fields'");
        }

        Set<String> cols = new HashSet<String>();
        for (int i = 1; i < jsonCols.length(); i++) {
            cols.add(jsonCols.getString(i));
        }
        ret = new DataStore(cols);

        JSONArray jsonData = json.getJSONArray("data");
        for (int i = 0; i < jsonData.length(); i++) {
            JSONArray row = jsonData.getJSONArray(i);
            Long ts = row.getLong(0);
            Map<String, Object> vals = new HashMap<String, Object>();
            for (int j = 1; j < row.length(); j++) {
                vals.put(jsonCols.getString(j), row.get(j));
            }
            ret.rows.put(ts, vals);
        }

        return ret;
    }

    /**
     * Convert this batch into a JSON representation.
     *
     * @return JSONObject representing this batch.
     */
    public JSONObject toJson() {
        JSONObject ret = new JSONObject();
        JSONArray columns = new JSONArray(Arrays.asList(new String[]{"time"}));
        for (String f : this.columns) {
            columns.put(f);
        }
        ret.put(KEY_COLUMNS, columns);

        JSONArray data = new JSONArray();
        ret.put(KEY_ROWS, data);
        for (Long ts : rows.keySet()) {
            JSONArray row = new JSONArray();
            row.put(ts);
            Map<String, Object> temp = rows.get(ts);
            for (String f : this.columns) {
                Object val = temp.get(f);
                row.put(val != null ? val : JSONObject.NULL);
            }
            data.put(row);
        }

        return ret;
    }

    @Override
    public String toString() {
        return "DataStore{" +
               "columns=" + this.columns +
               "dataSize=" + this.rows.size() +
               "}";
    }

    public List<DataStore> split(int maxRows) {
        return DataStore.split(this, maxRows);
    }

    /**
     * Alias for `reset()`.
     */
    public void clear() {
        reset();
    }

    /**
     * Removes all the data from the DataStore
     */
    public void reset() {
        this.rows.clear();
    }

    public static List<DataStore> split(DataStore batch, int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be greater than 0");
        }

        List<DataStore> ret = new ArrayList<DataStore>();
        if (batch.rows.size() <= maxRows) {
            ret.add(batch);
        } else {
            for (int i = 0; i < batch.rows.size(); i += maxRows) {
                DataStore temp = new DataStore(batch.columns);
                temp.rows.putAll(batch.rows.subMap((long) i, (long) i + maxRows));
                ret.add(temp);
            }
        }
        return ret;
    }

    public static DataStore snapshot(DataStore batch) {
        DataStore ret = new DataStore(batch.columns);
        ret.rows.putAll(batch.rows);

        return ret;
    }
}
