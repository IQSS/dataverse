package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.api.DefaultApi20;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUserNameFields;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.StringReader;
import java.util.Collections;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * IDP adaptor for GitHub.com
 * @author michael
 */
public class GitHubOAuth2AP extends AbstractOAuth2AuthenticationProvider {

    public static final String PROVIDER_ID = "github";

    public GitHubOAuth2AP(String aClientId, String aClientSecret) {
        id = PROVIDER_ID;
        title = BundleUtil.getStringFromBundle("auth.providers.title.github");
        clientId = aClientId;
        clientSecret = aClientSecret;
        baseUserEndpoint = "https://api.github.com/user";
    }
    
    @Override
    public DefaultApi20 getApiInstance() {
        return GitHubApi.instance();
    }
    
    @Override
    protected ParsedUserResponse parseUserResponse( String responseBody ) {
        
        try ( StringReader rdr = new StringReader(responseBody);
              JsonReader jrdr = Json.createReader(rdr) )  {
            JsonObject response = jrdr.readObject();
            // Github has no concept of a family name
            ShibUserNameFields shibUserNameFields = ShibUtil.findBestFirstAndLastName(null, null, response.getString("name",""));
            AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                    shibUserNameFields.getFirstName(),
                    shibUserNameFields.getLastName(),
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
