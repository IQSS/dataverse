package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.api.BaseApi;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * IDP adaptor for GitHub.com
 * @author michael
 */
public class GitHubOAuth2AP extends AbstractOAuth2AuthenticationProvider {
    
    public GitHubOAuth2AP(String aClientId, String aClientSecret) {
        id = "github";
        title = BundleUtil.getStringFromBundle("auth.providers.title.github");
        clientId = aClientId;
        clientSecret = aClientSecret;
        baseUserEndpoint = "https://api.github.com/user";
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
            return new ParsedUserResponse(
                    displayInfo, 
                    persistentUserId.toString(), 
                    username,
                    displayInfo.getEmailAddress().length()>0 ? Collections.singletonList(displayInfo.getEmailAddress())
                                                             : Collections.emptyList() );
        }
        
    }
    
}
