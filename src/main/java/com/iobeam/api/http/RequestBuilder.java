package com.iobeam.api.http;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Easily build URLConnections with a specific configuration.
 */
public class RequestBuilder implements Serializable {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 4000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 2000;
    private final String url;
    private RequestMethod method = RequestMethod.GET;
    private Object content = null;
    private long contentLength = 0;
    private ContentType contentType = ContentType.NONE;
    private final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private final Map<String, Object> parameters = new HashMap<String, Object>();
    private boolean doInput = true;
    private boolean doOutput = false;
    private boolean enableGzip = true;
    private int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

    public RequestBuilder(final String url) {
        this.url = url;
    }

    public RequestBuilder(final RequestBuilder builder) {
        this.url = builder.url;
        this.headers.putAll(builder.headers);
        this.parameters.putAll(builder.parameters);
        this.doInput = builder.doInput;
        this.doOutput = builder.doOutput;
        this.connectTimeoutMillis = builder.connectTimeoutMillis;
        this.readTimeoutMillis = builder.readTimeoutMillis;
        this.method = builder.method;
        this.content = builder.content;
        this.contentLength = builder.contentLength;
        this.contentType = builder.contentType;
    }

    public HttpURLConnection build() throws IOException {
        /*
        In case we want to use OkHttp with SPDY support:

        final OkUrlFactory urlFactory = new OkUrlFactory(new OkHttpClient());
        final HttpURLConnection conn = urlFactory.open(new URL(buildUrl()));
        */
        final HttpURLConnection conn = (HttpURLConnection) new URL(buildUrl()).openConnection();

        conn.setDoInput(doInput);
        conn.setDoOutput(doOutput);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setRequestMethod(method.name());
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Accept", "application/json");

        if (enableGzip) {
            conn.setRequestProperty("Accept-Encoding", "gzip");
        }

        if (contentType != ContentType.NONE) {
            conn.addRequestProperty("Content-Type", contentType.getValue());
        }

        if (doOutput && contentLength > 0 && contentLength <= Integer.MAX_VALUE) {
            // Must cast contentLength to int here, since long version of
            // setFixedLengthStreamingMode isn't available early versions of
            // Android
            conn.setFixedLengthStreamingMode((int) contentLength);
        }

        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (final String value : entry.getValue()) {
                conn.addRequestProperty(entry.getKey(), value);
            }
        }

        return conn;
    }

    public RequestMethod getMethod() {
        return method;
    }

    private RequestBuilder addHeader(final String name,
                                     final String value,
                                     final boolean clear) {
        List<String> values = headers.get(name);

        if (values == null) {
            values = new LinkedList<String>();
        } else if (clear) {
            values.clear();
        }
        values.add(value);
        headers.put(name, values);

        return this;
    }

    public RequestBuilder addHeader(final String name,
                                    final String value) {
        addHeader(name, value, false);
        return this;
    }

    public RequestBuilder setHeader(final String name,
                                    final String value) {
        addHeader(name, value, true);
        return this;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public RequestBuilder setReadTimeout(final int millis) {
        readTimeoutMillis = millis;
        return this;
    }

    public RequestBuilder setConnectTimeout(final int millis) {
        connectTimeoutMillis = millis;
        return this;
    }

    public RequestBuilder setEnableGzip(final boolean enableGzip) {
        this.enableGzip = enableGzip;
        return this;
    }

    public RequestBuilder setDoInput(final boolean doInput) {
        this.doInput = doInput;
        return this;
    }

    public RequestBuilder setDoOutput(final boolean doOutput) {
        this.doOutput = doOutput;
        return this;
    }

    public RequestBuilder setRequestMethod(final RequestMethod method) {
        this.method = method;
        return this;
    }

    public RequestBuilder setContent(final Object content) {
        this.content = content;

        if (content != null) {
            setDoOutput(true);
        } else {
            setDoOutput(false);
        }
        return this;
    }

    public Object getContent() {
        return content;
    }

    public RequestBuilder setContentType(final ContentType type) {
        contentType = type;
        return this;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public RequestBuilder setContentLength(final long length) {
        this.contentLength = length;

        if (length > 0) {
            setDoOutput(true);
        } else {
            setDoOutput(false);
        }
        return this;
    }

    public String getBaseUrl() {
        return url;
    }

    private String buildUrl() {
        final StringBuilder builder = new StringBuilder(url);
        final Iterator<Map.Entry<String, Object>> it = parameters.entrySet().iterator();

        if (it.hasNext()) {
            builder.append("?");
        }

        while (it.hasNext()) {
            final Map.Entry<String, Object> entry = it.next();

            try {
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));

                if (it.hasNext()) {
                    builder.append("&");
                }
            } catch (UnsupportedEncodingException e) {
                // Ignore, potentially log.
            }
        }

        return builder.toString();
    }

    public void addParameter(final String key, final String value) {
        if (key != null && value != null) {
            parameters.put(key, value);
        }
    }

    public void addParameter(final String key, final int value) {
        if (key != null) {
            parameters.put(key, value);
        }
    }

    public void addParameter(final String key, final long value) {
        if (key != null) {
            parameters.put(key, value);
        }
    }

    public void addParameter(final String key, final boolean value) {
        if (key != null) {
            parameters.put(key, value);
        }
    }

    @Override
    public String toString() {
        return "RequestBuilder{" +
               "url='" + url + '\'' +
               ", content=" + content +
               ", method=" + method +
               ", contentLength=" + contentLength +
               ", contentType=" + contentType +
               ", doOutput=" + doOutput +
               ", doInput=" + doInput +
               ", connectTimeoutMillis=" + connectTimeoutMillis +
               ", readTimeoutMillis=" + readTimeoutMillis +
               '}';
    }
}
