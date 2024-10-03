package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

@Stateless
@Named("openIdConfigBean")
public class OpenIDConfigBean implements java.io.Serializable {
    public String getProviderURI() {
        return JvmSettings.API_OIDC_PROVIDER_URI.lookup();
    }

    public String getClientId() {
        return JvmSettings.API_OIDC_CLIENT_ID.lookup();
    }

    public String getClientSecret() {
        return JvmSettings.API_OIDC_CLIENT_SECRET.lookup();
    }

    public String getRedirectURI() {
        return JvmSettings.API_OIDC_REDIRECT_URI.lookup();
    }
}
