package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omnifaces.util.Faces;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.OpenIDConfigBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
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
    public String getLogInLink() {
        openIdConfigBean.setTarget("JSF");
        final String email = getVerifiedEmail(openIdContext);
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
        final String email = getVerifiedEmail(openIdContext);
        AuthenticatedUser dvUser = authenticationSvc.getAuthenticatedUserByEmail(email);
        if (dvUser == null) {
            logger.log(Level.INFO, "user not found: " + email);
            if (!systemConfig.isSignupDisabledForRemoteAuthProvider("oidc-mpconfig")) {
                logger.log(Level.INFO, "redirect to first login: " + email);
                Faces.redirect("/oauth2/firstLogin.xhtml");
            }
        } else {
            dvUser = userService.updateLastLogin(dvUser);
            session.setUser(dvUser);
            Faces.redirect("/");
        }
    }

    public static String getVerifiedEmail(final OpenIdContext oidContext) {
        if (oidContext.getAccessToken().isExpired()) {
            return null;
        }
        final Object emailVerifiedObject = oidContext.getClaimsJson().get(OpenIdConstant.EMAIL_VERIFIED);
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
            logger.log(Level.SEVERE, "email not verified: " + oidContext.getClaimsJson().get(OpenIdConstant.EMAIL));
            return null;
        }
        return oidContext.getClaims().getEmail().orElse(null);
    }
}
