package com.iobeam.api.auth;

import com.iobeam.api.resource.util.Util;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * A bearer-style token for IOBeam projects.
 */
public class ProjectBearerAuthToken extends AuthToken {

    private final long projectId;
    private final long expires;

    public ProjectBearerAuthToken(final long projectId,
                                  final String token,
                                  final Date expires) {
        super(token);
        this.projectId = projectId;
        this.expires = expires.getTime();
    }

    @Override
    public String getType() {
        return "Bearer";
    }

    @Override
    public boolean isValid() {
        return !hasExpired();
    }

    public long getProjectId() {
        return projectId;
    }

    public Date getExpires() {
        return new Date(expires);
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expires;
    }

    public static ProjectBearerAuthToken fromJson(final JSONObject json) throws ParseException {
        Date expires = Util.DATE_FORMAT.parse(json.getString("expires"));
        return new ProjectBearerAuthToken(json.getLong("project_id"),
                                   json.getString("token"),
                                   expires);
    }

    @Override
    public String toString() {
        return "ProjectBearerAuthToken{" +
               "projectId=" + projectId +
               ", expires=" + expires +
               '}';
    }
}
