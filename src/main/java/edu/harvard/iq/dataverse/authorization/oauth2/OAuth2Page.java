package edu.harvard.iq.dataverse.authorization.oauth2;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author michael
 */
@Named(value = "OAuth2Page")
@ViewScoped
public class OAuth2Page implements Serializable {
    
    static final String GH_CLIENT_ID = "de1bf3127f3201d3e3a2";
    static final String GH_CLIENT_SECRET = "be71dc2176a37ae72b086dbc3223fc9da5a6d29c";
    static final String GH_CALLBACK = "http://localhost:8080/oauth2/callback.xhtml";
    static final  String GH_USER_ENDPOINT = "https://api.github.com/user";
    static final String STATE = "QWEASDZXC";
    
    static int counter = 0;
    
    private static final Logger logger = Logger.getLogger(OAuth2Page.class.getName());
    
    private int responseCode;
    private String responseBody;
    
    public String counter() { 
        counter++;
        logger.info( "counter is " + counter );
        return "number " + counter;
    }
    
    public String gitHubLink() {
        OAuth20Service gitHubSvc = createGitHubService( );
        
        return gitHubSvc.getAuthorizationUrl();
    }

    public void exchangeCodeForToken() throws IOException {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        final String code = req.getParameter("code");
        if ( code == null ) {
            return;
        }
        
        logger.info("Code: " + code);
        logger.info("State: " + req.getParameter("state"));
        // TODO validate state is same.
        logger.info("getting access token");
        
        OAuth20Service service = createGitHubService();
        OAuth2AccessToken accessToken = service.getAccessToken(code);
        
        logger.info("Got token " + accessToken.getAccessToken());
        final OAuthRequest request = new OAuthRequest(Verb.GET, GH_USER_ENDPOINT, service);
        service.signRequest(accessToken, request);
        final Response response = request.send();
        responseCode = response.getCode();
        responseBody = response.getBody();
    }

    private OAuth20Service createGitHubService() {
        OAuth20Service gitHubSvc = new ServiceBuilder()
                .apiKey(GH_CLIENT_ID)
                .apiSecret(GH_CLIENT_SECRET)
                .state(STATE)
                .callback(GH_CALLBACK)
                .scope("user")
                .build( GitHubApi.instance() );
        return gitHubSvc;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }
    
}
