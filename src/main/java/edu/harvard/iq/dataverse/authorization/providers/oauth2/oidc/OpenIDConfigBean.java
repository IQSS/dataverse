package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("openIdConfigBean")
public class OpenIDConfigBean implements java.io.Serializable {
    @Inject
    HttpServletRequest request;

    @Inject
    AuthenticationServiceBean authenticationSvc;

    public String getProviderURI() {
        final String oidcp = request.getParameter("oidcp");
        if (oidcp == null || oidcp == "") {
            return JvmSettings.OIDC_AUTH_SERVER_URL.lookupOptional().orElse(null);
        }
        try {
            return ((OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(oidcp)).getIssuerEndpointURL();
        } catch (Exception e) {
            return "";
        }
    }

    public String getClientId() {
        final String oidcp = request.getParameter("oidcp");
        if (oidcp == null || oidcp == "") {
            return JvmSettings.OIDC_CLIENT_ID.lookupOptional().orElse(null);
        }
        try {
            return ((OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(oidcp)).getClientId();
        } catch (Exception e) {
            return "";
        }
    }

    public String getClientSecret() {
        final String oidcp = request.getParameter("oidcp");
        if (oidcp == null || oidcp == "") {
            return JvmSettings.OIDC_CLIENT_SECRET.lookupOptional().orElse(null);
        }
        try {
            return ((OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(oidcp)).getClientSecret();
        } catch (Exception e) {
            return "";
        }
    }

    public String getRedirectURI() {
        String target = request.getParameter("target");
        target = target == null || target == "" ? "API" : target;
        return SystemConfig.getDataverseSiteUrlStatic() + "/oidc/callback/" + target;
    }

    public String getLogoutURI() {
        final String target = request.getParameter("target");
        final String baseURL = SystemConfig.getDataverseSiteUrlStatic();
        return "SPA".equals(target) ? baseURL + "/spa/" : baseURL;
    }
}
