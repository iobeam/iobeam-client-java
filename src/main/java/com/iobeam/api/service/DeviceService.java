package com.iobeam.api.service;

import com.iobeam.api.client.RestClient;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;
import com.iobeam.api.resource.Device;
import com.iobeam.api.resource.DeviceList;

import java.util.Date;

/**
 * Devices service API. This API is for managing devices in the Cerebriq backend. It can be used to
 * add new devices to projects, remove devices, get all the devices currently in a project, and get
 * a particular device.
 */
public class DeviceService {

    private static final String PATH = "/v1/devices";
    private final RestClient client;

    public DeviceService(final RestClient client) {
        this.client = client;
    }

    public final class Get extends RestRequest<DeviceList> {

        protected Get(long projectId) {
            super(client, RequestMethod.GET, PATH,
                  StatusCode.OK, DeviceList.class);
            getBuilder().addParameter("project_id", projectId);
        }

        public Get setOffset(final int offset) {
            getBuilder().addParameter("offset", offset);
            return this;
        }

        public Get setCount(final int count) {
            getBuilder().addParameter("count", count);
            return this;
        }
    }

    /**
     * A Get API request for fetching all the devices associated with a project.
     *
     * @param projectId Id of the project whose devices to fetch.
     * @return A Get API request that can be executed.
     */
    public Get get(long projectId) {
        return new Get(projectId);
    }


    public final class GetDevice extends RestRequest<Device> {

        protected GetDevice(final String deviceId) {
            super(client, RequestMethod.GET,
                  PATH + "/" + deviceId,
                  StatusCode.OK, Device.class);
        }
    }

    /**
     * Retrieves the device info for the corresponding device ID.
     *
     * @param deviceId Id of the device info to fetch.
     * @return A GetDevice API request that can be executed.
     */
    public GetDevice get(final String deviceId) {
        return new GetDevice(deviceId);
    }

    public GetDevice get(final Device.Id deviceId) {
        return get(deviceId.getId());
    }

    public final class Add extends RestRequest<Device> {

        protected Add(Device request) {
            super(client, RequestMethod.POST, PATH,
                  ContentType.JSON, request,
                  StatusCode.CREATED, Device.class);
        }
    }

    /**
     * Creates an Add API request for a new Device with the provided data.
     *
     * @param projectId  Project this device belongs to [required].
     * @param deviceId   Desired device ID. If invalid or <tt>null</tt>, a random one will be
     *                   generated.
     * @param deviceName Project-unique name (optional).
     * @param deviceType Device type description (optional).
     * @param created    Creation date for this date; if null, current time will be used.
     * @return An Add API request that can be executed to add the device to the project.
     * @deprecated Use add(long, Device.Spec, Date) instead. Will be removed in next release.
     */
    @Deprecated
    public Add add(long projectId, String deviceId, String deviceName, String deviceType,
                   Date created) {
        return add(projectId, new Device.Spec(deviceId, deviceName, deviceType), created);
    }

    public Add add(long projectId, Device.Spec spec, Date created) {
        Date date = created == null ? new Date(System.currentTimeMillis()) : created;
        Device req = new Device(projectId, spec, date);
        return new Add(req);
    }

    public Add add(long projectId, Device.Spec spec) {
        return add(projectId, spec, null);
    }

    public Add add(long projectId) {
        return add(projectId, null, null);
    }


    public final class Delete extends RestRequest<Void> {

        protected Delete(final String deviceId) {
            super(client, RequestMethod.DELETE,
                  PATH + "/" + deviceId,
                  StatusCode.NO_CONTENT, Void.class);
        }
    }

    /**
     * Creates a Delete API request for a Device to be removed.
     *
     * @param deviceId The device ID of the Device to be removed.
     * @return A Delete API request that can be executed to remove the device.
     */
    public Delete delete(String deviceId) {
        return new Delete(deviceId);
    }

    public Delete delete(Device.Id deviceId) {
        return delete(deviceId.getId());
    }
}
