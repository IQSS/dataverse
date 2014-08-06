package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.ApiKey;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
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
        return ok(userStuff.toString());
    }

    @GET
    @Path("username/{username}")
    public String getAuthenicatedUserFromUsername(@PathParam("username") String username) {
        AuthenticatedUser user = userService.findByUsername(username);
        if (user != null) {
            JsonObjectBuilder userObject = Json.createObjectBuilder();
            userObject
                    .add("username", user.getIdentifier())
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
}
