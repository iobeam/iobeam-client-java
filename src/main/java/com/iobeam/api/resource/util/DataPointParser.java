package com.iobeam.api.resource.util;

import com.iobeam.api.resource.DataPoint;

import java.util.ArrayList;
import java.util.List;


public class DataPointParser {

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
    public static List<DataPoint> parse(String points, String splitRegex, Class<?> type) {
        return parse(points, splitRegex, type, System.currentTimeMillis());
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
    public static List<DataPoint> parse(String points, String splitRegex, Class<?> type, long ts) {
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
     * Takes an array of values and converts it into a list of `DataPoint`s with the same (current)
     * timestamp.
     *
     * @param values Array of values to be converted
     * @return A List of DataPoints with the current timestamp.
     */
    public static List<DataPoint> parse(double[] values) {
        return parse(values, System.currentTimeMillis());
    }

    /**
     * Takes an array of values and converts it into a list of `DataPoint`s with the same
     * timestamp.
     *
     * @param values Array of values to be converted
     * @param ts     Timestamp to use.
     * @return A List of DataPoints with the same timestamp.
     */
    public static List<DataPoint> parse(double[] values, long ts) {
        List<DataPoint> ret = new ArrayList<DataPoint>(values.length);
        for (double v : values) {
            ret.add(new DataPoint(ts, v));
        }
        return ret;
    }

    /**
     * Takes an array of values and converts it into a list of `DataPoint`s with the same (current)
     * timestamp.
     *
     * @param values Array of values to be converted
     * @return A List of DataPoints with the current timestamp.
     */
    public static List<DataPoint> parse(float[] values) {
        return parse(values, System.currentTimeMillis());
    }

    /**
     * Takes an array of values and converts it into a list of `DataPoint`s with the same
     * timestamp.
     *
     * @param values Array of values to be converted
     * @param ts     Timestamp to use.
     * @return A List of DataPoints with the same timestamp.
     */
    public static List<DataPoint> parse(float[] values, long ts) {
        List<DataPoint> ret = new ArrayList<DataPoint>(values.length);
        for (float v : values) {
            ret.add(new DataPoint(ts, v));
        }
        return ret;
    }

    /**
     * Takes an array of values and converts it into a list of `DataPoint`s with the same (current)
     * timestamp.
     *
     * @param values Array of values to be converted
     * @return A List of DataPoints with the current timestamp.
     */
    public static List<DataPoint> parse(long[] values) {
        return parse(values, System.currentTimeMillis());
    }

    public static List<DataPoint> parse(long[] values, long ts) {
        List<DataPoint> ret = new ArrayList<DataPoint>(values.length);
        for (long v : values) {
            ret.add(new DataPoint(ts, v));
        }
        return ret;
    }

    /**
     * Takes an array of values and converts it into a list of `DataPoint`s with the same (current)
     * timestamp.
     *
     * @param values Array of values to be converted
     * @return A List of DataPoints with the current timestamp.
     */
    public static List<DataPoint> parse(int[] values) {
        return parse(values, System.currentTimeMillis());
    }

    /**
     * Takes an array of values and converts it into a list of `DataPoint`s with the same
     * timestamp.
     *
     * @param values Array of values to be converted
     * @param ts     Timestamp to use.
     * @return A List of DataPoints with the same timestamp.
     */
    public static List<DataPoint> parse(int[] values, long ts) {
        List<DataPoint> ret = new ArrayList<DataPoint>(values.length);
        for (int v : values) {
            ret.add(new DataPoint(ts, v));
        }
        return ret;
    }
}
