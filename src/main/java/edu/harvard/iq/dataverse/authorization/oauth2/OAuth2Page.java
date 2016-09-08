package edu.harvard.iq.dataverse.authorization.oauth2;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.authorization.oauth2.identityproviders.GitHubOAuth2Idp;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.servlet.http.HttpServletRequest;

/**
 * 
 * FIXME MBS State should reflect the IDP to use for the token exchange and user data, and some timeout. Then encrypt and BASE64.
 * @author michael
 */
@Named(value = "OAuth2Page")
@ViewScoped
public class OAuth2Page implements Serializable {
    
    static final AbstractOAuth2Idp idp = new GitHubOAuth2Idp();
   
    static final String STATE = "QWEASDZXC";
    
    static int counter = 0;
    
    private static final Logger logger = Logger.getLogger(OAuth2Page.class.getName());
    
    final String GH_STATE = idp.id + "--" + STATE;
    private int responseCode;
    private String responseBody;
    
    private OAuth2UserRecord user;
    
    public String counter() { 
        counter++;
        logger.info( "counter is " + counter );
        return "number " + counter;
    }
    
    public String gitHubLink() {
        return idp.getService(GH_STATE).getAuthorizationUrl();
    }

    public void exchangeCodeForToken() throws IOException {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        
        // TODO check for parameter "error", or catch OAuthException.
        final String code = req.getParameter("code");
        if ( code == null ) {
            return;
        }
        
        logger.info("Code: " + code);
        logger.info("State: " + req.getParameter("state"));
        // TODO validate state is same.
        logger.info("getting access token");
        try {
            user = idp.getUserRecord(code, GH_STATE);
        } catch (OAuth2Exception ex) {
            Logger.getLogger(OAuth2Page.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public OAuth2UserRecord getUser() {
        return user;
    }
    
}
