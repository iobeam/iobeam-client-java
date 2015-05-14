package com.iobeam.api.auth;

import com.iobeam.api.resource.annotations.JsonProperty;

import java.io.Serializable;

public class TokenRefresh implements Serializable {

    private final String token;

    public TokenRefresh(String oldToken) {
        this.token = oldToken;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return token;
    }

    @Override
    public String toString() {
        return "TokenRefresh{" +
               "refresh_token='" + token + "'" +
               "}";
    }
}
