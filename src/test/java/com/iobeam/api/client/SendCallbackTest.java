package com.iobeam.api.client;

import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.service.ImportService;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SendCallbackTest {

    private static final long PROJECT_ID = 0;
    private static final String DEVICE_ID = "fake_device_identifier";

    private static final DataPoint dp1 = new DataPoint(1);
    private static final DataPoint dp2 = new DataPoint(5);
    private static final DataPoint dp3 = new DataPoint(222);

    private ImportService.Submit getSubmitReq() {
        Import imp = new Import(DEVICE_ID, PROJECT_ID);
        imp.addDataPoint("series1", dp1);
        imp.addDataPoint("series2", dp2);
        imp.addDataPoint("series2", dp3);

        RestClient client = new RestClient();
        ImportService service = new ImportService(client);
        return service.submit(imp).get(0);
    }

    @Test
    public void testOnSuccess() throws Exception {
        ImportService.Submit req = getSubmitReq();

        SendCallback cb = new SendCallback() {
            @Override
            public void onSuccess(Map<String, Set<DataPoint>> data) {
                assertTrue(data.containsKey("series1"));
                assertTrue(data.containsKey("series2"));
                assertEquals(1, data.get("series1").size());
                assertEquals(2, data.get("series2").size());
                assertTrue(data.get("series1").contains(dp1));
                assertTrue(data.get("series2").contains(dp2));
                assertTrue(data.get("series2").contains(dp3));
            }

            @Override
            public void onFailure(Throwable exc, Map<String, Set<DataPoint>> data) {
                // not used.
            }
        };
        cb.innerCallback.completed(null, req);
    }

    @Test
    public void testOnFailure() throws Exception {
        ImportService.Submit req = getSubmitReq();

        SendCallback cb = new SendCallback() {
            @Override
            public void onSuccess(Map<String, Set<DataPoint>> data) {
                // not used.
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
