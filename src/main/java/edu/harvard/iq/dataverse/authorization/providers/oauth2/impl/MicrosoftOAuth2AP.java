package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;

import java.util.Collections;
import java.util.logging.Logger;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;

/**
 *
 * @author
 */
public class MicrosoftOAuth2AP extends AbstractOAuth2AuthenticationProvider{

    private static final Logger logger = Logger.getLogger(MicrosoftOAuth2AP.class.getCanonicalName());

    public MicrosoftOAuth2AP(String aClientId, String aClientSecret){
        this.id = "microsoft";
        this.title = "Microsoft";
        this.clientId = aClientId;
        this.clientSecret = aClientSecret;
        this.scope = "user.read";
        this.baseUserEndpoint = "https://graph.microsoft.com/v1.0/me";
    }

    @Override
    public BaseApi<OAuth20Service> getApiInstance(){
        return MicrosoftAzureApi.instance();
    }

    @Override
    protected ParsedUserResponse parseUserResponse(final String responseBody) {
        try ( StringReader rdr = new StringReader(responseBody);
              JsonReader jrdr = Json.createReader(rdr) )  {
            JsonObject response = jrdr.readObject();
            AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                    response.getString("givenName", ""),
                    response.getString("surname", ""),
                    response.getString("userPrincipalName", ""),
                    "", "");
            String persistentUserId = response.getString("id");
            String username = response.getString("userPrincipalName");
            return new ParsedUserResponse(displayInfo, persistentUserId, username,
                    (displayInfo.getEmailAddress().length() > 0 ? Collections.singletonList(displayInfo.getEmailAddress()) : Collections.emptyList() )
            );
        }
    }

    public boolean isDisplayIdentifier()
    {
        return false;
    }
}