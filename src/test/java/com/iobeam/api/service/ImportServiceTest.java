package com.iobeam.api.service;

import com.iobeam.api.client.RestClient;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;

import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ImportServiceTest {

    private static final String TEST_DEVICE_ID = "test1234only5678";

    private final ImportService service = new ImportService(new RestClient());

    private ImportBatch getSubmitData(ImportService.Submit req) {
        return (ImportBatch) req.getBuilder().getContent();
    }

    private long getSubmitSize(ImportService.Submit req) {
        return getSubmitData(req).getData().getDataSize();
    }

    @Test
    public void testSmallImport() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);

        for (int i = 0; i < ImportService.REQ_MAX_POINTS; i++) {
            imp.addDataPoint("series1", new DataPoint(i, i));
        }
        List<ImportService.Submit> reqs = service.submit(imp);
        assertEquals(1, reqs.size());
    }

    @Test
    public void testSingleBigSeriesImport() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        long total = ImportService.REQ_MAX_POINTS * 2;

        for (int i = 0; i < total; i++) {
            imp.addDataPoint("series1", new DataPoint(i, i));
        }
        List<ImportService.Submit> reqs = service.submit(imp);
        assertEquals(2, reqs.size());
        assertEquals(ImportService.REQ_MAX_POINTS, getSubmitSize(reqs.get(0)));
        assertEquals(ImportService.REQ_MAX_POINTS, getSubmitSize(reqs.get(1)));
    }

    @Test
    public void testSmallEnoughSeries() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        long total = (long) (ImportService.REQ_MAX_POINTS * 1.5);
        long midway = total / 2;

        for (int i = 0; i < total; i++) {
            if (i < midway) {
                imp.addDataPoint("series1", new DataPoint(i, i));
            } else {
                imp.addDataPoint("series2", new DataPoint(i, i));
            }
        }

        List<ImportService.Submit> reqs = service.submit(imp);
        assertEquals(2, reqs.size());
        assertEquals(midway, getSubmitSize(reqs.get(0)));
        assertEquals(total - midway, getSubmitSize(reqs.get(1)));
    }

    @Test
    public void testOneSmallOneBigSeries() throws Exception {
        Import imp = new Import(TEST_DEVICE_ID, 1000);
        long total = (long) (ImportService.REQ_MAX_POINTS * 2);
        long midway = ImportService.REQ_MAX_POINTS / 2;

        for (int i = 0; i < total; i++) {
            if (i < midway) {
                imp.addDataPoint("series1", new DataPoint(i, i));
            } else {
                imp.addDataPoint("series2", new DataPoint(i, i));
            }
        }

        List<ImportService.Submit> reqs = service.submit(imp);
        assertEquals(3, reqs.size());

        long series1Size = 0;
        series1Size += getSubmitData(reqs.get(0)).getData().getColumns().contains("series1") ?
                       getSubmitSize(reqs.get(0)) : 0;
        series1Size += getSubmitData(reqs.get(1)).getData().getColumns().contains("series1") ?
                       getSubmitSize(reqs.get(1)) : 0;
        series1Size += getSubmitData(reqs.get(2)).getData().getColumns().contains("series1") ?
                       getSubmitSize(reqs.get(2)) : 0;

        long series2Size = 0;
        series2Size += getSubmitData(reqs.get(0)).getData().getColumns().contains("series2") ?
                       getSubmitSize(reqs.get(0)) : 0;
        series2Size += getSubmitData(reqs.get(1)).getData().getColumns().contains("series2") ?
                       getSubmitSize(reqs.get(1)) : 0;
        series2Size += getSubmitData(reqs.get(2)).getData().getColumns().contains("series2") ?
                       getSubmitSize(reqs.get(2)) : 0;

        assertEquals(midway, series1Size);
        assertEquals(total - midway, series2Size);
    }
}
