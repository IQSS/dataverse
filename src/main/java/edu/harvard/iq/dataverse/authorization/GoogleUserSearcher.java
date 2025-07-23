package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

public class GoogleUserSearcher extends OAuthUserSearcher {

    public GoogleUserSearcher(String userId) {
        super(userId);
    }

    @Override
    public AuthenticatedUser searchAuthenticatedUser() {
        return null;
    }
}
