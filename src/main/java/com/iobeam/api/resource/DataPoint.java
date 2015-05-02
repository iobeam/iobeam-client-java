package com.iobeam.api.resource;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A resource representing a data point in a time series. This is represented by a timestamp and a
 * value that is either an integer (64-bits), a double, or a String.
 */
public class DataPoint implements Serializable {

    private static final String KEY_TIME = "time";
    private static final String KEY_VALUE = "value";

    private final long time;
    private final Object data;

    // TODO: There should probably be some error conditions that throw exceptions or fail nicely.

    /**
     * Takes a string that is a delimited list of values all of a particular type (integer, long,
     * float, double, or string) and parses them into a List of DataPoints all with the same
     * timestamp (the current time).
     *
     * @param points     Delimited string containing points to be parsed.
     * @param splitRegex Delimiter to split the string on (e.g., a comma (,) for csv).
     * @param type       Data type of the points, either Integer/Long, Float/Double, or String.
     * @return A DataPoint List of all parseable points.
     */
    public static List<DataPoint> parseDataPoints(String points,
                                                  String splitRegex,
                                                  Class<?> type) {
        return parseDataPoints(points, splitRegex, type, System.currentTimeMillis());
    }

    /**
     * Takes a string that is a delimited list of values all of a particular type (integer, long,
     * float, double, or string) and parses them into a List of DataPoints all with the same
     * timestamp.
     *
     * @param points     Delimited string containing points to be parsed.
     * @param splitRegex Delimiter to split the string on (e.g., a comma (,) for csv).
     * @param type       Data type of the points, either Integer/Long, Float/Double, or String.
     * @param ts         The timestamp to apply to all the points.
     * @return A DataPoint List of all parseable points.
     */
    public static List<DataPoint> parseDataPoints(String points,
                                                  String splitRegex,
                                                  Class<?> type,
                                                  long ts) {
        String[] items = points.split(splitRegex);
        List<DataPoint> ret = new ArrayList<DataPoint>();
        for (String i : items) {
            if (i.length() == 0) {
                continue;
            }

            if (type == Long.class || type == Integer.class) {
                ret.add(new DataPoint(ts, Long.parseLong(i)));
            } else if (type == Float.class || type == Double.class) {
                ret.add(new DataPoint(ts, Double.parseDouble(i)));
            } else if (type == String.class) {
                ret.add(new DataPoint(ts, i));
            }
        }
        return ret;
    }

    /**
     * Constructor for a point containing integer data
     *
     * @param time Timestamp of the data point (in milliseconds)
     * @param data Data in 64-bit integer form
     */
    public DataPoint(long time, long data) {
        this.time = time;
        this.data = data;
    }

    /**
     * Constructor for a point containing double data
     *
     * @param time Timestamp of the data point (in milliseconds)
     * @param data Data in double/float form
     */
    public DataPoint(long time, double data) {
        this.time = time;
        this.data = data;
    }

    /**
     * Constructor for a point containing String data
     *
     * @param time Timestamp of the data point (in milliseconds)
     * @param data Data in String form
     */
    public DataPoint(long time, String data) {
        this.time = time;
        this.data = data;
    }

    /**
     * Constructor for a point containing integer data at the current time.
     *
     * @param data Data in 64-bit integer form
     */
    public DataPoint(long data) {
        this(System.currentTimeMillis(), data);
    }

    /**
     * Constructor for a point containing double data at the current time.
     *
     * @param data Data in double/float form
     */
    public DataPoint(double data) {
        this(System.currentTimeMillis(), data);
    }

    /**
     * Constructor for a point containing String data at the current time.
     *
     * @param data Data in String form
     */
    public DataPoint(String data) {
        this(System.currentTimeMillis(), data);
    }

    public long getTime() {
        return time;
    }

    public Object getValue() {
        return data;
    }

    public static DataPoint fromJson(final JSONObject json) throws ParseException {
        long timestamp = json.getLong(KEY_TIME);
        Object value = json.get(KEY_VALUE);
        if (value instanceof Integer || value instanceof Long) {
            return new DataPoint(timestamp, json.getLong(KEY_VALUE));
        } else if (value instanceof Double || value instanceof Float) {
            return new DataPoint(timestamp, json.getDouble(KEY_VALUE));
        } else {
            return new DataPoint(timestamp, json.getString(KEY_VALUE));
        }
    }

    public JSONObject toJson() {
        JSONObject ret = new JSONObject();
        ret.put(KEY_TIME, time);
        ret.put(KEY_VALUE, data);

        return ret;
    }

    @Override
    public int hashCode() {
        return (int) this.time;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof DataPoint)) {
            return false;
        }
        DataPoint that = (DataPoint) object;
        return this.time == that.time && this.data.equals(that.data);
    }

    @Override
    public String toString() {
        String dataStr;
        boolean isInt = data instanceof Integer || data instanceof Long;
        boolean isDouble = data instanceof Float || data instanceof Double;
        if (isInt || isDouble) {
            dataStr = KEY_VALUE + "=" + data;
        } else {
            dataStr = KEY_VALUE + "='" + data + "'";
        }
        return "DataPoint{" +
               "time=" + time +
               ", " + dataStr +
               '}';
    }
}
