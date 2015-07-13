package com.iobeam.api.service;

import com.iobeam.api.client.RestClient;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;
import com.iobeam.api.resource.DataPoint;
import com.iobeam.api.resource.Import;

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

        protected Submit(Import imp) {
            super(client, RequestMethod.POST, PATH + "/",
                  ContentType.JSON, imp,
                  StatusCode.OK, Void.class);
        }
    }

    private Import cloneImportMetadata(Import imp) {
        return new Import(imp.getDeviceId(), imp.getProjectId());
    }

    public List<Submit> submit(final Import request) {
        List<Submit> ret = new ArrayList<Submit>();

        // Here we decide whether this request needs to be split up.
        long totalSize = request.getTotalSize();
        // Request is sufficiently small, send all at once.
        if (totalSize <= REQ_MAX_POINTS) {
            ret.add(new Submit(request));
        } else {  // Request is too big.
            Map<String, Set<DataPoint>> data = request.getSeries();

            // For each series, we see if that series worth of points is small enough to send as
            // a request. If so, we do that. Otherwise, we split the series up into multiple reqs,
            // that are at most `REQ_MAX_POINTS`.
            for (String k : data.keySet()) {
                Set<DataPoint> pts = data.get(k);

                if (pts.size() > REQ_MAX_POINTS) {
                    Import temp = cloneImportMetadata(request);
                    int i = 0;
                    for (DataPoint d : pts) {
                        temp.addDataPoint(k, d);
                        i++;
                        // Request is full, add to list and create next one.
                        if (i == REQ_MAX_POINTS) {
                            ret.add(new Submit(temp));
                            temp = cloneImportMetadata(request);
                            i = 0;
                        }
                    }
                    // Add to list if any points were added to last one.
                    if (i > 0) {
                        ret.add(new Submit(temp));
                    }
                } else {  // Series fits in one request.
                    Import temp = cloneImportMetadata(request);
                    temp.addDataPointSet(k, pts);
                    ret.add(new Submit(temp));
                }
            }

        }
        return ret;
    }
}
