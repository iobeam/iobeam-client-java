package com.iobeam.api.service;

import com.iobeam.api.client.RestClient;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;
import com.iobeam.api.resource.DataStore;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;
import com.iobeam.api.resource.ImportBatch;

import java.util.ArrayList;
import java.util.Collections;
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
            DataStore batch = new DataStore(new String[]{name});
            for (DataPoint p : store.get(name)) {
                batch.add(p.getTime(), new String[]{name}, new Object[]{p.getValue()});
            }
            List<DataStore> batches = batch.split(REQ_MAX_POINTS / batch.getColumns().size());
            for (DataStore b : batches) {
                ret.add(new ImportBatch(imp.getProjectId(), imp.getDeviceId(), b, true));
            }
        }

        return ret;
    }

    private List<ImportBatch> splitBigImportBatches(ImportBatch imp) {
        List<ImportBatch> ret = new ArrayList<ImportBatch>();

        DataStore batch = imp.getData();
        List<DataStore> batches = batch.split(REQ_MAX_POINTS / batch.getColumns().size());
        if (batches.size() == 1) {
            ret.add(imp);
        } else {
            for (DataStore b : batches) {
                ret.add(new ImportBatch(imp.getProjectId(), imp.getDeviceId(), b));
            }
        }
        return ret;
    }

    public List<Submit> submit(final Import request) {
        return submit(request, Collections.<ImportBatch>emptyList());
    }

    public List<Submit> submit(final List<ImportBatch> batches) {
        return submit(null, batches);
    }

    private List<Submit> submit(final Import request, final List<ImportBatch> batches) {
        List<Submit> ret = new ArrayList<Submit>();

        List<ImportBatch> reqs = new ArrayList<ImportBatch>();
        if (request != null) {
            reqs.addAll(convertImportToImportBatchs(request));
        }
        if (batches != null) {
            for (ImportBatch ib : batches) {
                reqs.addAll(splitBigImportBatches(ib));
            }
        }

        for (ImportBatch r : reqs) {
            ret.add(new Submit(r));
        }

        return ret;
    }
}
