package com.iobeam.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WriteAheadLogTest {

    private static final String TEST_LOG = "__wal-unittest.log";

    @Before
    public void setUp() throws Exception {
        new File(TEST_LOG).delete();
    }

    @After
    public void tearDown() throws Exception {
        new File(TEST_LOG).delete();
    }

    @Test
    public void testWriteLog() throws Exception {

        final WriteAheadLog log = new WriteAheadLog(TEST_LOG);
        WriteAheadLog.Writer writer = log.getWriter();

        try {
            writer.put((byte) 0, 0, "hello".getBytes("UTF-8"));
            writer.put((byte) 0, 0, "world".getBytes("UTF-8"));
        } finally {
            writer.close();
        }

        WriteAheadLog.Reader reader = log.getReader();

        try {
            WriteAheadLog.Record rec = reader.readRecord();
            assertEquals("hello", new String(rec.getPayload().array(), "UTF-8"));
        } finally {
            reader.close();
        }

        writer = log.getWriter();
        reader = log.getReader();

        try {
            writer.put((byte) 0, 0, "foo".getBytes("UTF-8"));

            WriteAheadLog.Record rec = reader.readRecord();
            assertEquals("world", new String(rec.getPayload().array(), "UTF-8"));

            rec = reader.readRecord();
            assertEquals("foo", new String(rec.getPayload().array(), "UTF-8"));

            rec = reader.readRecord();
            assertNull(rec);
        } finally {
            reader.close();
        }

        new File("test.log").delete();
    }

    @Test
    public void testTruncateLog() throws Exception {

        final WriteAheadLog log = new WriteAheadLog(TEST_LOG);
        WriteAheadLog.Writer writer = log.getWriter();

        try {
            writer.put((byte) 0, 0, "hello1".getBytes("UTF-8"));
            writer.put((byte) 0, 0, "hello2".getBytes("UTF-8"));
            writer.truncate();
        } finally {
            writer.close();
        }

        WriteAheadLog.Reader reader = log.getReader();

        try {
            WriteAheadLog.Record r = reader.readRecord();
            assertNull(r);
        } finally {
            reader.close();
        }

        writer = log.getWriter();

        try {
            writer.put((byte) 0, 0, "hello3".getBytes("UTF-8"));
        } finally {
            writer.close();
        }

        reader = log.getReader();

        try {
            WriteAheadLog.Record r = reader.readRecord();
            assertNotNull(r);
            assertEquals("hello3", new String(r.getPayload().array(), "UTF-8"));
        } finally {
            reader.close();
        }
    }
}