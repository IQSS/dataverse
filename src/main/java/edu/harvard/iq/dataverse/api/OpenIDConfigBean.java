package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

@Stateless
@Named("openIdConfigBean")
public class OpenIDConfigBean implements java.io.Serializable {
    public String getProviderURI() {
        return JvmSettings.OIDC_AUTH_SERVER_URL.lookupOptional().orElse(null);
    }

    public String getClientId() {
        return JvmSettings.OIDC_CLIENT_ID.lookupOptional().orElse(null);
    }

    public String getClientSecret() {
        return JvmSettings.OIDC_CLIENT_SECRET.lookupOptional().orElse(null);
    }

    public String getRedirectURI() {
        return JvmSettings.OIDC_REDIRECT_URI.lookupOptional().orElse(null);
    }
}
