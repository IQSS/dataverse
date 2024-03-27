package edu.harvard.iq.dataverse.globus;

import java.util.ArrayList;

public class AccessToken implements java.io.Serializable {

    private String accessToken;
    private String idToken;
    private Long expiresIn;
    private String resourceServer;
    private String tokenType;
    private String state;
    private String scope;
    private String refreshToken;
    private ArrayList<AccessToken> otherTokens;

    public String getAccessToken() {
        return accessToken;
    }

    String getIdToken() {
        return idToken;
    }

    Long getExpiresIn() {
        return expiresIn;
    }

    String getResourceServer() {
        return resourceServer;
    }

    String getTokenType() {
        return tokenType;
    }

    String getState() {
        return state;
    }

    String getScope() {
        return scope;
    }

    String getRefreshToken() {
        return refreshToken;
    }

    public ArrayList<AccessToken> getOtherTokens() {
        return otherTokens;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public void setOtherTokens(ArrayList<AccessToken> otherTokens) {
        this.otherTokens = otherTokens;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setResourceServer(String resourceServer) {
        this.resourceServer = resourceServer;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
