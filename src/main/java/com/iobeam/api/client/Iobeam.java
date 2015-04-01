package com.iobeam.api.client;

import com.iobeam.api.auth.DefaultAuthHandler;

import java.util.concurrent.Executors;

/**
 * The Iobeam client. An instance of this is initialized with <tt>init()</tt> for a particular
 * project. This instance is passed to services (for example, Imports).
 */
public class Iobeam {

    public static final String API_URL = "https://api.iobeam.com";
    private static final String REQUEST_LOG = "iobeam-api-test-request.log";

    public static Iobeam init(long projectId, String projectToken) {
        return new Iobeam(projectId, projectToken);
    }

    private RestClient mRestClient;

    private Iobeam(long projectId, String projectToken) {
        mRestClient = new RestClient(API_URL, null,
                                     Executors.newSingleThreadExecutor()).setAuthenticationHandler(
            new DefaultAuthHandler(projectId, projectToken));

    }

    public RestClient getRestClient() {
        return mRestClient;
    }
}
