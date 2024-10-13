package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import fish.payara.security.openid.api.AccessToken;
import fish.payara.security.openid.api.JwtClaims;
import fish.payara.security.openid.api.OpenIdConstant;
import fish.payara.security.openid.api.OpenIdContext;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
* This code is a part of an OpenID Connect solutions using Jakarta security annotations.
* The main building blocks are:
* - @OpenIdAuthenticationDefinition added on the authentication HttpServlet edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OpenIDAuthentication, see https://docs.payara.fish/enterprise/docs/Technical%20Documentation/Public%20API/OpenID%20Connect%20Support.html
* - IdentityStoreHandler and HttpAuthenticationMechanism, as provided on the server (no custom implementation involved here), see https://hantsy.gitbook.io/java-ee-8-by-example/security/security-auth
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
            final JwtClaims claims = openIdContext.getAccessToken().getJwtClaims();
            final UserRecordIdentifier userRecordIdentifier = getUserRecordIdentifier();
            final String subject = userRecordIdentifier.getUserIdInRepo();
            final String providerId = userRecordIdentifier.getUserRepoId();
            AuthenticatedUser dvUser = authenticationSvc.lookupUser(userRecordIdentifier);
            if (dvUser == null) {
                if (!systemConfig.isSignupDisabledForRemoteAuthProvider(providerId)) {
                    final String firstName = claims.getStringClaim(OpenIdConstant.GIVEN_NAME).orElse("");
                    final String lastName = claims.getStringClaim(OpenIdConstant.FAMILY_NAME).orElse("");
                    final String verifiedEmailAddress = claims.getStringClaim(OpenIdConstant.EMAIL).orElse("");
                    final String emailAddress = verifiedEmailAddress == null ? "" : verifiedEmailAddress;
                    final String affiliation = claims.getStringClaim("affiliation").orElse("");
                    final String position = claims.getStringClaim("position").orElse("");
                    final OAuth2UserRecord userRecord = new OAuth2UserRecord(
                            providerId,
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
        return getUserRecordIdentifier(openIdContext.getAccessToken());
    }

    public UserRecordIdentifier getUserRecordIdentifier(final AccessToken accessToken) {
        try {
            final OIDCAuthProvider provider = getProvider(accessToken);
            final String providerId = provider.getId();
            final String subject = provider.getSubject(accessToken);
            if (subject == null) {
                return null;
            }
            return new UserRecordIdentifier(providerId, subject);
        } catch (final Exception ignore) {
            return null;
        }
    }

    private OIDCAuthProvider getProvider(AccessToken accessToken) {
        return authenticationSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class).stream()
                .map(providerId -> (OIDCAuthProvider) authenticationSvc.getAuthenticationProvider(providerId))
                .filter(provider -> provider.isIssuerOf(accessToken)).findFirst().get();
    }
}
