package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;

public class GitHubUserLookupParams extends OAuthUserLookupParams {

    public GitHubUserLookupParams(String userId) {
        super(userId);
    }

    @Override
    public String getProviderId() {
        return GitHubOAuth2AP.PROVIDER_ID;
    }
}
