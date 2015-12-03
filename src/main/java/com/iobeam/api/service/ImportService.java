package com.iobeam.api.service;

import com.iobeam.api.client.RestClient;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;
import com.iobeam.api.resource.DataBatch;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Import service.
 */
public class ImportService {

    private final static Logger logger = Logger.getLogger(ImportService.class.getName());
    final static int REQ_MAX_POINTS = 1000;


    private final RestClient client;

    public ImportService(final RestClient client) {
        this.client = client;
    }

    public class Submit extends RestRequest<Void> {

        private static final String PATH = "/v1/imports";

        protected Submit(ImportBatch imp) {
            super(client, RequestMethod.POST, PATH + "/?fmt=table",
                  ContentType.JSON, imp,
                  StatusCode.OK, Void.class);
        }
    }

    private Import cloneImportMetadata(Import imp) {
        return new Import(imp.getDeviceId(), imp.getProjectId());
    }

    public List<Submit> submitBatch(final Import request) {
        List<Submit> ret = new ArrayList<Submit>();

        return ret;
    }

    private List<ImportBatch> convertImportToImportBatchs(Import imp) {
        Map<String, Set<DataPoint>> store = imp.getSeries();
        List<ImportBatch> ret = new ArrayList<ImportBatch>();
        for (String name : store.keySet()) {
            DataBatch batch = new DataBatch(new String[]{name});
            for (DataPoint p : store.get(name)) {
                batch.add(p.getTime(), new String[]{name}, new Object[]{p.getValue()});
            }
            List<DataBatch> batches = batch.split(REQ_MAX_POINTS / batch.getColumns().size());
            for (DataBatch b : batches) {
                ret.add(new ImportBatch(imp.getProjectId(), imp.getDeviceId(), b, true));
            }
        }

        return ret;
    }

    public List<Submit> submit(final Import request) {
        List<Submit> ret = new ArrayList<Submit>();

        List<ImportBatch> reqs = convertImportToImportBatchs(request);
        for (ImportBatch r : reqs) {
            ret.add(new Submit(r));
        }

        return ret;
    }
}
