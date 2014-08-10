package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.ApiKey;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.AuthenticationManager;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import javax.ws.rs.core.Response;

@Path("auth")
public class Auth extends AbstractApiBean {

    @EJB
    UserServiceBean userService;

    @GET
    public String get() {
        List<AuthenticatedUser> users = userService.findAllAuthenticatedUsers();
        List<String> userStuff = new ArrayList<>();
        for (AuthenticatedUser authenticatedUser : users) {
            RoleAssigneeDisplayInfo displayInfo = authenticatedUser.getDisplayInfo();
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
    public Response getAuthenicatedUserFromUsername(@PathParam("username") String username) {
        AuthenticatedUser user = userService.findByUsername(username);
        if (user != null) {
            return okResponse( json(user) );
        } else {
            return notFound("Couldn't find user based on username: " + username);
        }
    }

    @GET
    @Path("lookup/{idp}/{id}")
    public String getAuthenticatedUser(@PathParam("idp") String idp, @PathParam("id") String id) {
        AuthenticatedUserLookup userIdLookupString = userService.findByPersitentIdFromIdp(idp, id);
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
    public Response getApiKeyInfo(@PathParam("key") String key) {
        ApiKey apiKey = userService.findApiKey(key);
        if (apiKey != null) {
            AuthenticatedUser user = apiKey.getAuthenticatedUser();
            if (user != null) {
                JsonObjectBuilder keyInfo = Json.createObjectBuilder();
                keyInfo
                        .add("owner", json(user))
                        .add("disabled", apiKey.isDisabled())
                        .add("created", apiKey.getCreateTime().toString())
                        .add("expires", apiKey.getExpireTime().toString());
                return okResponse(keyInfo);
            } else {
                return notFound("Couldn't find user based on " + key);
            }
        } else {
            return notFound("Couldn't find user based on " + key);
        }
    }
}
