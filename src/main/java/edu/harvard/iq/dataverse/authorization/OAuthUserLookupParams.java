package edu.harvard.iq.dataverse.authorization;

abstract class OAuthUserLookupParams {

    protected String userId;

    public OAuthUserLookupParams(String userId) {
        this.userId = userId;
    }

    public String getAuthenticatedUserId() {
        return userId;
    }

    public abstract String getProviderId();
}
