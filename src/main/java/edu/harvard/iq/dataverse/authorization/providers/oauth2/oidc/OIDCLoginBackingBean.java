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
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenData;
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
 * Backing bean of the OIDC login process. Used from the login and the
 * callback pages.
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
        return "/oidc/login?target=JSF&oidcp="+oidcAuthProvider.getId();
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
                            OAuth2TokenData.from(openIdContext),
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
            if (openIdContext.getAccessToken().isExpired()) {
                return null;
            }
            final Object emailVerifiedObject = openIdContext.getClaimsJson().get(OpenIdConstant.EMAIL_VERIFIED);
            final boolean emailVerified;
            if (emailVerifiedObject instanceof JsonValue) {
                final JsonValue v = (JsonValue) emailVerifiedObject;
                emailVerified = JsonValue.TRUE.equals(v)
                        || (JsonValue.ValueType.STRING.equals(v.getValueType())
                                && Boolean.getBoolean(((JsonString) v).getString()));
            } else {
                emailVerified = false;
            }
            if (!emailVerified) {
                logger.log(Level.SEVERE,
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
