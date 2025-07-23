package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;

public class GoogleUserLookupParams extends OAuthUserLookupParams {

    public GoogleUserLookupParams(String userId) {
        super(userId);
    }

    @Override
    public String getProviderId() {
        return GoogleOAuth2AP.PROVIDER_ID;
    }
}
