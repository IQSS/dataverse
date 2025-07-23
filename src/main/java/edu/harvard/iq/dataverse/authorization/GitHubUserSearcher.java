package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

public class GitHubUserSearcher extends OAuthUserSearcher {

    public GitHubUserSearcher(String userId) {
        super(userId);
    }

    @Override
    public AuthenticatedUser searchAuthenticatedUser() {
        return null;
    }
}
