package com.iobeam.api.service;

import com.iobeam.api.auth.ProjectBearerAuthToken;
import com.iobeam.api.auth.TokenRefresh;
import com.iobeam.api.auth.UserBearerAuthToken;
import com.iobeam.api.client.RestClient;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.http.ContentType;
import com.iobeam.api.http.RequestMethod;
import com.iobeam.api.http.StatusCode;

import java.util.logging.Logger;

/**
 * Tokens service.
 */
public class Tokens {

    private final static Logger logger = Logger.getLogger(Tokens.class.getName());
    private final RestClient client;

    public Tokens(final RestClient client) {
        this.client = client;
    }

    public class GetUserToken extends RestRequest<UserBearerAuthToken> {

        private static final String PATH = "/v1/tokens/user";

        public GetUserToken(String username, String password) {
            super(client, RequestMethod.GET, PATH,
                  StatusCode.OK, UserBearerAuthToken.class);
            getBuilder().addParameter("name", username);
            getBuilder().addParameter("password", password);
        }
    }

    public GetUserToken getUserToken(String username, String password) {
        return new GetUserToken(username, password);
    }


    public class GetProjectToken extends RestRequest<ProjectBearerAuthToken> {

        private static final String PATH = "/v1/tokens/project";

        public GetProjectToken(long projectId, boolean read, boolean write, boolean admin) {
            super(client, RequestMethod.GET, PATH,
                  StatusCode.OK, ProjectBearerAuthToken.class);
            getBuilder().addParameter("project_id", projectId);
            getBuilder().addParameter("read", read);
            getBuilder().addParameter("write", write);
            getBuilder().addParameter("admin", admin);
        }
    }

    public GetProjectToken getProjectToken(long projectId, boolean read, boolean write,
                                           boolean admin) {
        return new GetProjectToken(projectId, read, write, admin);
    }

    public class RefreshProjectToken extends RestRequest<ProjectBearerAuthToken> {

        private static final String PATH = "/v1/tokens/project";

        public RefreshProjectToken(TokenRefresh tokenReq) {
            super(client, RequestMethod.POST, PATH,
                  ContentType.JSON, tokenReq,
                  StatusCode.OK, ProjectBearerAuthToken.class, false);
        }
    }

    public RefreshProjectToken refreshProjectToken(String oldToken) {
        TokenRefresh tr = new TokenRefresh(oldToken);
        return new RefreshProjectToken(tr);
    }

    public RefreshProjectToken refreshProjectToken(ProjectBearerAuthToken oldToken) {
        return refreshProjectToken(oldToken.getToken());
    }
}
