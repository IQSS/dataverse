package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * 
 * FIXME MBS State should reflect the IDP to use for the token exchange and user data, and some timeout. Then encrypt and BASE64.
 * @author michael
 */
@Named(value = "OAuth2Page")
@ViewScoped
public class OAuth2Page implements Serializable {
    
    static int counter = 0;
    
    private static final Logger logger = Logger.getLogger(OAuth2Page.class.getName());
    private static final long STATE_TIMEOUT = 1000*60*15; // 15 minutes in msec
    private int responseCode;
    private String responseBody;
    private OAuth2Exception error;
    private OAuth2UserRecord oauthUser;
    private final OAuth2AuthenticationProvider oauthPrv = new OAuth2AuthenticationProvider();
    
    @EJB
    AuthenticationServiceBean authenticationSvc;
    
    @Inject
    DataverseSession session;
    
    public String counter() { 
        return "number " + (++counter);
    }
    
    public String linkFor( String idpId ) {
        AbstractOAuth2Idp idp = oauthPrv.getProvider(idpId);
        return idp.getService(createState(idp)).getAuthorizationUrl();
    }

    public void exchangeCodeForToken() throws IOException {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        
        final String code = req.getParameter("code");
        logger.info("Code: '" + code + "'"); // TODO remove
        if ( code == null || code.trim().isEmpty() ) {
            try( BufferedReader rdr = req.getReader() ) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ( (line=rdr.readLine())!=null ) {
                    sb.append(line).append("\n");
                }
                error = new OAuth2Exception(-1, sb.toString(), "Remote system did not return an authorization code.");
                return;
            }
        }
        
        final String state = req.getParameter("state");
        
        try {
            AbstractOAuth2Idp idp = getIdpFromState(state);
            if ( idp == null ) {
                throw new OAuth2Exception(-1, "", "Invalid 'state' parameter");
            }
            oauthUser = idp.getUserRecord(code, state);
            UserRecordIdentifier idtf = oauthUser.getUserRecordIdentifier();
            AuthenticatedUser dvUser = authenticationSvc.lookupUser(idtf);
            
            if ( dvUser == null ) {
                // need to create the user
                dvUser = authenticationSvc.createAuthenticatedUser(idtf, oauthUser.getIdInService(), oauthUser.getDisplayInfo(), true);
                
            } else {
                // update profile
                dvUser = authenticationSvc.updateAuthenticatedUser(dvUser, oauthUser.getDisplayInfo());
            }
            
            // login the user and redirect to HOME.
            session.setUser(dvUser);
            FacesContext.getCurrentInstance().getExternalContext().redirect("/");
            
        } catch (OAuth2Exception ex) {
            error = ex;
            Logger.getLogger(OAuth2Page.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private AbstractOAuth2Idp getIdpFromState( String state ) {
        String[] topFields = state.split("~",2);
        if ( topFields.length != 2 ) {
            logger.log(Level.INFO, "Wrong number of fields in state string", state);
            return null;
        }
        AbstractOAuth2Idp idp = oauthPrv.getProvider( topFields[0] );
        if ( idp == null ) { 
            logger.log(Level.INFO, "Can''t find IDP ''{0}''", topFields[0]);
            return null;
        }
        String raw = StringUtil.decrypt(topFields[1], idp.clientSecret);
        String[] stateFields = raw.split("~", -1);
        if ( idp.getId().equals(stateFields[0]) ) {
            long timeOrigin = Long.parseLong(stateFields[1]);
            long timeDifference = System.currentTimeMillis() - timeOrigin;
            if ( timeDifference > 0 && timeDifference < STATE_TIMEOUT ) {
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
    
    private String createState( AbstractOAuth2Idp idp ) {
        if ( idp == null ) {
            throw new IllegalArgumentException("idp cannot be null");
        }
        String base = idp.getId() + "~" + System.currentTimeMillis() + "~" + (int)java.lang.Math.round( java.lang.Math.random()*1000 );
        
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
        return error!=null;
    }
    
    public List<AbstractOAuth2Idp> getProviders() {
        List<AbstractOAuth2Idp> providerList = new ArrayList<>();
        oauthPrv.providers().forEach( providerList::add );
        return providerList;
    }
    
}
