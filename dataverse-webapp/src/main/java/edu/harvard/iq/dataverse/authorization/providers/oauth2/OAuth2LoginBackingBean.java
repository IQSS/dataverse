package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.authorization.providers.common.ExternalIdpFirstLoginPage;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.OAuth2TokenData;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static edu.harvard.iq.dataverse.util.StringUtil.toOption;
import static java.util.stream.Collectors.toList;

/**
 * Backing bean of the oauth2 login process. Used from the login and the
 * callback pages.
 *
 * @author michael
 */
@ViewScoped
@Named("OAuth2Page")
public class OAuth2LoginBackingBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginBackingBean.class);
    private static final long STATE_TIMEOUT = TimeUnit.MINUTES.toMillis(15);
    private int responseCode;
    private String responseBody;
    private Optional<String> redirectPage;
    private OAuth2Exception error;
    private ExternalIdpUserRecord oauthUser;

    @EJB
    AuthenticationServiceBean authenticationSvc;

    @EJB
    OAuth2TokenDataServiceBean oauth2Tokens;

    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession session;

    @Inject
    ExternalIdpFirstLoginPage newAccountPage;

    public String linkFor(String idpId, String redirectPage) {
        OAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(idpId);
        return idp.createAuthorizationUrl(createState(idp, toOption(redirectPage)), getCallbackUrl());
    }

    public String getCallbackUrl() {
        return systemConfig.getOAuth2CallbackUrl();
    }

    public void exchangeCodeForToken() throws IOException {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        final String code = req.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            try (BufferedReader rdr = req.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rdr.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                error = new OAuth2Exception(-1, sb.toString(), "Remote system did not return an authorization code.");
                logger.info("OAuth2Exception getting code parameter. HTTP return code: {}. Message: {} Message body: {}", error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody());
                return;
            }
        }

        final String state = req.getParameter("state");

        try {
            OAuth2AuthenticationProvider idp = parseState(state);
            if (idp == null) {
                throw new OAuth2Exception(-1, "", "Invalid 'state' parameter.");
            }
            oauthUser = idp.getUserRecord(code, state, getCallbackUrl());
            UserRecordIdentifier idtf = oauthUser.toUserRecordIdentifier();
            AuthenticatedUser dvUser = authenticationSvc.lookupUser(idtf);

            if (dvUser == null) {
                // need to create the user
                newAccountPage.setNewUser(oauthUser);
                FacesContext.getCurrentInstance().getExternalContext().redirect("/firstLogin.xhtml");

            } else {
                // login the user and redirect to HOME of intended page (if any).
                session.setUser(dvUser);

                if (!systemConfig.isReadonlyMode()) {
                    final OAuth2TokenData tokenData = oauthUser.getTokenData();
                    if (tokenData != null) {
                        tokenData.setUser(dvUser);
                        tokenData.setOauthProviderId(idp.getId());
                        oauth2Tokens.store(tokenData);
                    }
                } else {
                    logger.warn("Can't store OAuth2TokenData when readonlyMode is turned on");
                }
                String destination = redirectPage.orElse("/");
                HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
                String prettyUrl = response.encodeRedirectURL(destination);
                FacesContext.getCurrentInstance().getExternalContext().redirect(prettyUrl);
            }

        } catch (OAuth2Exception ex) {
            error = ex;
            logger.info("OAuth2Exception caught. HTTP return code: {}. Message: {}. Message body: {}", error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody());
            logger.error("", ex);
        }

    }

    private OAuth2AuthenticationProvider parseState(String state) {
        String[] topFields = state.split("~", 2);
        if (topFields.length != 2) {
            logger.info("Wrong number of fields in state string: {}", state);
            return null;
        }
        OAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(topFields[0]);
        if (idp == null) {
            logger.info("Can''t find IDP ''{}''", topFields[0]);
            return null;
        }
        String raw = StringUtil.decrypt(topFields[1], idp.getClientSecret());
        String[] stateFields = raw.split("~", -1);
        if (idp.getId().equals(stateFields[0])) {
            long timeOrigin = Long.parseLong(stateFields[1]);
            long timeDifference = System.currentTimeMillis() - timeOrigin;
            if (timeDifference > 0 && timeDifference < STATE_TIMEOUT) {
                if (stateFields.length > 3) {
                    redirectPage = Optional.ofNullable(stateFields[3]);
                }
                return idp;
            } else {
                logger.info("State timeout");
                return null;
            }
        } else {
            logger.info("Invalid id field: ''{}''", stateFields[0]);
            return null;
        }
    }

    private String createState(OAuth2AuthenticationProvider idp, Optional<String> redirectPage) {
        if (idp == null) {
            throw new IllegalArgumentException("idp cannot be null");
        }
        String base = idp.getId() + "~" + System.currentTimeMillis()
                + "~" + (int) java.lang.Math.round(java.lang.Math.random() * 1000)
                + redirectPage.map(page -> "~" + page).orElse("");

        String encrypted = StringUtil.encrypt(base, idp.getClientSecret());
        final String state = idp.getId() + "~" + encrypted;
        return state;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public ExternalIdpUserRecord getUser() {
        return oauthUser;
    }

    public OAuth2Exception getError() {
        return error;
    }

    public boolean isInError() {
        return error != null;
    }

    public List<OAuth2AuthenticationProvider> getProviders() {
        return authenticationSvc.getOAuth2Providers().stream()
                .sorted(Comparator.comparing(OAuth2AuthenticationProvider::getTitle))
                .collect(toList());
    }

    public boolean isOAuth2ProvidersDefined() {
        return !authenticationSvc.getOAuth2Providers().isEmpty();
    }
}
