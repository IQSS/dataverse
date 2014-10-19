package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("auth")
public class Auth extends AbstractApiBean {

    @GET
    public String get() {
        List<AuthenticatedUser> users = authSvc.findAllAuthenticatedUsers();
        List<String> userStuff = new ArrayList<>();
        for (AuthenticatedUser authenticatedUser : users) {
            RoleAssigneeDisplayInfo displayInfo = authenticatedUser.getDisplayInfo();
            userStuff.add(authenticatedUser.getIdentifier() + ":" + displayInfo);
        }
        
        // TODO - who gets the user's API keys here? Not sure it's a very secure solution...
        List<ApiToken> apiKeys = userSvc.findAllApiKeys();
        for (ApiToken apiKey : apiKeys) {
            userStuff.add(apiKey.getTokenString());
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
                .add("numAuthProviders", authSvc.getAuthenticationProviderIds().size())
                .add("endpoints", Json.createArrayBuilder()
                        .add(usernameEndpoint)
                        .add(lookupEndpoint)
                        .add(apiKeyEndpoint));
        return ok(info.build());
    }

    @GET
    @Path("foo")
    public Response getFoo(@QueryParam("pretty") boolean prettyPrintResponse) {
        JsonArrayBuilder bar = Json.createArrayBuilder().add("foo").add("bar");
        if (prettyPrintResponse) {
            return okResponse(bar, Format.PRETTY);
        } else {
            return okResponse(bar);
        }
    }

    private final String usernameEndpoint = "username";
    private final String usernameParam = "username";
    private final String usernameEndpointSignature = usernameEndpoint + "/{" + usernameParam + "}";

    @GET
    @Path(usernameEndpoint)
    public String getUsername() {
        return error("Please provide a username. The endpoint expects " + usernameEndpointSignature);
    }

    @GET
    @Path(usernameEndpointSignature)
    public Response getAuthenicatedUserFromUsername(@PathParam(usernameParam) String username) {
        AuthenticatedUser user = userSvc.findByUsername(username);
        if (user != null) {
            return okResponse( json(user) );
        } else {
            return notFound("Couldn't find user based on username: " + username);
        }
    }

    private final String lookupEndpoint = "lookup";

    @GET
    @Path("lookup/{idp}/{id}")
    public String getAuthenticatedUser(@PathParam("idp") String idp, @PathParam("id") String id) {
        AuthenticatedUser authenticatedUser = authSvc.lookupUser(idp, id);
        if (authenticatedUser != null) {
            return ok(authenticatedUser.getDisplayInfo() + " (" + authenticatedUser.getIdentifier() + ")");
        } else {
            return error("Couldn't find user based on " + id);
        }
    }

    private final String apiKeyEndpoint = "apikey";

    @GET
    @Path(apiKeyEndpoint + "/{key}")
    public Response getApiKeyInfo(@PathParam("key") String key) {
        ApiToken apiKey = authSvc.findApiToken(key);
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
            return errorResponse(Response.Status.NOT_FOUND, "Couldn't find a key based on " + key);
        }
    }
}
