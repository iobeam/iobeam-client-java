package com.iobeam.api.auth;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility functions for auth tokens.
 */
public class AuthUtils {

    public static void writeToken(final AuthToken token,
                                  final String file) throws IOException {
        ObjectOutputStream out = null;

        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(token);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static AuthToken readToken(final String file)
        throws IOException, ClassNotFoundException {

        ObjectInputStream in = null;

        try {
            in = new ObjectInputStream(new FileInputStream(file));
            return (AuthToken) in.readObject();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
