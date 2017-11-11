package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static edu.harvard.iq.dataverse.util.StringUtil.toOption;
import edu.harvard.iq.dataverse.util.SystemConfig;

/**
 * Backing bean of the oauth2 login process. Used from the login and the
 * callback pages.
 *
 * @author michael
 */
@Named(value = "OAuth2Page")
@ViewScoped
public class OAuth2LoginBackingBean implements Serializable {

    private static final Logger logger = Logger.getLogger(OAuth2LoginBackingBean.class.getName());
    private static final long STATE_TIMEOUT = 1000 * 60 * 15; // 15 minutes in msec
    private int responseCode;
    private String responseBody;
    private Optional<String> redirectPage;
    private OAuth2Exception error;
    private OAuth2UserRecord oauthUser;

    @EJB
    AuthenticationServiceBean authenticationSvc;
    
    @EJB
    OAuth2TokenDataServiceBean oauth2Tokens;

    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession session;

    @Inject
    OAuth2FirstLoginPage newAccountPage;

    public String linkFor(String idpId, String redirectPage) {
        AbstractOAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(idpId);
        return idp.getService(createState(idp, toOption(redirectPage) ), getCallbackUrl()).getAuthorizationUrl();
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
                logger.log(Level.INFO, "OAuth2Exception getting code parameter. HTTP return code: {0}. Message: {1} Message body: {2}", new Object[]{error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody()});
                return;
            }
        }

        final String state = req.getParameter("state");

        try {
            AbstractOAuth2AuthenticationProvider idp = parseState(state);
            if (idp == null) {
                throw new OAuth2Exception(-1, "", "Invalid 'state' parameter.");
            }
            oauthUser = idp.getUserRecord(code, state, getCallbackUrl());
            UserRecordIdentifier idtf = oauthUser.getUserRecordIdentifier();
            AuthenticatedUser dvUser = authenticationSvc.lookupUser(idtf);
            
            if (dvUser == null) {
                // need to create the user
                newAccountPage.setNewUser(oauthUser);
                FacesContext.getCurrentInstance().getExternalContext().redirect("/oauth2/firstLogin.xhtml");

            } else {
                // login the user and redirect to HOME of intended page (if any).
                session.setUser(dvUser);
                final OAuth2TokenData tokenData = oauthUser.getTokenData();
                tokenData.setUser(dvUser);
                tokenData.setOauthProviderId(idp.getId());
                oauth2Tokens.store(tokenData);
                String destination = redirectPage.orElse("/");
                HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
                String prettyUrl = response.encodeRedirectURL(destination);
                FacesContext.getCurrentInstance().getExternalContext().redirect(prettyUrl);
            }

        } catch (OAuth2Exception ex) {
            error = ex;
            logger.log(Level.INFO, "OAuth2Exception caught. HTTP return code: {0}. Message: {1}. Message body: {2}", new Object[]{error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody()});
            Logger.getLogger(OAuth2LoginBackingBean.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private AbstractOAuth2AuthenticationProvider parseState(String state) {
        String[] topFields = state.split("~", 2);
        if (topFields.length != 2) {
            logger.log(Level.INFO, "Wrong number of fields in state string", state);
            return null;
        }
        AbstractOAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(topFields[0]);
        if (idp == null) {
            logger.log(Level.INFO, "Can''t find IDP ''{0}''", topFields[0]);
            return null;
        }
        String raw = StringUtil.decrypt(topFields[1], idp.clientSecret);
        String[] stateFields = raw.split("~", -1);
        if (idp.getId().equals(stateFields[0])) {
            long timeOrigin = Long.parseLong(stateFields[1]);
            long timeDifference = System.currentTimeMillis() - timeOrigin;
            if (timeDifference > 0 && timeDifference < STATE_TIMEOUT) {
                if ( stateFields.length > 3) {
                    redirectPage = Optional.ofNullable(stateFields[3]);
                }
                return idp;
            } else {
                logger.info("State timeout");
                return null;
            }
        } else {
            logger.log(Level.INFO, "Invalid id field: ''{0}''", stateFields[0]);
            return null;
        }
    }

    private String createState(AbstractOAuth2AuthenticationProvider idp, Optional<String> redirectPage ) {
        if (idp == null) {
            throw new IllegalArgumentException("idp cannot be null");
        }
        String base = idp.getId() + "~" + System.currentTimeMillis() 
                                  + "~" + (int) java.lang.Math.round(java.lang.Math.random() * 1000)
                                  + redirectPage.map( page -> "~"+page).orElse("");

        String encrypted = StringUtil.encrypt(base, idp.clientSecret);
        final String state = idp.getId() + "~" + encrypted;
        return state;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public OAuth2UserRecord getUser() {
        return oauthUser;
    }

    public OAuth2Exception getError() {
        return error;
    }

    public boolean isInError() {
        return error != null;
    }

    public List<AbstractOAuth2AuthenticationProvider> getProviders() {
        return authenticationSvc.getOAuth2Providers().stream()
                .sorted(Comparator.comparing(AbstractOAuth2AuthenticationProvider::getTitle))
                .collect(toList());
    }

    public boolean isOAuth2ProvidersDefined() {
        return !authenticationSvc.getOAuth2Providers().isEmpty();
    }
}
