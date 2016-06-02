package com.iobeam.api.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.iobeam.api.resource.util.Util;

import org.json.JSONObject;
import org.junit.Test;

public class BasicTokenTests {

    private static final String TEST_DATE_STRING = "2016-08-28 17:25:31 +0000";
    private static final String TEST_DATE_STRING_8601 = "2016-08-28T17:25:31+00:00";
    private static final String TEST_DATE_STRING_8601_2 = "2016-06-02T17:51:39Z";
    private static final String TEST_DATE_STRING_WRONG = "2016-08-28X17:25:31+00:00";

    private final String PROJECT_TOKEN_OLD = "{\n" +
                                             "  \"token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6MTMzNX0=.eyJ1aWQiOjIsInBpZCI6NiwiZXhwIjoxNDcyNDA1MTMxLCJwbXMiOjd9.KhMKWinW0p_kPL2qNBTSrJjVutK1R26MkWukQSepU_0=\",\n" +
                                             "  \"expires\": \"" + TEST_DATE_STRING + "\",\n" +
                                             "  \"created\": \"" + TEST_DATE_STRING + "\",\n" +
                                             "  \"user_id\": 0,\n" +
                                             "  \"project_id\": 6,\n" +
                                             "  \"read\": true,\n" +
                                             "  \"write\": true,\n" +
                                             "  \"admin\": true\n" +
                                             "}";

    private final String PROJECT_TOKEN_NEW = "{\n" +
                                             "  \"token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6MTMzNX0=.eyJ1aWQiOjIsInBpZCI6NiwiZXhwIjoxNDcyNDA1MTMxLCJwbXMiOjd9.KhMKWinW0p_kPL2qNBTSrJjVutK1R26MkWukQSepU_0=\",\n" +
                                             "  \"expires\": \"" + TEST_DATE_STRING_8601 + "\",\n" +
                                             "  \"created\": \"" + TEST_DATE_STRING_8601 + "\",\n" +
                                             "  \"user_id\": 0,\n" +
                                             "  \"project_id\": 6,\n" +
                                             "  \"read\": true,\n" +
                                             "  \"write\": true,\n" +
                                             "  \"admin\": true\n" +
                                             "}";

    private final String PROJECT_TOKEN_NEW_2 = "{\n" +
                                             "  \"token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6MTMzNX0=.eyJ1aWQiOjIsInBpZCI6NiwiZXhwIjoxNDcyNDA1MTMxLCJwbXMiOjd9.KhMKWinW0p_kPL2qNBTSrJjVutK1R26MkWukQSepU_0=\",\n" +
                                             "  \"expires\": \"" + TEST_DATE_STRING_8601_2 + "\",\n" +
                                             "  \"created\": \"" + TEST_DATE_STRING_8601_2 + "\",\n" +
                                             "  \"user_id\": 0,\n" +
                                             "  \"project_id\": 6,\n" +
                                             "  \"read\": true,\n" +
                                             "  \"write\": true,\n" +
                                             "  \"admin\": true\n" +
                                             "}";

    @Test
    public void testProjectTokenFromJson() throws Exception {
        ProjectBearerAuthToken t =
            ProjectBearerAuthToken.fromJson(new JSONObject(PROJECT_TOKEN_NEW));
        assertNotNull(t);
        assertEquals(Util.parseToDate(TEST_DATE_STRING_8601), t.getExpires());
    }

    @Test
    public void testProjectTokenFromJson2() throws Exception {
        ProjectBearerAuthToken t =
            ProjectBearerAuthToken.fromJson(new JSONObject(PROJECT_TOKEN_NEW_2));
        assertNotNull(t);
        assertEquals(Util.parseToDate(TEST_DATE_STRING_8601_2), t.getExpires());
    }

    @Test
    public void testProjectTokenFromJsonOld() throws Exception {
        ProjectBearerAuthToken t =
            ProjectBearerAuthToken.fromJson(new JSONObject(PROJECT_TOKEN_OLD));
        assertNotNull(t);
        assertEquals(Util.parseToDate(TEST_DATE_STRING_8601), t.getExpires());

    }

    @Test(expected = java.text.ParseException.class)
    public void testProjectTokenFromJsonBad() throws Exception {
        final JSONObject json = new JSONObject(PROJECT_TOKEN_OLD);
        json.put("expires", TEST_DATE_STRING_WRONG);
        ProjectBearerAuthToken.fromJson(json);
    }
}
