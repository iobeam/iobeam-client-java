package com.iobeam.api.resource;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

/**
 * A resource represented a import request in batch/table format.
 */
public class ImportBatch implements Serializable {

    private final long projectId;
    private final String deviceId;
    private final DataStore data;
    private final boolean legacy;

    public ImportBatch(long projectId, String deviceId, DataStore data) {
        this(projectId, deviceId, data, false);
    }


    private ImportBatch(long projectId, String deviceId, DataStore data, boolean legacy) {
        this.projectId = projectId;
        this.deviceId = deviceId;
        this.data = data;
        this.legacy = legacy;
    }

    public static ImportBatch createLegacy(long projectId, String deviceId, DataStore data) {
        return new ImportBatch(projectId, deviceId, data, true);
    }

    public long getProjectId() {
        return this.projectId;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public DataStore getData() {
        return this.data;
    }

    public boolean isFromLegacy() {
        return this.legacy;
    }

    public JSONObject serialize() {
        JSONObject ret = new JSONObject();
        ret.put("project_id", this.projectId);
        ret.put("device_id", this.deviceId);
        ret.put("sources", this.data.toJson());

        return ret;
    }

    @Deprecated
    public JSONObject serialize(Map<String, Object> out) {
        out.put("project_id", this.projectId);
        out.put("device_id", this.deviceId);
        out.put("sources", this.data.toJson());

        return serialize();
    }

    public JSONObject toJson() {
        return serialize();
    }

    public JSONObject toJson(Map<String, Object> out) {
        return serialize(out);
    }

    @Override
    public String toString() {
        return "ImportBatch{" +
               "projectId=" + this.projectId + ", " +
               "deviceId=" + this.deviceId + ", " +
               "legacy=" + this.legacy + ", " +
               "data=" + this.data +
               "}";
    }
}
