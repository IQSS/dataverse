package edu.harvard.iq.dataverse.authorization.providers.oauth2.identityproviders;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.builder.api.BaseApi;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 *
 * @author michael
 */
public class GoogleOAuth2Idp extends AbstractOAuth2AuthenticationProvider {
    
    public GoogleOAuth2Idp(String aClientId, String aClientSecret) {
        id = "google";
        title = "Google";
        clientId = aClientId;
        clientSecret = aClientSecret;
        scope =  "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email";
        userEndpoint = "https://www.googleapis.com/oauth2/v2/userinfo";
    }
    
    @Override
    public BaseApi getApiInstance() {
        return GoogleApi20.instance();
    }

    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        try ( StringReader rdr = new StringReader(responseBody);
              JsonReader jrdr = Json.createReader(rdr) )  {
            JsonObject response = jrdr.readObject();
            
            AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                    response.getString("given_name",""),
                    response.getString("family_name",""),
                    response.getString("email",""),
                    "",
                    ""
            );
            String immutableUserId = response.getString("id");
            String username = null;
            return new ParsedUserResponse(displayInfo, immutableUserId, username);
        }
    }
    
}
