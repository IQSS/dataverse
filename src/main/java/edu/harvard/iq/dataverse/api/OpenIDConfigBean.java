package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Stateless
@Named("openIdConfigBean")
public class OpenIDConfigBean implements java.io.Serializable {

    @Inject
    HttpServletRequest request;

    private String target = "API";
    
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
        return SystemConfig.getDataverseSiteUrlStatic() + "/api/v1/callback/token";
    }

    public String getTarget() {
        return this.target;
    }

    public String setTarget(String target) {
        return this.target = target;
    }
}
