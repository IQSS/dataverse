package edu.harvard.iq.dataverse.authorization.providers.oauth2.identityproviders;

import com.github.scribejava.core.builder.api.BaseApi;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2Idp;
import java.util.logging.Logger;

/**
 *
 * @author michael
 */
public class OrcidPublicOAuth2Idp extends AbstractOAuth2Idp {
    
    private static final String API_ENDPOINT = "https://pub.orcid.org/v1.2/"; //https://api.sandbox.orcid.org/"; 
    
    public OrcidPublicOAuth2Idp() {
        id = "orcid-pub";
        title = "ORCiD (public api)";
        clientId = "APP-3FSE1LYJI56P5VNJ"; // TODO load from config
        clientSecret = "771e0798-0a0a-4748-99f4-a4d8c6faa037"; // TODO load from config
        redirectUrl = "http://localhost:8080/oauth2/callback.xhtml"; // TODO load from config
        scope = "/read-public"; // might need to add "/authenticate" again
// Sandbox data
//        clientId = "APP-HIV99BRM37FSWPH6"; // TODO load from config
//        clientSecret = "ee844b70-f223-4f15-9b6f-4991bf8ed7f0"; // TODO load from config
//        redirectUrl = "http://localhost:8080/oauth2-callback"; // TODO load from config
        userEndpoint = API_ENDPOINT + "orcid_id";
        imageUrl = null;
    }
    
    @Override
    public BaseApi getApiInstance() {
        return OrcidApi.instance();
    }

    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        Logger.getAnonymousLogger().info("ORCiD Response:");
        Logger.getAnonymousLogger().info(responseBody);
        String username = null;
        return new ParsedUserResponse(new AuthenticatedUserDisplayInfo("fn", "ln", "email", "aff", "pos"), "id in ORCiD", username);
    }
    
}
