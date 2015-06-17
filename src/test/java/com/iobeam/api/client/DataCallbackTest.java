package com.iobeam.api.client;

import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.service.Imports;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class DataCallbackTest {

    private static final long PROJECT_ID = 0;
    private static final String DEVICE_ID = "fake_device_identifier";

    @Test
    public void testOnFailure() throws Exception {
        Import imp = new Import(DEVICE_ID, PROJECT_ID);
        final DataPoint dp1 = new DataPoint(1);
        final DataPoint dp2 = new DataPoint(5);
        final DataPoint dp3 = new DataPoint(222);
        imp.addDataPoint("series1", dp1);
        imp.addDataPoint("series2", dp2);
        imp.addDataPoint("series2", dp3);

        RestClient client = new RestClient();
        Imports service = new Imports(client);
        Imports.Submit req = service.submit(imp).get(0);

        DataCallback cb = new DataCallback() {
            @Override
            public void onSuccess() {
                // do nothing
            }

            @Override
            public void onFailure(Throwable exc, Map<String, Set<DataPoint>> data) {
                assertTrue(data.containsKey("series1"));
                assertTrue(data.containsKey("series2"));
                assertEquals(1, data.get("series1").size());
                assertEquals(2, data.get("series2").size());
                assertTrue(data.get("series1").contains(dp1));
                assertTrue(data.get("series2").contains(dp2));
                assertTrue(data.get("series2").contains(dp3));
            }
        };
        cb.innerCallback.failed(null, req);
    }
}
