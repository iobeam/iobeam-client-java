package com.iobeam.api.auth;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class AuthHandlerTest {

    @Test
    public void testParseProjectString() throws Exception {
        String pt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6N30.eyJ1aWQiOjAsInBpZCI6MCwiZXhwIj"
                    + "oxNDMyMTU5ODM4LCJwbXMiOjN9.v8KUYwN79EE9tFlcXYdQEOhZraPxrVUwk69Y1DvZuO0";
        ProjectBearerAuthToken token = DefaultAuthHandler.parseStringToProjectToken(pt);
        assertEquals(0, token.getProjectId());
        assertEquals(1432159838000l, token.getExpires().getTime());
    }
}
