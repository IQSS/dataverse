package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

abstract class OAuthUserSearcher {

    protected String userId;

    public OAuthUserSearcher(String userId) {
        this.userId = userId;
    }

    abstract public AuthenticatedUser searchAuthenticatedUser();
}
