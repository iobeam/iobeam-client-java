package com.iobeam.api.client;

import com.iobeam.api.resource.DataBatch;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;
import com.iobeam.api.service.ImportService;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SendCallbackTest {

    private static final long PROJECT_ID = 0;
    private static final String DEVICE_ID = "fake_device_identifier";

    private static final DataPoint dp1 = new DataPoint(1, 1);

    private ImportService.Submit getSubmitReq() {
        Import imp = new Import(DEVICE_ID, PROJECT_ID);
        imp.addDataPoint("series1", dp1);

        RestClient client = new RestClient();
        ImportService service = new ImportService(client);
        return service.submit(imp).get(0);
    }

    @Test
    public void testOnSuccess() throws Exception {
        ImportService.Submit req = getSubmitReq();

        SendCallback cb = new SendCallback() {
            @Override
            public void onSuccess(ImportBatch data) {
                DataBatch db = data.getData();
                assertEquals(1, db.getColumns().size());
                assertTrue(db.getColumns().contains("series1"));
                assertEquals(1, db.getRows().size());
            }

            @Override
            public void onFailure(Throwable exc, ImportBatch data) {
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
            public void onSuccess(ImportBatch data) {
                // not used.
            }

            @Override
            public void onFailure(Throwable exc, ImportBatch data) {
                DataBatch db = data.getData();
                assertEquals(1, db.getColumns().size());
                assertTrue(db.getColumns().contains("series1"));
                assertEquals(1, db.getRows().size());
            }
        };
        cb.innerCallback.failed(null, req);
    }
}
