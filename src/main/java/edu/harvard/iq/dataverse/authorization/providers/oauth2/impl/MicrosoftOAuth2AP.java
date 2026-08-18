package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.api.DefaultApi20;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonObject;
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
        this.scope = Arrays.asList("User.Read");
        this.baseUserEndpoint = "https://graph.microsoft.com/v1.0/me";
    }

    @Override
    public DefaultApi20 getApiInstance(){
        return MicrosoftAzureActiveDirectory20Api.instance();
    }

    @Override
    protected ParsedUserResponse parseUserResponse(final String responseBody) {
        JsonObject response = JsonUtil.getJsonObject(responseBody);
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

    public boolean isDisplayIdentifier()
    {
        return false;
    }
}