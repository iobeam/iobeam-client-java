package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * When converted to JSON, a "time" column is prepended to the other columns which are listed
 * in alphabetical order. Rows are represented as a list with the first value being the timestamp,
 * followed by corresponding values for each column. If a row does not have a value for a column,
 * then it is filled in with null.
 */
public class DataBatch implements Serializable {

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

    private static final Logger logger = Logger.getLogger(DataBatch.class.getName());
    private static final String KEY_COLUMNS = "fields";
    private static final String KEY_ROWS = "data";


    private final TreeSet<String> columns;
    private final TreeMap<Long, Map<String, Object>> rows = new TreeMap<Long, Map<String, Object>>();

    /**
     * Constructs a DataBatch, using the list to construct a _set_ of columns.
     * Note: Duplicates will be removed and a warning will be logged.
     *
     * @param columns Set of field names to track in this batch.
     */
    public DataBatch(Set<String> columns) {
        this.columns = new TreeSet<String>(columns);
    }

    /**
     * Constructs a DataBatch, using the list to construct a _set_ of columns.
     * Note: Duplicates will be removed and a warning will be logged.
     *
     * @param columns List of field names to track in this batch.
     */
    public DataBatch(List<String> columns) {
        this.columns = new TreeSet<String>(columns);
        if (columns.size() != this.columns.size()) {
            logger.warning("Size mismatch in provided list of columns and resulting set of columns;" +
                           " list may have contained duplicates.");
        }
    }

    public DataBatch(String[] columns) {
        this(Arrays.asList(columns));
    }

    /**
     * Add a data row to the batch at a particular timestamp.
     *
     * This method will throw a `MismatchedLengthException` if the length of `columns` and
     * `values` are not the same.
     *
     * This method will throw an `UnknownFieldException` if `data` contains a key that is
     * not in the set of columns this batch was constructed with.
     *
     * @param timestamp Timestamp for all data points
     * @param columns The list of columns for a field to value mapping
     * @param values The list of values for a field to value mapping
     */
    public void add(long timestamp, String[] columns, Object[] values) {
        if (columns.length != values.length) {
            throw new MismatchedLengthException();
        }
        Map<String, Object> temp = new HashMap<String, Object>();
        for (int i = 0; i < columns.length; i++) {
            temp.put(columns[i], values[i]);
        }
        add(timestamp, temp);
    }

    /**
     * Add a data row to the batch at a particular timestamp.
     *
     * This method will throw an `UnknownFieldException` if `data` contains a key that is
     * not in the set of columns this batch was constructed with.
     *
     * @param timestamp Timestamp for all data points
     * @param data Map that has field names as keys and the data value as values.
     */
    public void add(long timestamp, Map<String, Object> data) {
        for (String k : data.keySet()) {
            if (!columns.contains(k)) {
                throw new UnknownFieldException(k);
            }
        }
        this.rows.put(timestamp, new HashMap<String, Object>(data));
    }

    public void merge(DataBatch other) {
        if (!this.columns.equals(other.columns)) {
            throw new IllegalArgumentException("DataBatch must have the same columns to merge");
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

    public TreeMap<Long, Map<String, Object>> getRows() {
        return new TreeMap<Long, Map<String, Object>>(this.rows);
    }

    public long getDataSize() {
        return this.rows.size() * this.columns.size();
    }

    public boolean hasSameColumns(DataBatch other) {
        if (other == null) {
            return false;
        }
        return this.columns.equals(other.columns);
    }

    // NOTE(robatticus): Not sure this will ever be needed, since batches are only sent TO iobeam,
    // not received from iobeam.
    public static DataBatch fromJson(final JSONObject json) throws ParseException {
        throw new UnsupportedOperationException();
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
        return "DataBatch{" +
               "columns=" + this.columns +
               "dataSize=" + this.rows.size() + 
               "}";
    }

    public List<DataBatch> split(int maxRows) {
        return DataBatch.split(this, maxRows);
    }

    public void reset() {
        this.rows.clear();
    }

    public static List<DataBatch> split(DataBatch batch, int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be greater than 0");
        }

        List<DataBatch> ret = new ArrayList<DataBatch>();
        if (batch.rows.size() <= maxRows) {
            ret.add(batch);
        } else {
            for (int i = 0; i < batch.rows.size(); i += maxRows) {
                DataBatch temp = new DataBatch(batch.columns);
                temp.rows.putAll(batch.rows.subMap((long) i, (long) i + maxRows));
                ret.add(temp);
            }
        }
        return ret;
    }

    public static DataBatch snapshot(DataBatch batch) {
        DataBatch ret = new DataBatch(batch.columns);
        ret.rows.putAll(batch.rows);

        return ret;
    }
}
