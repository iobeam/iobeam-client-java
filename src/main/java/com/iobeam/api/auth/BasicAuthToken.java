package com.iobeam.api.auth;

import com.iobeam.util.Base64;

import java.io.UnsupportedEncodingException;

/**
 * A token for basic authentication.
 */
public class BasicAuthToken extends AuthToken {

    public BasicAuthToken(final String user,
                          final String password) {
        super(encode(user, password));
    }

    private static String encode(final String username,
                                 final String password) {
        final String combo = username + ":" + password;
        try {
            return Base64.encodeToString(combo.getBytes("UTF-8"), true);
        } catch (UnsupportedEncodingException e) {
            // Ignore. Shouldn't happen.
            return null;
        }
    }

    @Override
    public String getType() {
        return "Basic";
    }
}
