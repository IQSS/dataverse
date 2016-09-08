package edu.harvard.iq.dataverse.authorization.oauth2.identityproviders;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.api.BaseApi;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.oauth2.AbstractOAuth2Idp;
import edu.harvard.iq.dataverse.authorization.oauth2.OAuth2UserRecord;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * IDP adaptor for GitHub.com
 * @author michael
 */
public class GitHubOAuth2Idp extends AbstractOAuth2Idp {
    
    public GitHubOAuth2Idp() {
        id = "github";
        title = "GitHub";
        clientId = "de1bf3127f3201d3e3a2"; // TODO load from config
        clientSecret = "be71dc2176a37ae72b086dbc3223fc9da5a6d29c"; // TODO load from config
        userEndpoint = "https://api.github.com/user";
        redirectUrl = "http://localhost:8080/oauth2/callback.xhtml"; // TODO load from config
        imageUrl = null;
    }
    
    @Override
    public BaseApi getApiInstance() {
        return GitHubApi.instance();
    }
    
    @Override
    protected ParsedUserResponse parseUserResponse( String responseBody ) {
        
        try ( StringReader rdr = new StringReader(responseBody);
              JsonReader jrdr = Json.createReader(rdr) )  {
            JsonObject response = jrdr.readObject();
            
            AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                    response.getString("name",""),
                    "", // Github has no concept of family name
                    response.getString("email",""),
                    response.getString("company",""),
                    ""
            );
            return new ParsedUserResponse(displayInfo, response.getString("login"));
        }
        
    }
    
}
