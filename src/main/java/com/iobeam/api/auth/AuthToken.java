package com.iobeam.api.auth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * AuthToken used to authenticate with IOBeam API servers.
 */
public abstract class AuthToken implements Serializable {

    private static final long serialVersionUID = 2L;
    private final String token;

    public AuthToken(final String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public abstract String getType();

    public boolean isValid() {
        return true;
    }

    private void readObject(final ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    @Override
    public String toString() {
        return "AuthToken{" +
               "token='" + token + '\'' +
               "type='" + getType() + '\'' +
               '}';
    }
}
