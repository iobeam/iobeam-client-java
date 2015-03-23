package com.iobeam.api.client;

import com.iobeam.api.http.RequestBuilder;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OfflineManagerTest {

    private static final String OFFLINE_LOG = "__unit_test_offline.log";

    private OfflineManager offMan = null;

    private final RequestBuilderMock reqBuilder =
        new RequestBuilderMock("http://localhost:14634/foo");

    private static class RequestBuilderMock extends RequestBuilder {

        private boolean networkError = false;

        public RequestBuilderMock(String url) {
            super(url);
        }

        @Override
        public HttpURLConnection build() throws IOException {
            final HttpURLConnection conn = spy(super.build());
            doNothing().when(conn).connect();
            if (networkError) {
                doThrow(new UnknownHostException("unknown host")).when(conn).getResponseCode();
            } else {
                doReturn(200).when(conn).getResponseCode();
            }
            doReturn(0).when(conn).getContentLength();
            doReturn(true).when(conn).getDoInput();
            return conn;
        }

        public void setNetworkError(final boolean value) {
            networkError = value;
        }
    }

    @Before
    public void setUp() throws Exception {
        new File(OFFLINE_LOG).delete();
        offMan = new OfflineManager(OFFLINE_LOG);
        reqBuilder.setRequestMethod(RequestMethod.POST);
    }

    @After
    public void tearDown() throws Exception {
        new File(OFFLINE_LOG).delete();

        if (offMan != null) {
            offMan.destroy();
        }
    }

    @Test
    public void testOffline() throws Exception {
        final RestClient client = new RestClient(offMan);

        client.executeRequest(reqBuilder, StatusCode.OK, Void.class);

        reqBuilder.setNetworkError(true);

        try {
            // Should throw network exception
            client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        } catch (IOException e) {
            // ignore
        }

        try {
            // Should throw network exception
            client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        } catch (IOException e) {
            // ignore
        }

        assertEquals(2, offMan.getOfflineRequests().size());

        for (OfflineManager.RequestRecord rr : offMan.getOfflineRequests()) {
            assertEquals(StatusCode.OK, rr.getExpectedCode());
            assertEquals(Void.class, rr.getResponseClass());
        }

        try {
            // Should throw network exception
            client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        } catch (IOException e) {
            // ignore
        }

        assertEquals(1, offMan.getOfflineRequests().size());
    }

    @Test
    public void testTruncateLog() throws Exception {
        final RestClient client = new RestClient(offMan);

        reqBuilder.setNetworkError(true);

        try {
            // Should throw network exception
            client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        } catch (IOException e) {
            // ignore
        }

        assertFalse(offMan.getLog().isEmpty());
        offMan.truncateLog();
        assertTrue(offMan.getLog().isEmpty());

        try {
            // Should throw network exception
            client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        } catch (IOException e) {
            // ignore
        }

        assertEquals(1, offMan.getOfflineRequests().size());
    }

    @Test
    public void testManuallySetReadMark() throws Exception {
        final RestClient client = new RestClient(offMan);

        reqBuilder.setNetworkError(true);

        for (int i = 0; i < 2; i++) {
            try {
                // Should throw network exception
                client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
            } catch (IOException e) {
                // ignore
            }
        }

        assertEquals(2, offMan.getOfflineRequests(false).size());

        try {
            // Should throw network exception
            client.executeRequest(reqBuilder, StatusCode.OK, Void.class);
        } catch (IOException e) {
            // ignore
        }

        assertEquals(3, offMan.getOfflineRequests(false).size());

        long mark = offMan.getOfflineRequests(false).iterator().next().getLogPosition();

        offMan.setMark(mark);

        assertEquals(2, offMan.getOfflineRequests(false).size());

        Iterator<OfflineManager.RequestRecord> it = offMan.getOfflineRequests(false).iterator();
        it.next();
        mark = it.next().getLogPosition();

        offMan.setMark(mark);

        assertEquals(0, offMan.getOfflineRequests(false).size());
    }
}