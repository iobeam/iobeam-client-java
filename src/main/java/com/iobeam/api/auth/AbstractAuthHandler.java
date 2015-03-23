package com.iobeam.api.auth;

import com.iobeam.api.ApiException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Handler for implementing authentication.
 */
public abstract class AbstractAuthHandler implements AuthHandler {

    private final static Logger logger = Logger.getLogger(AbstractAuthHandler.class.getName());
    private final String authTokenFilePath;
    private boolean forceRefresh = false;

    public AbstractAuthHandler() {
        this("auth.token");
    }

    public AbstractAuthHandler(final String authTokenFilePath) {
        this.authTokenFilePath = authTokenFilePath;
    }

    protected AuthToken readToken() {
        if (authTokenFilePath == null) {
            return null;
        }

        try {
            final AuthToken token = AuthUtils.readToken(authTokenFilePath);

            if (token.isValid()) {
                logger.info("read valid auth token from file '" + authTokenFilePath + "'");
            }
            return token;
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    protected void writeToken(final AuthToken token) {
        if (authTokenFilePath == null) {
            return;
        }

        try {
            AuthUtils.writeToken(token, authTokenFilePath);
            logger.info("wrote auth token to file '" + authTokenFilePath + "'");
        } catch (IOException e) {
            // Ignore
        }
    }

    @Override
    public void setForceRefresh(final boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public abstract AuthToken refreshToken() throws IOException, ApiException;

    @Override
    public AuthToken call() throws Exception {

        if (!forceRefresh) {
            final AuthToken token = readToken();

            if (token != null && token.isValid()) {
                return token;
            }
        }

        final AuthToken token = refreshToken();

        if (token != null && token.isValid()) {
            logger.info("Refreshed auth token: " + token);
            writeToken(token);
            return token;
        }

        return null;
    }
}
