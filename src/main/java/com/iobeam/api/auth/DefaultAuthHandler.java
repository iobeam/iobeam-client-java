package com.iobeam.api.auth;

import com.iobeam.api.ApiException;
import com.iobeam.api.client.RestClient;
import com.iobeam.api.resource.util.Util;
import com.iobeam.api.service.TokenService;
import com.iobeam.util.Base64;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Default AuthHandler implementation.
 */
public class DefaultAuthHandler extends AbstractAuthHandler {

    private static final String FMT_DEFAULT_PATH = "project_%d.authtoken";

    public static ProjectBearerAuthToken parseStringToProjectToken(String t) {
        if (t == null) {
            return null;
        }

        int firstDot = t.indexOf('.');
        if (firstDot < 0) {
            return null;
        }

        int secondDot = t.indexOf('.', firstDot + 1);
        String substr = t.substring(firstDot + 1, secondDot);
        byte[] decoded = Base64.decode(substr);
        if (decoded != null) {
            String s = new String(decoded);
            JSONObject temp = new JSONObject(s);
            temp.put("project_id", temp.getLong("pid"));
            temp.put("token", t);
            temp.put("expires", Util.DATE_FORMAT.format(new Date(temp.getLong("exp") * 1000)));
            try {
                return ProjectBearerAuthToken.fromJson(temp);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private RestClient client;
    private final ProjectBearerAuthToken token;

    private DefaultAuthHandler(final RestClient client,
                               final String projectToken,
                               final String storagePath) {
        super(storagePath);
        this.client = client;
        if (client != null) {
            client.setAuthenticationHandler(this);
        }
        token = parseStringToProjectToken(projectToken);
        writeToken(token);
    }

    public DefaultAuthHandler(final RestClient client, final long projectId,
                              final String projectToken, final File storageDir) {
        this(client, projectToken, storageDir == null ? null : new File(
            storageDir, String.format(FMT_DEFAULT_PATH, projectId)).getAbsolutePath());
    }

    public DefaultAuthHandler(final RestClient client, final long projectId,
                              final String projectToken) {
        this(client, projectId, projectToken, null);
    }

    public DefaultAuthHandler(final long projectId, final String projectToken) {
        this(null, projectId, projectToken);
    }

    @Override
    public AuthToken refreshToken() throws IOException, ApiException {
        if (client != null) {
            TokenService service = new TokenService(client);
            TokenService.RefreshProjectToken req = service.refreshProjectToken(token.getToken());
            return req.execute();
        }
        return null;
    }
}
