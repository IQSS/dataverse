package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

@Stateless
@Named("openIdConfigBean")
public class OpenIDConfigBean implements java.io.Serializable {
    private String target = "API";
    private String providerURI = JvmSettings.OIDC_AUTH_SERVER_URL.lookupOptional().orElse(null);
    private String clientId = JvmSettings.OIDC_CLIENT_ID.lookupOptional().orElse(null);
    private String clientSecret = JvmSettings.OIDC_CLIENT_SECRET.lookupOptional().orElse(null);

    public String getProviderURI() {
        return providerURI;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectURI() {
        return SystemConfig.getDataverseSiteUrlStatic() + "/api/v1/callback/token";
    }

    public String getTarget() {
        return this.target;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    public void setProviderURI(final String providerURI) {
        this.providerURI = providerURI;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
