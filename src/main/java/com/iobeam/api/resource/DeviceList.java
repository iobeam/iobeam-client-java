package com.iobeam.api.resource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.List;

/**
 * Collection of devices.
 */
public class DeviceList extends ResourceList<Device> implements Serializable {

    private static final String KEY_LIST = "devices";

    public List<Device> getDevices() {
        return getList();
    }

    public static DeviceList fromJson(final JSONObject json)
        throws ParseException {

        final DeviceList list = new DeviceList();
        final JSONArray arr = json.getJSONArray(KEY_LIST);

        for (int i = 0; i < arr.length(); i++) {
            list.add(Device.fromJson(arr.getJSONObject(i)));
        }

        return list;
    }
}
