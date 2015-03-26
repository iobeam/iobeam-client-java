package com.iobeam.api.client;

import com.iobeam.api.http.RequestBuilder;
import com.iobeam.api.http.StatusCode;
import com.iobeam.util.WriteAheadLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Offline manager allows the logging of HTTP requests and responses.
 *
 * Only request will be logged during offline periods since there will be no responses due to lack
 * of network connectivity. The requests that have received no response can later be retrieved via
 * getOfflineRequests().
 */
public class OfflineManager {

    private final String logfile;
    private final WriteAheadLog log;
    private final WriteAheadLog.Writer logWriter;

    public OfflineManager(final String logfile) throws IOException {
        this.logfile = logfile;
        this.log = new WriteAheadLog(logfile);
        this.logWriter = log.getWriter();
    }

    public void truncateLog() throws IOException {
        logWriter.truncate();
    }

    public void destroy() throws IOException {
        logWriter.close();
    }

    public boolean deleteLog() {
        try {
            logWriter.close();
        } catch (IOException e) {
            // ignore
        }

        return new File(logfile).delete();
    }

    public WriteAheadLog getLog() {
        return log;
    }

    // A serializable class that wraps an HTTP request as a payload in the log record.
    public static class RequestRecord implements Serializable {

        private final RequestBuilder builder;
        private final Class<?> responseClass;
        private final StatusCode expectedCode;
        private transient long logPosition = 0;

        public RequestRecord(final RequestBuilder builder,
                             final Class<?> responseClass,
                             final StatusCode expectedCode) {
            this.builder = builder;
            this.responseClass = responseClass;
            this.expectedCode = expectedCode;
        }

        public long getLogPosition() {
            return logPosition;
        }

        private void setLogPosition(long logPosition) {
            this.logPosition = logPosition;
        }

        public RequestBuilder getBuilder() {
            return builder;
        }

        public Class<?> getResponseClass() {
            return responseClass;
        }

        public StatusCode getExpectedCode() {
            return expectedCode;
        }

        @Override
        public String toString() {
            return "RequestRecord{" +
                   "builder=" + builder +
                   ", responseClass=" + responseClass +
                   ", expectedCode=" + expectedCode +
                   '}';
        }

        public static RequestRecord fromLogRecord(final byte[] record) throws IOException {
            final ByteArrayInputStream arrIn = new ByteArrayInputStream(record);
            ObjectInputStream in = null;

            try {
                in = new ObjectInputStream(arrIn);
                return (RequestRecord) in.readObject();
            } catch (ClassNotFoundException e) {
                return null;
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    // Types of records we write, either a request or a response.
    public static final byte REQ_TYPE = 1;
    public static final byte RSP_TYPE = 2;

    public long logRequest(final RequestRecord rr) throws IOException {
        final ByteArrayOutputStream arrOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;

        try {
            out = new ObjectOutputStream(arrOut);
            out.writeObject(rr);
            final WriteAheadLog.Record rec = logWriter.put(REQ_TYPE, -1, arrOut.toByteArray());
            return rec.getPosition();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public long logRequest(final RequestBuilder builder,
                           final Class<?> responseClass,
                           final StatusCode expectedCode) throws IOException {
        return logRequest(new RequestRecord(builder, responseClass, expectedCode));
    }

    /**
     * Log a HTTP response.
     *
     * This function will log only a reference to a earlier HTTP request. It won't log any actual
     * response data.
     *
     * @param reference the ID (position in the log) of an earlier HTTP request
     * @return The ID (position) of the log entry.
     * @throws IOException on logging error.
     */
    public long logResponse(final long reference) throws IOException {
        final WriteAheadLog.Record rec = logWriter.put(RSP_TYPE, reference, null);
        return rec.getPosition();
    }

    public void setMark(final long position) {
        WriteAheadLog.Reader reader = null;

        try {
            reader = log.getReader();

            while (true) {

                final WriteAheadLog.Record rec = reader.readRecord(false);

                if (rec != null) {
                    if (rec.getPosition() > position) {
                        break;
                    }
                    reader.markRecordAsRead(rec);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Nothing we can do here really
                }
            }
        }

    }

    /**
     * Return a collection of all requests that have not received a response since the last time
     * this function was called.
     *
     * @return all unread requests with no responses.
     */
    public Collection<RequestRecord> getOfflineRequests(final boolean updateReadMark) {

        // Use LinkedHashMap for insertion-order iteration
        final Map<Long, RequestRecord> requests = new LinkedHashMap<Long, RequestRecord>();
        WriteAheadLog.Reader reader = null;

        try {
            reader = log.getReader();

            while (true) {

                final WriteAheadLog.Record rec = reader.readRecord(updateReadMark);

                if (rec != null) {
                    if (rec.getType() == REQ_TYPE) {
                        final RequestRecord rr =
                            RequestRecord.fromLogRecord(rec.getPayload().array());
                        rr.setLogPosition(rec.getPosition());
                        requests.put(rec.getPosition(), rr);
                    } else {
                        requests.remove(rec.getReference());
                    }
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Nothing we can do here really
                }
            }
        }
        return requests.values();
    }

    public Collection<RequestRecord> getOfflineRequests() {
        return getOfflineRequests(true);
    }
}
