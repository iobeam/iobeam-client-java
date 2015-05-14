package com.iobeam.api.auth;

import com.iobeam.api.ApiException;
import com.iobeam.api.client.RestClient;
import com.iobeam.api.service.Tokens;

import java.io.File;
import java.io.IOException;

/**
 * Default AuthHandler implementation.
 */
public class DefaultAuthHandler extends AbstractAuthHandler {

    private static final String FMT_DEFAULT_PATH = "project_%d.authtoken";

    private RestClient client;
    private final String projectToken;

    private DefaultAuthHandler(final RestClient client,
                               final String projectToken,
                               final String storagePath) {
        super(storagePath);
        this.projectToken = projectToken;
        this.client = client;
        if (client != null) {
            client.setAuthenticationHandler(this);
        }
    }

    public DefaultAuthHandler(final RestClient client, final long projectId,
                              final String projectToken, final File storageDir) {
        this(client, projectToken, storageDir == null ? null : new File(
            storageDir, String.format(FMT_DEFAULT_PATH, projectId)).getAbsolutePath());
    }

    public DefaultAuthHandler(final RestClient client, final long projectId,
                              final String projectToken) {
        this(client, projectId, projectToken, null);
    }

    public DefaultAuthHandler(final long projectId, final String projectToken) {
        this(null, projectId, projectToken);
    }

    @Override
    public AuthToken refreshToken() throws IOException, ApiException {
        if (client != null) {
            Tokens service = new Tokens(client);
            Tokens.RefreshProjectToken req = service.refreshProjectToken(projectToken);
            return req.execute();
        }
        return null;
    }
}
