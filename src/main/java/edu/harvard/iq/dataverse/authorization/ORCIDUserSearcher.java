package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

public class ORCIDUserSearcher extends OAuthUserSearcher {

    public ORCIDUserSearcher(String userId) {
        super(userId);
    }

    @Override
    public AuthenticatedUser searchAuthenticatedUser() {
        return null;
    }
}
