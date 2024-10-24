package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
* This code is a part of an OpenID Connect solutions using Jakarta security annotations.
* The main building blocks are:
* - @OpenIdAuthenticationDefinition added on the authentication HttpServlet edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OpenIDAuthentication, see https://docs.payara.fish/enterprise/docs/Technical%20Documentation/Public%20API/OpenID%20Connect%20Support.html
* - IdentityStoreHandler and HttpAuthenticationMechanism, as provided on the server (no custom implementation involved here), see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-auth
* - SecurityContext injected in AbstractAPIBean to handle authentication, see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-context
*/

@Named("openIdConfigBean")
public class OpenIDConfigBean implements java.io.Serializable {
    @Inject
    HttpServletRequest request;

    @Inject
    AuthenticationServiceBean authenticationSvc;

    public String getProviderURI() {
        final String oidcp = request.getParameter("oidcp");
        if (oidcp == null || oidcp.isBlank()) {
            return JvmSettings.OIDC_AUTH_SERVER_URL.lookupOptional().orElse("");
        }
        try {
            return ((OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(oidcp)).getIssuerEndpointURL();
        } catch (Exception e) {
            return "";
        }
    }

    public String getClientId() {
        final String oidcp = request.getParameter("oidcp");
        if (oidcp == null || oidcp.isBlank()) {
            return JvmSettings.OIDC_CLIENT_ID.lookupOptional().orElse("");
        }
        try {
            return ((OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(oidcp)).getClientId();
        } catch (Exception e) {
            return "";
        }
    }

    public String getClientSecret() {
        final String oidcp = request.getParameter("oidcp");
        if (oidcp == null || oidcp.isBlank()) {
            return JvmSettings.OIDC_CLIENT_SECRET.lookupOptional().orElse("");
        }
        try {
            return ((OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(oidcp)).getClientSecret();
        } catch (Exception e) {
            return "";
        }
    }

    public String getRedirectURI() {
        String target = request.getParameter("target");
        target = target == null || target.isBlank() ? "API" : target;
        return SystemConfig.getDataverseSiteUrlStatic() + "/oidc/callback/" + target;
    }

    public String getLogoutURI() {
        final String target = request.getParameter("target");
        final String baseURL = SystemConfig.getDataverseSiteUrlStatic();
        return "SPA".equals(target) ? baseURL + "/spa/" : baseURL;
    }
}
