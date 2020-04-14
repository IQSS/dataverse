package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.ClockUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import static edu.harvard.iq.dataverse.util.StringUtil.toOption;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.omnifaces.util.Faces;

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
    static final long STATE_TIMEOUT = 1000 * 60 * 15; // 15 minutes in msec
    private int responseCode;
    private String responseBody;
    Optional<String> redirectPage = Optional.empty();
    private OAuth2Exception error;
    /**
     * TODO: Only used in exchangeCodeForToken(). Make local var in method.
     */
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
    
    @Inject
    @ClockUtil.LocalTime
    Clock clock;
    
    /**
     * Generate the OAuth2 Provider URL to be used in the login page link for the provider.
     * @param idpId Unique ID for the provider (used to lookup in authn service bean)
     * @param redirectPage page part of URL where we should be redirected after login (e.g. "dataverse.xhtml")
     * @return A generated link for the OAuth2 provider login
     */
    public String linkFor(String idpId, String redirectPage) {
        AbstractOAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(idpId);
        String state = createState(idp, toOption(redirectPage));
        return idp.buildAuthzUrl(state, systemConfig.getOAuth2CallbackUrl());
    }
    
    /**
     * View action for callback.xhtml, the browser redirect target for the OAuth2 provider.
     * @throws IOException
     */
    public void exchangeCodeForToken() throws IOException {
        HttpServletRequest req = Faces.getRequest();
        
        try {
            Optional<AbstractOAuth2AuthenticationProvider> oIdp = parseStateFromRequest(req.getParameter("state"));
            Optional<String> code = parseCodeFromRequest(req);

            if (oIdp.isPresent() && code.isPresent()) {
                AbstractOAuth2AuthenticationProvider idp = oIdp.get();
                oauthUser = idp.getUserRecord(code.get(), systemConfig.getOAuth2CallbackUrl());
                
                UserRecordIdentifier idtf = oauthUser.getUserRecordIdentifier();
                AuthenticatedUser dvUser = authenticationSvc.lookupUser(idtf);
    
                if (dvUser == null) {
                    // need to create the user
                    newAccountPage.setNewUser(oauthUser);
                    Faces.redirect("/oauth2/firstLogin.xhtml");
        
                } else {
                    // login the user and redirect to HOME of intended page (if any).
                    session.setUser(dvUser);
                    session.configureSessionTimeout();
                    final OAuth2TokenData tokenData = oauthUser.getTokenData();
                    if (tokenData != null) {
                        tokenData.setUser(dvUser);
                        tokenData.setOauthProviderId(idp.getId());
                        oauth2Tokens.store(tokenData);
                    }
                    
                    Faces.redirect(redirectPage.orElse("/"));
                }
            }
        } catch (OAuth2Exception ex) {
            error = ex;
            logger.log(Level.INFO, "OAuth2Exception caught. HTTP return code: {0}. Message: {1}. Message body: {2}", new Object[]{error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody()});
            Logger.getLogger(OAuth2LoginBackingBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException | ExecutionException ex) {
            error = new OAuth2Exception(-1, "Please see server logs for more details", "Could not login due to threading exceptions.");
            logger.log(Level.WARNING, "Threading exception caught. Message: {0}", ex.getLocalizedMessage());
        }
    }
    
    /**
     * TODO: Refactor this to be included in calling method.
     * TODO: Use org.apache.commons.io.IOUtils.toString(req.getReader()) instead of overcomplicated code below.
     */
    private Optional<String> parseCodeFromRequest(@NotNull HttpServletRequest req) {
        String code = req.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            try (BufferedReader rdr = req.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rdr.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                error = new OAuth2Exception(-1, sb.toString(), "Remote system did not return an authorization code.");
                logger.log(Level.INFO, "OAuth2Exception getting code parameter. HTTP return code: {0}. Message: {1} Message body: {2}", new Object[]{error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody()});
                return Optional.empty();
            } catch (IOException e) {
                error = new OAuth2Exception(-1, "", "Could not parse OAuth2 code due to IO error.");
                logger.log(Level.WARNING, "IOException getting code parameter.", e.getLocalizedMessage());
                return Optional.empty();
            }
        }
        return Optional.of(code);
    }
    
    /**
     * Parse and verify the state returned from the provider.
     *
     * As it contains the providers implementation "id" field when send by us,
     * we can return the corresponding provider object.
     *
     * This function is not side effect free: it will (if present) set {@link #redirectPage}
     * to the value received from the state.
     *
     * @param state The state string, created in  {@link #createState(AbstractOAuth2AuthenticationProvider, Optional)}, send and returned by provider
     * @return A corresponding provider object when state verification succeeded.
     */
    Optional<AbstractOAuth2AuthenticationProvider> parseStateFromRequest(@NotNull String state) {
        if (state == null || state.trim().equals("")) {
            logger.log(Level.INFO, "No state present in request");
            return Optional.empty();
        }
        
        String[] topFields = state.split("~", 2);
        if (topFields.length != 2) {
            logger.log(Level.INFO, "Wrong number of fields in state string", state);
            return Optional.empty();
        }
        AbstractOAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(topFields[0]);
        if (idp == null) {
            logger.log(Level.INFO, "Can''t find IDP ''{0}''", topFields[0]);
            return Optional.empty();
        }
        
        // Verify the response by decrypting values and check for state valid timeout
        String raw = StringUtil.decrypt(topFields[1], idp.clientSecret);
        String[] stateFields = raw.split("~", -1);
        if (idp.getId().equals(stateFields[0])) {
            long timeOrigin = Long.parseLong(stateFields[1]);
            long timeDifference = this.clock.millis() - timeOrigin;
            if (timeDifference > 0 && timeDifference < STATE_TIMEOUT) {
                if ( stateFields.length > 3) {
                    this.redirectPage = Optional.ofNullable(stateFields[3]);
                }
                return Optional.of(idp);
            } else {
                logger.info("State timeout");
                return Optional.empty();
            }
        } else {
            logger.log(Level.INFO, "Invalid id field: ''{0}''", stateFields[0]);
            return Optional.empty();
        }
    }
    
    /**
     * Create a randomized unique state string to be used while crafting the autorization request
     * @param idp
     * @param redirectPage
     * @return Random state string, composed from system time, random numbers and redirectPage parameter
     */
    String createState(AbstractOAuth2AuthenticationProvider idp, Optional<String> redirectPage) {
        if (idp == null) {
            throw new IllegalArgumentException("idp cannot be null");
        }
        SecureRandom rand = new SecureRandom();
        
        String base = idp.getId() + "~" + this.clock.millis()
                                  + "~" + rand.nextInt(1000)
                                  + redirectPage.map( page -> "~"+page).orElse("");

        String encrypted = StringUtil.encrypt(base, idp.clientSecret);
        final String state = idp.getId() + "~" + encrypted;
        return state;
    }
    
    /**
     * TODO: Unused. Remove.
     */
    public String getResponseBody() {
        return responseBody;
    }
    
    /**
     * TODO: Unused. Remove.
     */
    public int getResponseCode() {
        return responseCode;
    }
    
    /**
     * TODO: Unused. Remove.
     */
    public OAuth2UserRecord getUser() {
        return oauthUser;
    }

    public OAuth2Exception getError() {
        return error;
    }
    
    /**
     * TODO: Unused. Remove.
     */
    public boolean isInError() {
        return error != null;
    }
    
    /**
     * TODO: Unused. Remove.
     */
    public List<AbstractOAuth2AuthenticationProvider> getProviders() {
        return authenticationSvc.getOAuth2Providers().stream()
                .sorted(Comparator.comparing(AbstractOAuth2AuthenticationProvider::getTitle))
                .collect(toList());
    }
    
    /**
     * TODO: Unused. Remove.
     */
    public boolean isOAuth2ProvidersDefined() {
        return !authenticationSvc.getOAuth2Providers().isEmpty();
    }
}
