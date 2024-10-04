package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.omnifaces.util.Faces;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.OpenIDConfigBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.SystemConfig;
import fish.payara.security.openid.api.OpenIdConstant;
import fish.payara.security.openid.api.OpenIdContext;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Backing bean of the OIDC login process. Used from the login and the
 * callback pages.
 */
@Stateless
@Named
public class OIDCLoginBackingBean implements Serializable {

    private static final Logger logger = Logger.getLogger(OAuth2LoginBackingBean.class.getName());

    @EJB
    AuthenticationServiceBean authenticationSvc;

    @EJB
    SystemConfig systemConfig;

    @EJB
    UserServiceBean userService;

    @Inject
    DataverseSession session;

    @Inject
    OAuth2FirstLoginPage newAccountPage;

    @Inject
    OpenIdContext openIdContext;

    @EJB
    OpenIDConfigBean openIdConfigBean;

    /**
     * Generate the OIDC log in link.
     */
    public String getLogInLink(final OIDCAuthProvider oidcAuthProvider) {
        oidcAuthProvider.setConfig(openIdConfigBean);
        final String email = getVerifiedEmail();
        if (email != null) {
            setUser();
            return SystemConfig.getDataverseSiteUrlStatic();
        }
        return SystemConfig.getDataverseSiteUrlStatic() + "/oidc/login";
    }

    /**
     * View action for callback.xhtml, the browser redirect target for the OAuth2
     * provider.
     * 
     * @throws IOException
     */
    public void setUser() {
        final String email = getVerifiedEmail();
        AuthenticatedUser dvUser = authenticationSvc.getAuthenticatedUserByEmail(email);
        if (dvUser == null) {
            logger.log(Level.INFO, "user not found: " + email);
            if (!systemConfig.isSignupDisabledForRemoteAuthProvider("oidc-mpconfig")) {
                logger.log(Level.INFO, "redirect to first login: " + email);
                /*
                 * final OAuth2UserRecord userRecord = OAuth2UserRecord("oidc",
                 * parsed.userIdInProvider,
                 * openIdContext.getSubject(),
                 * OAuth2TokenData.from(openIdContext.getAccessToken()),
                 * parsed.displayInfo,
                 * parsed.emails);
                 * newAccountPage.setNewUser(userRecord);
                 */
                Faces.redirect("/oauth2/firstLogin.xhtml");
            }
        } else {
            dvUser = userService.updateLastLogin(dvUser);
            session.setUser(dvUser);
            storeBearerToken();
            Faces.redirect("/");
        }
    }

    public String getVerifiedEmail() {
        if (openIdContext.getAccessToken().isExpired()) {
            return null;
        }
        final Object emailVerifiedObject = openIdContext.getClaimsJson().get(OpenIdConstant.EMAIL_VERIFIED);
        final boolean emailVerified;
        if (emailVerifiedObject instanceof JsonValue) {
            final JsonValue v = (JsonValue) emailVerifiedObject;
            emailVerified = JsonValue.TRUE.equals(emailVerifiedObject)
                    || (JsonValue.ValueType.STRING.equals(v.getValueType())
                            && Boolean.getBoolean(((JsonString) v).getString()));
        } else {
            emailVerified = false;
        }
        if (!emailVerified) {
            logger.log(Level.SEVERE, "email not verified: " + openIdContext.getClaimsJson().get(OpenIdConstant.EMAIL));
            return null;
        }
        return openIdContext.getClaims().getEmail().orElse(null);
    }

    public void storeBearerToken() {
        if (!FeatureFlags.API_BEARER_AUTH.enabled()) {
            return;
        }
        final String email = getVerifiedEmail();
        if (email == null) {
            logger.log(Level.WARNING, "Could not store bearer token, verified email not found");
        }
        final String issuerEndpointURL = openIdContext.getClaims().getStringClaim(OpenIdConstant.ISSUER_IDENTIFIER)
                .orElse(null);
        List<OIDCAuthProvider> providers = authenticationSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)
                .stream()
                .map(providerId -> (OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(providerId))
                .filter(provider -> issuerEndpointURL.equals(provider.getIssuerEndpointURL()))
                .collect(Collectors.toUnmodifiableList());
        // If not OIDC Provider are configured we cannot validate a Token
        if (providers.isEmpty()) {
            logger.log(Level.WARNING, "OIDC provider not found for URL: " + issuerEndpointURL);
        } else {
            final String token = openIdContext.getAccessToken().getToken();
            providers.get(0).storeBearerToken(token, email);
        }
    }
}
