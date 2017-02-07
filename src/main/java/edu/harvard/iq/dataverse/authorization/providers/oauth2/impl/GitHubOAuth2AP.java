package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.api.BaseApi;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUserNameFields;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil;
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

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    @Override
    public String getPersistentIdName() {
        return BundleUtil.getStringFromBundle("auth.providers.persistentUserIdName.github");
    }

    @Override
    public String getPersistentIdDescription() {
        return BundleUtil.getStringFromBundle("auth.providers.persistentUserIdTooltip.github");
    }

    @Override
    public String getPersistentIdUrlPrefix() {
        return null;
    }

    @Override
    public String getLogo() {
        return null;
    }
    
}
