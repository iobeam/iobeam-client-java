package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.auth.AbstractAuthHandler;
import com.iobeam.api.auth.AuthHandler;
import com.iobeam.api.auth.AuthToken;
import com.iobeam.api.auth.UserBearerAuthToken;
import com.iobeam.api.http.RequestBuilder;
import com.iobeam.api.http.StatusCode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RestClientTest {

    @Spy
    private final RequestBuilder reqBuilder =
        new RequestBuilder("http://localhost:14634/foo");

    @Spy
    private final RestClient client = new RestClient();

    private AuthToken validToken;

    private AuthToken expiredToken = new UserBearerAuthToken(0, "user", new Date(0));

    private HttpURLConnection conn;

    @Before
    public void setUp() throws Exception {
        conn = spy(reqBuilder.build());
        doReturn(conn).when(reqBuilder).build();
        doNothing().when(conn).connect();
        doReturn(401).when(conn).getResponseCode();
        doReturn(0).when(conn).getContentLength();
        doReturn(true).when(conn).getDoInput();
        validToken = new UserBearerAuthToken(0, "user", new Date(
            System.currentTimeMillis() + 60000));
    }

    @Test
    public void testRequestWithExpiredAuthTokenShouldRefreshToken() throws Exception {

        final AuthHandler handler = spy(new AbstractAuthHandler() {
            @Override
            protected AuthToken readToken() {
                return null;
            }

            @Override
            protected void writeToken(AuthToken token) {
            }

            @Override
            public AuthToken refreshToken() throws IOException, ApiException {
                doReturn(200).when(conn).getResponseCode();
                return validToken;
            }
        });
        client.setAuthenticationHandler(handler);
        client.setAuthToken(expiredToken);
        assertFalse(client.hasValidAuthToken());
        client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        verify(handler).refreshToken();
        assertTrue(client.hasValidAuthToken());
    }

    @Test
    public void testRequestWithExpiredAuthTokenShouldReadToken() throws Exception {

        final AuthHandler handler = spy(new AbstractAuthHandler() {
            @Override
            protected AuthToken readToken() {
                try {
                    doReturn(200).when(conn).getResponseCode();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return validToken;
            }

            @Override
            protected void writeToken(AuthToken token) {
            }

            @Override
            public AuthToken refreshToken() throws IOException, ApiException {
                return null;
            }
        });
        client.setAuthenticationHandler(handler);
        client.setAuthToken(expiredToken);
        assertFalse(client.hasValidAuthToken());
        client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        verify(handler, times(0)).refreshToken();
        assertTrue(client.hasValidAuthToken());
    }

    @Test
    public void testRequestWithExpiredAuthTokenShouldForceRefresh() throws Exception {

        final AuthHandler handler = spy(new AbstractAuthHandler() {
            int count = 0;

            @Override
            protected AuthToken readToken() {
                return null;
            }

            @Override
            protected void writeToken(AuthToken token) {
            }

            @Override
            public AuthToken refreshToken() throws IOException, ApiException {
                // Do not return a valid token on the first two refreshes
                // to force a (fake) 401 server response that requests a valid token
                if (count++ < 2) {
                    return expiredToken;
                }
                doReturn(200).when(conn).getResponseCode();
                return validToken;
            }
        });
        client.setAuthenticationHandler(handler);
        client.setAuthToken(expiredToken);
        assertFalse(client.hasValidAuthToken());
        client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        verify(handler, times(3)).refreshToken();
        assertTrue(client.hasValidAuthToken());
    }

}