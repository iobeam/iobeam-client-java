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
    @Deprecated
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

    public static final class Spec {

        private final String id;
        private final String name;
        private final String type;

        public Spec() {
            this(null, null, null);
        }

        public Spec(String id) {
            this(id, null, null);
        }

        public Spec(String id, String name) {
            this(id, name, null);
        }

        public Spec(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    private final long projectId;
    private final Spec spec;
    private final Date created;

    // TODO(robatticus) Remove in 0.6.0
    @Deprecated
    public Device(String id,
                  long projectId,
                  String name,
                  String type,
                  Date created) {
        this(projectId, new Device.Spec(id, name, type), created);
    }

    // TODO(robatticus) Remove in 0.6.0
    @Deprecated
    public Device(Id id,
                  long projectId,
                  String name,
                  String type,
                  Date created) {
        this(projectId, new Device.Spec(id.getId(), name, type), created);
    }

    public Device(long projectId, Device.Spec spec, Date created) {
        this.projectId = projectId;
        this.spec = spec;
        this.created = created;
    }

    public Device(long projectId, Device.Spec spec) {
        this(projectId, spec, null);
    }

    public Device(Device d) {
        this(d.projectId, d.spec, d.created);
    }

    @JsonProperty("device_id")
    public String getId() {
        return spec.id;
    }

    @JsonProperty("project_id")
    public long getProjectId() {
        return projectId;
    }

    @JsonProperty("device_name")
    public String getName() {
        return spec.name;
    }

    @JsonProperty("device_type")
    public String getType() {
        return spec.type;
    }

    public Date getCreated() {
        return created;
    }

    public static Device fromJson(final JSONObject json)
        throws ParseException {

        final long projectId = json.getLong("project_id");
        final Builder builder = new Builder(projectId).
            setId(json.getString("device_id")).
            setName(json.optString("device_name", null)).
            setType(json.optString("device_type", null));
        // TODO(rrk) - Fix the backend inconsistency first.
        //Date created = Util.RESOURCE_DATE_FORMAT.parse(json.getString("created"));

        return builder.build();
    }

    @Override
    public String toString() {
        return "Device{" +
               "id='" + spec.id + "'" +
               ", projectId=" + projectId +
               ", name='" + spec.name + "'" +
               ", type='" + spec.type + "'" +
               ", created=" + (created != null ? Util.DATE_FORMAT.format(created) : null) +
               '}';
    }

    public static class Builder {

        private final long projectId;
        private String deviceId;
        private String deviceName;
        private String deviceType;

        public Builder(final long projectId) {
            this.projectId = projectId;
        }

        public Builder setId(final String id) {
            this.deviceId = id;
            return this;
        }

        public Builder setName(final String name) {
            this.deviceName = name;
            return this;
        }

        public Builder setType(final String type) {
            this.deviceType = type;
            return this;
        }

        public Device build() {
            return new Device(projectId, new Spec(deviceId, deviceName, deviceType), null);
        }
    }
}
