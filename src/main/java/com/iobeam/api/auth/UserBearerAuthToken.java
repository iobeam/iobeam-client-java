package com.iobeam.api.auth;

import com.iobeam.api.resource.util.Util;

import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * A bearer-style token for IOBeam users.
 */
public class UserBearerAuthToken extends AuthToken {

    private final long userId;
    private final long expires;

    public UserBearerAuthToken(final long userId,
                               final String token,
                               final Date expires) {
        super(token);
        this.userId = userId;
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

    public long getUserId() {
        return userId;
    }

    public Date getExpires() {
        return new Date(expires);
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expires;
    }

    public static UserBearerAuthToken fromJson(final JSONObject json) throws ParseException {
        return new UserBearerAuthToken(json.getLong("user_id"),
                                       json.getString("token"),
                                       Util.parseToDate(json.getString("expires")));
    }

    @Override
    public String toString() {
        return "UserBearerAuthToken{" +
               "userId=" + userId +
               ", expires=" + expires +
               '}';
    }
}
