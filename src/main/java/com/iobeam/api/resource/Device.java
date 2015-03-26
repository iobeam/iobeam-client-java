package com.iobeam.api.resource;

import com.iobeam.api.resource.annotations.JsonProperty;
import com.iobeam.api.resource.util.Util;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

/**
 * A resource for representing devices in the IOBeam API.
 */
public class Device implements Serializable {

    /**
     * A wrapper class around a String that uniquely identifies the device.
     */
    public static final class Id implements Serializable {

        private final String id;

        public Id(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static Device.Id fromJson(final JSONObject json)
            throws ParseException {
            return new Device.Id(json.getString("device_id"));
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private final Id id;
    private final long projectId;
    private final String name;
    private final String type;
    private final Date created;

    public Device(String id,
                  long projectId,
                  String name,
                  String type,
                  Date created) {
        this(new Id(id), projectId, name, type, created);
    }

    public Device(Id id,
                  long projectId,
                  String name,
                  String type,
                  Date created) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.type = type;
        this.created = created;
    }

    public Device(Device d) {
        this(d.id, d.projectId, d.name, d.type, d.created);
    }

    @JsonProperty("device_id")
    public String getId() {
        return id.getId();
    }

    @JsonProperty("project_id")
    public long getProjectId() {
        return projectId;
    }

    @JsonProperty("device_name")
    public String getName() {
        return name;
    }

    @JsonProperty("device_type")
    public String getType() {
        return type;
    }

    public Date getCreated() {
        return created;
    }

    public static Device fromJson(final JSONObject json)
        throws ParseException {
        String id = json.getString("device_id");
        long projectId = json.getLong("project_id");
        String name = json.optString("device_name");
        String type = json.optString("device_type");
        Date created = Util.DATE_FORMAT.parse(json.getString("created"));
        return new Device(id, projectId, name, type, created);
    }

    @Override
    public String toString() {
        return "Device{" +
               "id='" + id.getId() + "'" +
               ", projectId=" + projectId +
               ", name='" + name + "'" +
               ", type='" + type + "'" +
               ", created=" + (created != null ? Util.DATE_FORMAT.format(created) : null) +
               '}';
    }
}
