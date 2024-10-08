package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.omnifaces.util.Faces;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2FirstLoginPage;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.util.SystemConfig;
import fish.payara.security.openid.api.JwtClaims;
import fish.payara.security.openid.api.OpenIdConstant;
import fish.payara.security.openid.api.OpenIdContext;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
* This code is a part of an OpenID Connect solutions using Jakarta security annotations.
* The main building blocks are:
* - @OpenIdAuthenticationDefinition added on the authentication HttpServlet edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OpenIDAuthentication, see https://docs.payara.fish/enterprise/docs/Technical%20Documentation/Public%20API/OpenID%20Connect%20Support.html
* - IdentityStoreHandler and HttpAuthenticationMechanism, as provided on the server (no custom implementation involved here), see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-auth
* - IdentityStore implemented for Bearer tokens in edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.BearerTokenMechanism, see also https://docs.payara.fish/enterprise/docs/Technical%20Documentation/Public%20API/OpenID%20Connect%20Support.html and https://hantsy.gitbook.io/java-ee-8-by-example/security/security-store
* - SecurityContext injected in AbstractAPIBean to handle authentication, see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-context
*/

/**
 * Backing bean of the OIDC login process. Used from the login and the callback pages.
 * It also provides UserRecordIdentifier retrieval method used in the AbstractAPIBean for OpenIdContext processing to identify the connected user.
 */
@Stateless
@Named
public class OIDCLoginBackingBean implements Serializable {
    private static final Logger logger = Logger.getLogger(OIDCLoginBackingBean.class.getName());

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

    /**
     * Generate the OIDC log in link.
     */
    public String getLogInLink(final OIDCAuthProvider oidcAuthProvider) {
        final UserRecordIdentifier userRecordIdentifier = getUserRecordIdentifier();
        if (userRecordIdentifier != null) {
            setUser();
            return SystemConfig.getDataverseSiteUrlStatic();
        }
        return "/oidc/login?target=JSF&oidcp=" + oidcAuthProvider.getId();
    }

    /**
     * View action for callback.xhtml, the browser redirect target for the OAuth2
     * provider.
     * 
     * @throws IOException
     */
    public void setUser() {
        try {
            final String subject = openIdContext.getSubject();
            final OIDCAuthProvider provider = getProvider();
            final UserRecordIdentifier userRecordIdentifier = new UserRecordIdentifier(provider.getId(), subject);
            AuthenticatedUser dvUser = authenticationSvc.lookupUser(userRecordIdentifier);
            if (dvUser == null) {
                if (!systemConfig.isSignupDisabledForRemoteAuthProvider(provider.getId())) {
                    final JwtClaims claims = openIdContext.getAccessToken().getJwtClaims();
                    final String firstName = claims.getStringClaim(OpenIdConstant.GIVEN_NAME).orElse("");
                    final String lastName = claims.getStringClaim(OpenIdConstant.FAMILY_NAME).orElse("");
                    final String verifiedEmailAddress = getVerifiedEmail();
                    final String emailAddress = verifiedEmailAddress == null ? "" : verifiedEmailAddress;
                    final String affiliation = claims.getStringClaim("affiliation").orElse("");
                    final String position = claims.getStringClaim("position").orElse("");
                    final OAuth2UserRecord userRecord = new OAuth2UserRecord(
                            provider.getId(),
                            subject,
                            claims.getStringClaim(OpenIdConstant.PREFERRED_USERNAME).orElse(subject),
                            null,
                            new AuthenticatedUserDisplayInfo(firstName, lastName, emailAddress, affiliation, position),
                            List.of(emailAddress));
                    logger.log(Level.INFO, "redirect to first login: " + userRecordIdentifier);
                    newAccountPage.setNewUser(userRecord);
                    Faces.redirect("/oauth2/firstLogin.xhtml");
                }
            } else {
                dvUser = userService.updateLastLogin(dvUser);
                session.setUser(dvUser);
                Faces.redirect("/");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Setting user failed: " + e.getMessage());
        }
    }

    public UserRecordIdentifier getUserRecordIdentifier() {
        try {
            final String subject = openIdContext.getSubject();
            final OIDCAuthProvider provider = getProvider();
            return new UserRecordIdentifier(provider.getId(), subject);
        } catch (final Exception ignore) {
            return null;
        }
    }

    private String getVerifiedEmail() {
        try {
            final Object emailVerifiedObject = openIdContext.getClaimsJson().get(OpenIdConstant.EMAIL_VERIFIED);
            final boolean emailVerified;
            if (emailVerifiedObject instanceof JsonValue v) {
                emailVerified = JsonValue.TRUE.equals(v)
                        || (JsonValue.ValueType.STRING.equals(v.getValueType())
                                && Boolean.getBoolean(((JsonString) v).getString()));
            } else {
                emailVerified = false;
            }
            if (!emailVerified) {
                logger.log(Level.FINE,
                        "email not verified: " + openIdContext.getClaimsJson().get(OpenIdConstant.EMAIL));
                return null;
            }
            return openIdContext.getClaims().getEmail().orElse(null);
        } catch (final Exception ignore) {
            return null;
        }
    }

    private OIDCAuthProvider getProvider() {
        final String issuerEndpointURL = openIdContext.getAccessToken().getJwtClaims()
                .getStringClaim(OpenIdConstant.ISSUER_IDENTIFIER)
                .orElse(null);
        if (issuerEndpointURL == null) {
            logger.log(Level.SEVERE,
                    "Issuer URL (iss) not found in " + openIdContext.getAccessToken().getJwtClaims().toString());
            return null;
        }
        // Are we sure these values are equal? Does the issuer URL have to be the full qualified one or could it be just the "top" URL from where you can access the .well-known endpoint?
        // - No, not sure. This might cause problems in the future.
        List<OIDCAuthProvider> providers = authenticationSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)
                .stream()
                .map(providerId -> (OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(providerId))
                .filter(provider -> issuerEndpointURL.equals(provider.getIssuerEndpointURL()))
                .collect(Collectors.toUnmodifiableList());
        if (providers.isEmpty()) {
            logger.log(Level.SEVERE, "OIDC provider not found for URL: " + issuerEndpointURL);
            return null;
        } else {
            return providers.get(0);
        }
    }
}
