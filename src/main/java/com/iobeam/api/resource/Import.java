package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A resource representing an import request resource for IOBeam.
 */
public class Import implements Serializable {

    private String deviceId;
    private final long projectId;

    /**
     * DataSet is a convenience name for a `HashSet` containing `DataPoint`s.
     */
    public final class DataSet extends HashSet<DataPoint> {

        public DataSet() {
            super();
        }

        public DataSet(Set<DataPoint> pts) {
            super(pts);
        }
    }

    private final Map<String, DataSet> sources = new HashMap<String, DataSet>();

    /**
     * Creates a new Import object for a particular device and project.
     *
     * @param deviceId  Device ID this data is for
     * @param projectId Project ID this data is for
     */
    public Import(String deviceId, long projectId) {
        this.deviceId = deviceId;
        this.projectId = projectId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getProjectId() {
        return projectId;
    }

    public long getTotalSize() {
        int total = 0;
        for (DataSet set : sources.values()) {
            total += set.size();
        }
        return total;
    }

    /**
     * Gets a mapping from series to an unordered set of DataPoints.
     *
     * @return Mapping from a series name to unordered set of data.
     */
    public Map<String, Set<DataPoint>> getSeries() {
        return new HashMap<String, Set<DataPoint>>(sources);
    }

    /**
     * Gets the unordered set of data associated with a series.
     *
     * @param label The name of the series to get
     * @return Set of unordered data for that series.
     */
    public DataSet getDataSet(String label) {
        return sources.get(label);
    }

    /**
     * Adds a new data point to a particular series.
     *
     * @param series    The series this data point should be added to. If it doesn't exist, it will
     *                  be created.
     * @param dataPoint Data to be added.
     */
    public void addDataPoint(String series, DataPoint dataPoint) {
        DataSet set = sources.get(series);
        if (set == null) {
            set = new DataSet();
            sources.put(series, set);
        }
        set.add(dataPoint);
    }

    /**
     * Adds a set of `DataPoint`s to a series.
     *
     * @param series     The series the DataPoints should be added to. If the series doesn't exist,
     *                   it will be created.
     * @param dataPoints Data to be added.
     */
    public void addDataPointSet(String series, Set<DataPoint> dataPoints) {
        DataSet set = sources.get(series);
        if (set == null) {
            set = new DataSet(dataPoints);
            sources.put(series, set);
        } else {
            set.addAll(dataPoints);
        }
    }

    /**
     * Adds a set of `DataPoint`s to a series.
     *
     * @param series  The series to add the contents of DataSet to. If the series doesn't exist, it
     *                will be created.
     * @param dataSet Data to be added.
     */
    public void addDataSet(String series, DataSet dataSet) {
        addDataPointSet(series, dataSet);
    }

    /**
     * Custom serialization method for this object to JSON.
     *
     * @param out In-out parameter that maps JSON keys to their values.
     * @return A JSONObject representing this object.
     */
    public JSONObject serialize(Map<String, Object> out) {
        JSONObject json = new JSONObject();
        json.put("device_id", deviceId);
        json.put("project_id", projectId);
        out.put("device_id", deviceId);
        out.put("project_id", projectId);
        JSONArray sourcesArr = new JSONArray();
        List<String> sourceNames = new ArrayList<String>(sources.keySet());
        Collections.sort(sourceNames);
        for (String k : sourceNames) {
            JSONObject temp = new JSONObject();
            temp.put("name", k);
            JSONArray dataArr = new JSONArray();
            for (DataPoint d : sources.get(k)) {
                dataArr.put(d.toJson());
            }
            temp.put("data", dataArr);
            sourcesArr.put(temp);
        }
        json.put("sources", sourcesArr);
        out.put("sources", sourcesArr);
        return json;
    }

    public static Import fromJson(final JSONObject json) throws ParseException {
        return new Import(json.getString("device_id"),
                          json.getLong("project_id"));
    }

    @Override
    public String toString() {
        return "Import{" +
               "deviceId=" + deviceId +
               ", projectId='" + projectId + "'" +
               '}';
    }
}
