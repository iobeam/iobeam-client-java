package com.iobeam.api.service;

import com.iobeam.api.client.IoBeam;
import com.iobeam.api.client.RestClient;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;
import com.iobeam.api.resource.Import;

import java.util.logging.Logger;

/**
 * Import service.
 */
public class Imports {

    private final static Logger logger = Logger.getLogger(Imports.class.getName());
    private final RestClient client;

    public Imports(final IoBeam client) {
        this.client = client.getRestClient();
    }

    public class Submit extends RestRequest<Void> {

        private static final String PATH = "/v1/imports";

        protected Submit(Import imp) {
            super(client, RequestMethod.POST, PATH + "/",
                  ContentType.JSON, imp,
                  StatusCode.OK, Void.class);
        }
    }

    public Submit submit(final Import request) {
        return new Submit(request);
    }
}
