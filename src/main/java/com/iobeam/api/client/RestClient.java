package com.iobeam.api.client;

import com.iobeam.api.ApiException;
import com.iobeam.api.RestException;
import com.iobeam.api.auth.AuthException;
import com.iobeam.api.auth.AuthHandler;
import com.iobeam.api.auth.AuthToken;
import com.iobeam.api.auth.ProjectBearerAuthToken;
import com.iobeam.api.auth.UserBearerAuthToken;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestBuilder;
import com.iobeam.api.http.StatusCode;
import com.iobeam.api.resource.ResourceException;
import com.iobeam.api.resource.ResourceMapper;
import com.iobeam.util.concurrent.SameThreadExecutorService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class RestClient {

    protected final static Logger logger = Logger.getLogger(RestClient.class.getName());
    public static final String DEFAULT_DEV_API_HOST = "https://api-dev.iobeam.com";
    public static final String DEFAULT_API_HOST = "https://api.iobeam.com";
    private static final int MAX_HTTP_RETRIES = 3;
    private static final int DEFAULT_MAX_AUTH_ATTEMPTS = 3;
    private final URL url;
    private final CookieManager cookieManager;
    private final ExecutorService executor;
    private final ResourceMapper mapper = new ResourceMapper();
    private AtomicReference<AuthHandler> authHandler =
        new AtomicReference<AuthHandler>(null);
    private AtomicReference<AuthToken> authToken = new AtomicReference<AuthToken>(null);
    private volatile int maxAuthAttempts = DEFAULT_MAX_AUTH_ATTEMPTS;
    private volatile boolean enableGzip = true;

    public RestClient() {
        // Executor that executes on the calling thread.
        this(DEFAULT_API_HOST, null, new SameThreadExecutorService());
    }

    public RestClient(final String url) {
        this(url, null, new SameThreadExecutorService());
    }

    public RestClient(final CookieManager manager) {
        // Executor that executes on the calling thread.
        this(DEFAULT_API_HOST, manager, new SameThreadExecutorService());
    }

    public RestClient(final ExecutorService executor) {
        this(DEFAULT_API_HOST, null, executor);
    }

    public RestClient(final String url,
                      final ExecutorService executor) {
        this(url, null, executor);
    }

    public RestClient(final String url,
                      final CookieManager manager) {
        this(url, manager, new SameThreadExecutorService());
    }

    public RestClient(final String url,
                      final CookieManager cookieManager,
                      final ExecutorService executor) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad API server URL");
        }
        this.cookieManager = cookieManager;
        this.executor = executor;
    }

    public RestClient setEnableGzip(final boolean gzip) {
        this.enableGzip = gzip;
        return this;
    }

    public RestClient setAuthToken(final AuthToken token) {
        this.authToken.set(token);
        return this;
    }

    public RestClient setAuthenticationHandler(final AuthHandler handler) {
        this.authHandler.set(handler);
        return this;
    }

    public AuthHandler getAuthenticationHandler() {
        return authHandler.get();
    }

    public int getMaxAuthAttempts() {
        return maxAuthAttempts;
    }

    public RestClient setMaxAuthAttempts(final int maxAuthAttempts) {
        this.maxAuthAttempts = maxAuthAttempts;
        return this;
    }

    public AuthToken getAuthToken() {
        return authToken.get();
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }

    public boolean hasValidAuthToken() {
        final AuthToken token = authToken.get();
        return (token != null && token.isValid());
    }

    public URL getBaseUrl() {
        return url;
    }

    public ResourceMapper getMapper() {
        return mapper;
    }

    private boolean isContentType(final HttpURLConnection conn,
                                  final ContentType type) {
        return conn.getContentType() != null &&
               conn.getContentType().startsWith(type.getValue());
    }

    // Return true if caller should retry, false if give up
    private synchronized boolean refreshAuthToken(final AuthHandler handler,
                                                  final boolean forceRefresh) {

        if (handler == null) {
            return false;
        }

        AuthToken token = authToken.get();

        if (!forceRefresh && token != null && token.isValid()) {
            logger.fine("Auth token is already valid");
            return true;
        }

        try {
            handler.setForceRefresh(forceRefresh);
            token = handler.call();
            authToken.set(token);

            if (token != null) {
                if (token instanceof UserBearerAuthToken) {
                    final UserBearerAuthToken bt = (UserBearerAuthToken) token;
                    logger.info("Acquired auth token. Expires: "
                                + bt.getExpires() + " token="
                                + bt.getToken());
                } else if (token instanceof ProjectBearerAuthToken) {
                    ProjectBearerAuthToken pt = (ProjectBearerAuthToken) token;
                    logger.info("Acquired proj token. Expires: " + pt.getExpires() + " token="
                                + pt.getToken());
                } else {
                    logger.info("Acquired auth token. Token valid="
                                + token.isValid());
                }
            }
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();

            if (cause != null) {
                logger.warning("Authentication failed: " + cause.getMessage());
            } else {
                logger.warning("Authentication failed: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.warning("Authentication failed: " + e.getMessage());
        } finally {
            handler.setForceRefresh(false);
        }
        return true;
    }

    private JSONObject readJson(final InputStream in) throws IOException, JSONException {

        final StringBuilder json = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;

        while ((line = reader.readLine()) != null) {
            json.append(line);
        }

        return new JSONObject(json.toString());
    }

    private <T> T readContent(final HttpURLConnection conn,
                              final StatusCode statusCode,
                              final Class<T> responseClass)
        throws ApiException, IOException {

        InputStream in = null;
        boolean error = false;

        if (conn.getContentLength() <= 0) {
            logger.fine("Content length is " + conn.getContentLength());
            return null;
        }

        final boolean gzipEncoding = "gzip".equalsIgnoreCase(conn.getContentEncoding());

        try {
            try {
                if (gzipEncoding) {
                    in = new GZIPInputStream(conn.getInputStream());
                } else {
                    in = conn.getInputStream();
                }
            } catch (IOException e) {
                if (gzipEncoding) {
                    in = new GZIPInputStream(conn.getErrorStream());
                } else {
                    in = conn.getErrorStream();
                }
                error = true;
            }

            if (in == null) {
                logger.fine("Could not get an input stream");
                return null;
            }

            if (isContentType(conn, ContentType.JSON)) {
                try {
                    if (error) {
                        if (statusCode == StatusCode.UNAUTHORIZED) {
                            throw new AuthException(mapper.fromJson(readJson(in),
                                                                    RestError.class));
                        }
                        throw new RestException(statusCode,
                                                mapper.fromJson(readJson(in),
                                                                RestError.class));
                    }
                    return mapper.fromJson(readJson(in), responseClass);
                } catch (JSONException e) {
                    throw new ResourceException("Content is not valid JSON");
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        logger.fine("Unexpected content type: " + conn.getContentType());

        return null;
    }

    public <T> T executeRequest(final RequestBuilder builder,
                                final StatusCode expectedStatusCode,
                                final Class<T> responseClass,
                                final boolean needAuth)
        throws IOException, ApiException {

        final Object content = builder.getContent();
        byte[] output = null;

        if (content == null) {
            builder.setContentLength(0)
                .addHeader("Content-Length", "0");
        } else if (builder.getContentType() == ContentType.JSON) {
            output = mapper.toJsonBytes(content);
            builder.setContentLength(output.length);
        } else if (builder.getContentType() == ContentType.URLENCODED) {
            output = content.toString().getBytes("UTF-8");
            builder.setContentLength(output.length);
        }

        builder.setEnableGzip(enableGzip);

        /*
            Do connection retries due to weird behavior in Android's HttpURLConnection.
            Apparently, a connection can be reused although the server has closed it, causing
            a EOFException when trying to read the response.

            http://stackoverflow.com/questions/17208336/getting-java-io-eofexception-using-httpurlconnection
         */
        T result = null;
        HttpURLConnection conn = null;
        DataOutputStream out = null;
        int retryCount = 0;
        boolean forceRefreshToken = false;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                conn = builder.build();

                if (needAuth) {
                    AuthToken token = authToken.get();
                    final AuthHandler handler = authHandler.get();

                    if ((token == null || !token.isValid()) && handler != null) {
                        refreshAuthToken(handler, forceRefreshToken);
                    }

                    token = authToken.get();

                    if (token != null && token.isValid()) {
                        conn.setRequestProperty("Authorization",
                                                token.getType() + " " +
                                                token.getToken());
                    }
                }

                conn.connect();
                logger.info(conn.getRequestMethod() + " " + conn.getURL());

                if (output != null) {
                    out = new DataOutputStream(conn.getOutputStream());
                    out.write(output);
                    out.flush();
                    out.close();
                    out = null;
                }

                if (conn.getDoInput()) {
                    final StatusCode statusCode = StatusCode.fromValue(conn.getResponseCode());

                    if (statusCode == StatusCode.UNAUTHORIZED) {
                        logger.info("Authentication failure (401)");
                        if ((retryCount++ < MAX_HTTP_RETRIES)) {
                            forceRefreshToken = true;
                            setAuthToken(null);
                            continue;
                        }
                    }

                    // Read the content. This will return null if no content,
                    // throw an exception in case of error message,
                    // or return the expected content object
                    result = readContent(conn,
                                         statusCode,
                                         responseClass);

                    if (statusCode != expectedStatusCode) {
                        logger.fine("Status code: " + statusCode);
                        throw new ApiException("Expected response code "
                                               + expectedStatusCode
                                               + " got " + statusCode);
                    }
                } else {
                    logger.fine("Connection can't do input");
                }
                logger.fine("Request successful.");
                break;
            } catch (EOFException e) {
                if (retryCount++ == MAX_HTTP_RETRIES) {
                    throw e;
                }
                logger.warning("Request failed, retrying... (" + retryCount + ")");
            } catch (RuntimeException e) {
                logger.fine("Got exception: " + e.getMessage());
                throw e;
            } finally {

                if (out != null) {
                    out.close();
                    out = null;
                }

                if (conn != null) {
                    conn.disconnect();
                    conn = null;
                }
            }
        }

        if (result == null && !responseClass.equals(Void.class)) {
            throw new ApiException("Unexpected empty response");
        }
        return result;
    }

    public <V> Future<V> submit(final Callable<V> callable) {
        return executor.submit(callable);

    }

    public ExecutorService getExecutorService() {
        return executor;
    }
}
