package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

/**
 * A resource representing an import request resource for IOBeam.
 */
public class Import implements Serializable {

    private final String deviceId;
    private final long projectId;

    private final Map<String, Set<DataPoint>> sources = new HashMap<String, Set<DataPoint>>();

    /**
     * Creates a new Import object for a particular device and project.
     * @param deviceId Device ID this data is for
     * @param projectId Project ID this data is for
     */
    public Import(String deviceId, long projectId) {
        this.deviceId = deviceId;
        this.projectId = projectId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public long getProjectId() {
        return projectId;
    }

    /**
     * Gets a mapping from series to an unordered set of DataPoints.
     * @return Mapping from a series name to unordered set of data.
     */
    public Map<String, Set<DataPoint>> getSources() {
        return new HashMap<String, Set<DataPoint>>(sources);
    }

    /**
     * Gets the unordered set of data associated with a series.
     * @param label The name of the series to get
     * @return Set of unordered data for that series.
     */
    public Set<DataPoint> getDataSeries(String label) {
        return sources.get(label);
    }

    /**
     * Adds a new data point to a particular series.
     * @param series The series this data point should be added to. If it doesn't exist, it will be created.
     * @param dataPoint Data to be added.
     */
    public void addDataPoint(String series, DataPoint dataPoint) {
        Set<DataPoint> set = sources.get(series);
        if (set == null) {
            set = new HashSet<DataPoint>();
            sources.put(series, set);
        }
        set.add(dataPoint);
    }

    /**
     * Custom serialization method for this object to JSON.
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
            for (DataPoint d : sources.get(k))
                dataArr.put(d.toJson());
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
