package com.iobeam.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

// @formatter:off
/**
 *
 * Write-ahead log implementation that looks as follows:
 *
 * |------------|--------|----------|--------|----------|--------|----------|
 * | Log Header | Header | Payload  | Header | Payload  | Header | Payload  |
 * |------------|--------|----------|--------|----------|--------|----------|
 *
 *
 * The log header contains meta information, such as read and write offsets,
 * which allows a reader and writer to quickly find the current position of the
 * last written and last read records.
 *
 * A log Writer writes records consisting of a Header and Payload, where
 * the payload can be any byte blob. The Header contains information about
 * the length of the record, the type of record (defined by writer) and a
 * set of flags. A GARBAGE flag marks a record as 'read', which means the record
 * can be deleted when the log is truncated or rotated.
 *
 * A log Reader will read records, starting after the last read record, as
 * indicated in the log header. Note that the last read record offset might
 * not always be up-to-date, so a reader will scan the log for any unread records
 * starting at the offset. After a record has been read they can optionally be
 * marked as garbage, so that they are not read again.
 */
// @formatter:on

public class WriteAheadLog {

    private final static Logger logger = Logger.getLogger(WriteAheadLog.class.getName());
    private final String file;
    private final AtomicLong lastWriteRecordPos = new AtomicLong(0);
    private final AtomicLong lastReadRecordPos = new AtomicLong(0);
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public WriteAheadLog(final String file) throws IOException {
        this.file = file;
    }

    private void updatePosition(final AtomicLong atomic, final long position) {

        while (true) {
            final long currPos = atomic.get();

            if (currPos >= position) {
                break;
            }

            if (atomic.compareAndSet(currPos, position)) {
                break;
            }
        }
    }

    void setLastReadPos(final long position) {
        updatePosition(lastReadRecordPos, position);
    }

    void setLastWritePos(final long position) {
        updatePosition(lastWriteRecordPos, position);
    }

    void updateReadWritePositions(final long readPos, final long writePos) {
        setLastReadPos(readPos);
        setLastWritePos(writePos);
    }

    void updateReadWritePositions(final LogHeader header) {
        updateReadWritePositions(header.getLastReadRecordPos(),
                                 header.getLastWriteRecordPos());
    }

    public synchronized LogHeader readHeader(final FileChannel ch) throws IOException {
        final LogHeader header;

        if (ch.size() == 0) {
            header = new LogHeader();
            header.write(ch);
        } else {
            header = LogHeader.read(ch);
        }

        logger.info(file + " : " + header);

        updateReadWritePositions(header);

        return header;
    }

    public boolean isEmpty() {
        return lastWriteRecordPos.get() == 0;
    }

    public static class Writer {

        private final FileChannel ch;
        private final WriteAheadLog log;

        private Writer(final WriteAheadLog log) throws IOException {
            this.log = log;

            if (!log.rwLock.writeLock().tryLock()) {
                throw new IOException("Log already has a writer");
            }

            ch = new RandomAccessFile(log.file, "rws").getChannel();

            final LogHeader header = log.readHeader(ch);
            moveToLastWriteRecord(header);
        }

        public void truncate() throws IOException {
            ch.truncate(0);
            // Android's implementation of file channel doesn't automatically
            // reset the file channel's position when truncating. Therefore,
            // we need to explicitly do it here:
            ch.position(0);
            log.readHeader(ch);
            log.lastReadRecordPos.set(0);
            log.lastWriteRecordPos.set(0);
        }

        private Record moveToLastWriteRecord(final LogHeader header) throws IOException {

            Record lastRecord = null;

            final long lastWritePos = header.getLastWriteRecordPos();

            if (lastWritePos > ch.position()) {
                ch.position(lastWritePos);
            }

            while (true) {
                Record rec = Record.read(ch);

                if (rec == null) {
                    break;
                }
                lastRecord = rec;
            }

            if (lastRecord != null) {
                log.lastWriteRecordPos.set(lastRecord.getPosition());
            }

            return lastRecord;
        }

        public Record put(final Record rec) throws IOException {

            final long position = rec.position;

            log.lastWriteRecordPos.set(position);

            // TODO: failure handling
            rec.write(ch);

            return rec;
        }

        public Record put(final byte type,
                          final long reference,
                          final byte[] data) throws IOException {

            final Record rec = new Record(type,
                                          ch.position(),
                                          reference,
                                          data);

            //logger.info("" + rec);
            return put(rec);
        }

        public void close() throws IOException {
            final LogHeader h = new LogHeader(log.lastReadRecordPos.get(),
                                              log.lastWriteRecordPos.get());
            ch.position(0);
            h.write(ch);
            ch.close();
            log.rwLock.writeLock().unlock();
        }
    }

    public Writer getWriter() throws IOException {
        return new Writer(this);
    }

    public Reader getReader() throws IOException {
        return new Reader(this);
    }

    public static class Reader {

        private final FileChannel ch;
        private final WriteAheadLog log;

        private Reader(final WriteAheadLog log) throws IOException {
            this.log = log;
            ch = new RandomAccessFile(log.file, "rws").getChannel();
            moveToFirstUnreadRecord(log.readHeader(ch));
        }

        private Record moveToFirstUnreadRecord(final LogHeader header) throws IOException {

            log.updateReadWritePositions(header);

            // Jump to last known read position
            final long lastReadPos = header.getLastReadRecordPos();

            if (lastReadPos > ch.position()) {
                ch.position(lastReadPos);
            }

            return getFirstUnreadRecord();
        }

        public boolean canRead() throws IOException {
            return ch.size() > ch.position();
        }

        private void moveBackToStartOfRecord(final Record rec) throws IOException {
            ch.position(rec.getPosition());
        }

        public Record markRecordAsRead(final Record rec) throws IOException {
            if (rec != null) {
                rec.markRead(ch);
                log.setLastReadPos(rec.getPosition());
            }
            return rec;
        }

        private Record getFirstUnreadRecord() throws IOException {
            Record rec;

            while (true) {
                rec = Record.read(ch);

                if (rec == null) {
                    break;
                } else if (!rec.isGarbage()) {
                    moveBackToStartOfRecord(rec);
                    break;
                }
            }

            if (rec != null) {
                //logger.info("Unread record: " + rec);
                log.setLastReadPos(rec.getPosition());
            }

            return rec;
        }

        public Record readRecord() throws IOException {
            return readRecord(true);
        }

        public Record readRecord(boolean setMark) throws IOException {
            final Record rec = Record.read(ch);

            if (rec != null && setMark) {
                markRecordAsRead(rec);
            }

            //logger.info("record: " + rec);
            return rec;
        }

        public void close() throws IOException {
            final LogHeader h = new LogHeader(log.lastReadRecordPos.get(),
                                              log.lastWriteRecordPos.get());
            ch.position(0);
            h.write(ch);
            ch.close();
        }
    }

    public static class Record {

        public static final int HEADER_LEN = 26;
        private static final byte FLAG_GARBAGE = 0x1;
        private volatile byte flags;
        private final byte type;
        private final long position;
        private final long reference;
        private final ByteBuffer payload;

        Record(final byte flags,
               final byte type,
               final long position,
               final long reference,
               final int length) {
            this.flags = flags;
            this.type = type;
            this.position = position;
            this.reference = reference;
            this.payload = ByteBuffer.allocate(length);
        }

        public Record(final byte type,
                      final long position,
                      final long reference,
                      final byte[] data) {
            this.flags = 0;
            this.type = type;
            this.position = position;
            this.reference = reference;
            if (data == null) {
                this.payload = ByteBuffer.allocate(0);
            } else {
                this.payload = ByteBuffer.wrap(data);
            }
        }

        public int write(final FileChannel ch) throws IOException {
            final ByteBuffer header = ByteBuffer.allocate(HEADER_LEN);
            int len;

            header.put(flags);
            header.put(type);
            header.putLong(reference);
            header.putInt(payload.capacity());
            header.rewind();
            payload.rewind();

            // TODO check return values
            len = ch.write(header);
            len += ch.write(payload);

            return len;
        }

        public static Record read(final FileChannel ch) throws IOException {
            final ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN);

            final long position = ch.position();
            final int len = ch.read(buf);

            if (len <= 0 || position == 0) {
                return null;
            } else if (len != HEADER_LEN) {
                logger.warning("wrong record header length " + len);
                throw new IOException("Bad record size " + len);
            }

            buf.rewind();

            final Record rec = new Record(buf.get(),
                                          buf.get(),
                                          position,
                                          buf.getLong(),
                                          buf.getInt());

            // Read payload
            ch.read(rec.payload);

            return rec;
        }

        private Record writeFlags(final FileChannel ch, final byte addFlags) throws IOException {
            final ByteBuffer buf = ByteBuffer.allocate(1);
            flags = (byte) (flags | addFlags);
            buf.put(flags);
            buf.rewind();
            ch.write(buf, position);
            return this;
        }

        private Record markRead(final FileChannel ch) throws IOException {
            return writeFlags(ch, Record.FLAG_GARBAGE);
        }

        public boolean isGarbage() {
            return (flags & FLAG_GARBAGE) == 1;
        }

        public byte getFlags() {
            return flags;
        }

        public short getType() {
            return type;
        }

        public long getPosition() {
            return position;
        }

        public long getReference() {
            return reference;
        }

        public int getPayloadLength() {
            return payload.capacity();
        }

        public ByteBuffer getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return "Record{" +
                   "flags=" + flags +
                   ", type=" + type +
                   ", position=" + position +
                   ", length=" + getPayloadLength() +
                   '}';
        }
    }

    private static class LogHeader {

        public static final int LOG_VERSION = 1;
        public static final int LOG_HEADER_SIZE = 17;

        // beginning of the last record written.
        private final byte version;
        private final long lastReadRecordPos;
        private final long lastWriteRecordPos;

        public LogHeader() {
            this.version = LOG_VERSION & 0xff;
            this.lastReadRecordPos = 0;
            this.lastWriteRecordPos = 0;
        }

        public LogHeader(final long lastReadRecordPos,
                         final long lastWriteRecordPos) {
            this.version = LOG_VERSION & 0xff;
            this.lastReadRecordPos = lastReadRecordPos;
            this.lastWriteRecordPos = lastWriteRecordPos;
        }

        public int write(final FileChannel ch) throws IOException {
            final ByteBuffer buf = ByteBuffer.allocate(LOG_HEADER_SIZE);
            buf.put((byte) (version & 0xff));
            buf.putLong(lastReadRecordPos);
            buf.putLong(lastWriteRecordPos);
            buf.rewind();

            // Always write at end of file
            return ch.write(buf);
        }

        public static LogHeader read(final FileChannel ch) throws IOException {
            final ByteBuffer buf = ByteBuffer.allocate(LOG_HEADER_SIZE);

            final int len = ch.read(buf);

            if (len == -1) {
                return null;
            } else if (len != LOG_HEADER_SIZE) {
                throw new IOException("Log header is wrong size");
            }

            buf.rewind();

            final int version = buf.get();

            if (version != LOG_VERSION) {
                throw new IOException("Mismatching log version " + version);
            }

            return new LogHeader(buf.getLong(),
                                 buf.getLong());
        }

        public int getVersion() {
            return version;
        }

        public long getLastReadRecordPos() {
            return lastReadRecordPos;
        }

        public long getLastWriteRecordPos() {
            return lastWriteRecordPos;
        }

        @Override
        public String toString() {
            return "LogHeader{" +
                   "version=" + version +
                   ", lastReadRecordPos=" + lastReadRecordPos +
                   ", lastWriteRecordPos=" + lastWriteRecordPos +
                   '}';
        }
    }
}
