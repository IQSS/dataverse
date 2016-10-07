package edu.harvard.iq.dataverse.authorization.providers.oauth2.identityproviders;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.api.BaseApi;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * IDP adaptor for GitHub.com
 * @author michael
 */
public class GitHubOAuth2Idp extends AbstractOAuth2AuthenticationProvider {
    
    public GitHubOAuth2Idp(String aClientId, String aClientSecret) {
        id = "github";
        title = "GitHub";
        clientId = aClientId;
        clientSecret = aClientSecret;
        userEndpoint = "https://api.github.com/user";
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
                    "", // Github has no concept of a family name
                    response.getString("email",""),
                    response.getString("company",""),
                    ""
            );
            Integer persistentUserId = response.getInt("id");
            String username = response.getString("login");
            return new ParsedUserResponse(displayInfo, persistentUserId.toString(), username);
        }
        
    }
    
}
