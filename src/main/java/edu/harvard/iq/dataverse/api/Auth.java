package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.ApiKey;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.AuthenticationManager;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("auth")
public class Auth extends AbstractApiBean {

    @EJB
    UserServiceBean userService;

    @GET
    public String get() {
        AuthenticatedUser ua = null;
        List<AuthenticatedUser> users = userService.findAllAuthenticatedUsers();
        List<String> userStuff = new ArrayList<>();
        for (AuthenticatedUser authenticatedUser : users) {
            String displayInfo = authenticatedUser.getDisplayInfo();
            userStuff.add(authenticatedUser.getIdentifier() + ":" + displayInfo);
        }
        List<AuthenticatedUserLookup> lookupStrings = userService.findByAllLookupStrings();
        for (AuthenticatedUserLookup authenticatedUserLookup : lookupStrings) {
            userStuff.add(authenticatedUserLookup.getPersistentUserIdFromIdp());
        }
        List<ApiKey> apiKeys = userService.findAllApiKeys();
        for (ApiKey apiKey : apiKeys) {
            userStuff.add(apiKey.getKey());
        }
        /**
         * [jsmith:John Smith, jasmith:John Smith,
         * B4D449E0-561E-493D-B514-96781D70CD18,
         * 5D080DBB-73AE-47FE-9D11-C9960C93D59A,
         * shib:idp.testshib.org:0109C89C-4BA2-42A5-969D-BB43D47DB409,
         * local:jsmith, 3ef969eb-a7f5-46f5-b999-669a763893dc]
         */
        JsonObjectBuilder info = Json.createObjectBuilder();
        info
                .add("userStuff", userStuff.toString())
                .add("numAuthProviders", AuthenticationManager.getInstance().getAuthenticationProviders().size());
        return ok(info.build());
    }

    @GET
    @Path("username/{username}")
    public String getAuthenicatedUserFromUsername(@PathParam("username") String username) {
        AuthenticatedUser user = userService.findByUsername(username);
        if (user != null) {
            JsonObjectBuilder userObject = Json.createObjectBuilder();
            userObject
                    .add("identifier", user.getIdentifier())
                    .add("displayInfo", user.getDisplayInfo());
            return ok(userObject.build());
        } else {
            return error("Couldn't find user based on username: " + username);
        }
    }

    @GET
    @Path("lookup/{id}")
    public String getAuthenticatedUser(@PathParam("id") String id) {
        AuthenticatedUserLookup userIdLookupString = userService.findByPersitentIdFromIdp(id);
        if (userIdLookupString != null) {
            AuthenticatedUser user = userIdLookupString.getAuthenticatedUser();
            if (user != null) {
                return ok(user.getDisplayInfo() + " (" + user.getIdentifier() + ")");
            } else {
                return error("Couldn't find user based on " + id);
            }
        } else {
            return error("Couldn't find user based on " + id);
        }
    }

    @GET
    @Path("apikey/{key}")
    public String getApiKeyInfo(@PathParam("key") String key) {
        ApiKey apiKey = userService.findApiKey(key);
        if (apiKey != null) {
            AuthenticatedUser user = apiKey.getAuthenticatedUser();
            if (user != null) {
                JsonObjectBuilder userObject = Json.createObjectBuilder();
                userObject
                        .add("identifier", user.getIdentifier())
                        .add("displayInfo", user.getDisplayInfo());
                JsonObjectBuilder keyInfo = Json.createObjectBuilder();
                keyInfo
                        .add("owner", userObject)
                        .add("disabled", apiKey.isDisabled())
                        .add("created", apiKey.getExpireTime().toString())
                        .add("expires", apiKey.getExpireTime().toString());
                return ok(keyInfo.build());
            } else {
                return error("Couldn't find user based on " + key);
            }
        } else {
            return error("Couldn't find user based on " + key);
        }
    }
}
