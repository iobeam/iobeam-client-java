package com.iobeam.api.auth;

import com.iobeam.api.ApiException;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Handler interface for refreshing auth tokens.
 */
public interface AuthHandler extends Callable<AuthToken> {

    public void setForceRefresh(boolean force);

    public AuthToken refreshToken() throws IOException, ApiException;
}
