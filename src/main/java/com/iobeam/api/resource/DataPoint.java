package com.iobeam.api.resource;

import org.json.JSONObject;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.text.ParseException;

/**
 * A resource representing a data point in a time series. This is represented by a timestamp and a
 * value that is either an integer (64-bits), a double, or a String.
 *
 * @deprecated Use the new DataStore format instead.
 */
@Deprecated
public class DataPoint implements Serializable {

    private static final String KEY_TIME = "time";
    private static final String KEY_VALUE = "value";

    public static final byte TYPE_LONG = 0x0;
    public static final byte TYPE_DOUBLE = TYPE_LONG + 1;
    public static final byte TYPE_STRING = TYPE_DOUBLE + 1;
    /* Byte array format, all start with [time (8)][type (1)] (9 bytes), followed by:
        - for int/long/float/double:     [value (8)] (17 total bytes)
        - for strings:                   [len (4)][value (X)] (13 + X total bytes, X = len of str)
     */

    private final long time;
    private final Object data;


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

    public static DataPoint fromByteArray(byte[] array) {
        ByteBuffer buf = ByteBuffer.wrap(array);
        long timestamp = buf.getLong();
        byte type = buf.get();
        if (type == TYPE_LONG) {
            return new DataPoint(timestamp, buf.getLong());
        } else if (type == TYPE_DOUBLE) {
            return new DataPoint(timestamp, buf.getDouble());
        } else if (type == TYPE_STRING) {
            int len = buf.getInt();
            byte[] bytes = new byte[len];
            buf.get(bytes, 0, len);
            return new DataPoint(timestamp, new String(bytes));
        } else {
            // TODO - Throw exception if invalid format.
            return null;
        }
    }

    /**
     * Returns the byte array representation of this data point. Byte array format are as follows:
     *
     * Starts with 8 bytes for time and 1 byte for type for a total of 9 bytes. Then, based on value
     * type, it does the following:
     *
     * (1) int/long/float/double: 8 bytes for the value, for 17 total bytes.
     *
     * (2) strings: 4 bytes for string length, then X bytes for string for a total of 13 + X bytes.
     *
     * @return Byte array representation
     */
    public byte[] toByteArray() {
        int arrSize = 1 + Long.BYTES;  // first byte specifies type of data
        byte type;
        if (data instanceof Long) {
            arrSize += Long.BYTES;
            type = TYPE_LONG;
        } else if (data instanceof Double) {
            arrSize += Double.BYTES;
            type = TYPE_DOUBLE;
        } else {
            arrSize += Integer.BYTES + ((String) data).length();
            type = TYPE_STRING;
        }

        ByteBuffer buf = ByteBuffer.allocate(arrSize);
        buf.putLong(time);
        buf.put(type);
        if (type == TYPE_LONG) {
            buf.putLong((Long) data);
        } else if (type == TYPE_DOUBLE) {
            buf.putDouble((Double) data);
        } else {
            String temp = (String) data;
            buf.putInt(temp.length());
            buf.put(temp.getBytes());
        }
        buf.flip();
        return buf.array();
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
