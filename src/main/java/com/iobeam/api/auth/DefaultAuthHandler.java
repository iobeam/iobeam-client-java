package com.iobeam.api.auth;

import com.iobeam.api.ApiException;
import com.iobeam.api.client.RestClient;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Default AuthHandler implementation.
 */
public class DefaultAuthHandler extends AbstractAuthHandler {

    private static final String FMT_DEFAULT_PATH = "project_%d.authtoken";

    private final RestClient client;
    private final long projectId;
    private final String projectToken;

    public DefaultAuthHandler(final RestClient client,
                              final long projectId,
                              final String projectToken,
                              final String storagePath) {
        super(storagePath);
        this.client = client;
        this.projectId = projectId;
        this.projectToken = projectToken;
    }

    public DefaultAuthHandler(final RestClient client,
                              final long projectId,
                              final String projectToken) {
        this(client, projectId, projectToken, String.format(FMT_DEFAULT_PATH,  projectId));
    }


    public DefaultAuthHandler(final String apiServer,
                              final long projectId,
                              final String projectToken) {
        this(new RestClient(apiServer), projectId, projectToken, String.format(FMT_DEFAULT_PATH,  projectId));
    }

    public DefaultAuthHandler(final long projectId,
                              final String projectToken) {
        this(new RestClient(), projectId, projectToken, String.format(FMT_DEFAULT_PATH,  projectId));
    }

    @Override
    public AuthToken refreshToken() throws IOException, ApiException {
        return new ProjectBearerAuthToken(projectId, projectToken, new Date(
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
    }
}
