/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;

import static edu.harvard.iq.dataverse.api.auth.AuthUtil.extractBearerTokenFromHeaderParam;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.Stateless;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 *
 * @author madunlap
 */
@Stateless
@Path("users")
@Tag(name = "Users", description = "User account and authenticated user operations.")
public class Users extends AbstractApiBean {
    
    private static final Logger logger = Logger.getLogger(Users.class.getName());
    
    @POST
    @AuthRequired
    @Path("{consumedIdentifier}/mergeIntoUser/{baseIdentifier}")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Merges one user account into another",
            description = "Moves account data from one authenticated user into another when the requester is a superuser.")
    public Response mergeInAuthenticatedUser(@Context ContainerRequestContext crc,
            @Parameter(description = "Identifier of the authenticated user account whose data is merged into another account.", required = true)
            @PathParam("consumedIdentifier") String consumedIdentifier,
            @Parameter(description = "Identifier of the authenticated user account that receives the merged account data.", required = true)
            @PathParam("baseIdentifier") String baseIdentifier) {
        User u;
        try {
            u = getRequestUser(crc);
            if(!u.isSuperuser()) {
                throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "Only superusers can merge users"));
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        
        if(null == baseIdentifier || baseIdentifier.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Base identifier provided to change is empty.");
        } else if(null == consumedIdentifier || consumedIdentifier.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Identifier to merge in is empty.");
        }

        AuthenticatedUser baseAuthenticatedUser = authSvc.getAuthenticatedUser(baseIdentifier);
        if (baseAuthenticatedUser == null) {
            return error(Response.Status.BAD_REQUEST, "User " + baseIdentifier + " not found in AuthenticatedUser");
        }

        AuthenticatedUser consumedAuthenticatedUser = authSvc.getAuthenticatedUser(consumedIdentifier);
        if (consumedAuthenticatedUser == null) {
            return error(Response.Status.BAD_REQUEST, "User " + consumedIdentifier + " not found in AuthenticatedUser");
        }

        try {
            execCommand(new MergeInAccountCommand(createDataverseRequest(u), consumedAuthenticatedUser,  baseAuthenticatedUser));
        } catch (Exception e){
            return error(Response.Status.BAD_REQUEST, "Error calling ChangeUserIdentifierCommand: " + e.getLocalizedMessage());
        }

        return ok(String.format("All account data for %s has been merged into %s.", consumedIdentifier, baseIdentifier));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/changeIdentifier/{newIdentifier}")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Changes a user identifier",
            description = "Changes an authenticated user's identifier when the requester is a superuser.")
    public Response changeAuthenticatedUserIdentifier(@Context ContainerRequestContext crc,
            @Parameter(description = "Current authenticated user identifier.", required = true)
            @PathParam("identifier") String oldIdentifier,
            @Parameter(description = "New authenticated user identifier to assign.", required = true)
            @PathParam("newIdentifier") String newIdentifier) {
        User u;
        try {
            u = getRequestUser(crc);
            if(!u.isSuperuser()) {
                throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "Only superusers can change userIdentifiers"));
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        
        if(null == oldIdentifier || oldIdentifier.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Old identifier provided to change is empty.");
        } else if(null == newIdentifier || newIdentifier.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "New identifier provided to change is empty.");
        }

        AuthenticatedUser authenticatedUser = authSvc.getAuthenticatedUser(oldIdentifier);
        if (authenticatedUser == null) {
            return error(Response.Status.BAD_REQUEST, "User " + oldIdentifier + " not found in AuthenticatedUser");
        }

        try {
            execCommand(new ChangeUserIdentifierCommand(createDataverseRequest(u), authenticatedUser,  newIdentifier));
        } catch (Exception e){
            return error(Response.Status.BAD_REQUEST, "Error calling ChangeUserIdentifierCommand: " + e.getLocalizedMessage());
        }

        return ok("UserIdentifier changed from " + oldIdentifier + " to " + newIdentifier);
    }
    
    @Path("token")
    @AuthRequired
    @DELETE
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Deletes the current user's API token",
            description = "Removes the API token for the authenticated user.")
    public Response deleteToken(@Context ContainerRequestContext crc) {
        User u = getRequestUser(crc);
        AuthenticatedUser au;
       
        try{
             au = (AuthenticatedUser) u; 
        } catch (ClassCastException e){ 
            //if we have a non-authenticated user we stop here.
            return notFound("Token for " + u.getIdentifier() + " not eligible for deletion.");
        }       
       
        authSvc.removeApiToken(au);
        return ok("Token for " + au.getUserIdentifier() + " deleted.");
        
    }
    
    @Path("token")
    @AuthRequired
    @GET
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Returns the current user's API token expiration",
            description = "Returns the authenticated user's API token string and expiration time.")
    public Response getTokenExpirationDate(@Context ContainerRequestContext crc) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            ApiToken token = authSvc.findApiTokenByUser(user);

            if (token == null) {
                return notFound("Token not found.");
            }

            return ok(String.format("Token %s expires on %s", token.getTokenString(), token.getExpireTime()));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    @Path("token/recreate")
    @AuthRequired
    @POST
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Recreates the current user's API token",
            description = "Deletes the authenticated user's existing API token, creates a new token, and optionally includes its expiration time in the response.")
    public Response recreateToken(@Context ContainerRequestContext crc,
            @Parameter(description = "Include the new token expiration time in the response.")
            @QueryParam("returnExpiration") boolean returnExpiration) {
        User u = getRequestUser(crc);

        AuthenticatedUser au;        
        try{
             au = (AuthenticatedUser) u; 
        } catch (ClassCastException e){ 
            //if we have a non-authenticated user we stop here.
            return notFound("Token for " + u.getIdentifier() + " is not eligible for recreation.");
        } 
        

        authSvc.removeApiToken(au);

        ApiToken newToken = authSvc.generateApiTokenForUser(au);
        authSvc.save(newToken);

        String message = "New token for " + au.getUserIdentifier() + " is " + newToken.getTokenString();
        if (returnExpiration) {
            message += " and expires on " + newToken.getExpireTime();
        }

        return ok(message);
    }
    
    @GET
    @AuthRequired
    @Path(":me")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Returns the authenticated user",
            description = "Returns the authenticated user associated with the supplied API token or active API session.")
    public Response getAuthenticatedUserByToken(@Context ContainerRequestContext crc) {

        String tokenFromRequestAPI = getRequestApiKey();

        AuthenticatedUser authenticatedUser = findUserByApiToken(tokenFromRequestAPI);
        // This allows use of the :me API call from an active login session. Not sure
        // this is a good idea
        if (authenticatedUser == null) {
            try {
                authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            } catch (WrappedResponse ex) {
                Logger.getLogger(Users.class.getName()).log(Level.SEVERE, null, ex);
                return error(Response.Status.BAD_REQUEST, "User with token " + tokenFromRequestAPI + " not found.");
            }
        }
        return ok(json(authenticatedUser));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/removeRoles")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Removes all roles from a user",
            description = "Revokes all role assignments for the specified authenticated user.")
    public Response removeUserRoles(@Context ContainerRequestContext crc,
            @Parameter(description = "Authenticated user identifier whose role assignments are removed.", required = true)
            @PathParam("identifier") String identifier) {
        try {
            AuthenticatedUser userToModify = authSvc.getAuthenticatedUser(identifier);
            if (userToModify == null) {
                return error(Response.Status.BAD_REQUEST, "Cannot find user based on " + identifier + ".");
            }
            execCommand(new RevokeAllRolesCommand(userToModify, createDataverseRequest(getRequestUser(crc))));
            return ok("Roles removed for user " + identifier + ".");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/traces")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Returns user traces",
            description = "Returns trace information showing where the specified authenticated user appears across role assignments, groups, datasets, files, guestbooks, and saved searches.")
    public Response getTraces(@Context ContainerRequestContext crc,
            @Parameter(description = "Authenticated user identifier whose traces are returned.", required = true)
            @PathParam("identifier") String identifier) {
        try {
            AuthenticatedUser userToQuery = authSvc.getAuthenticatedUser(identifier);
            JsonObjectBuilder jsonObj = execCommand(new GetUserTracesCommand(createDataverseRequest(getRequestUser(crc)), userToQuery, null));
            return ok(jsonObj);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    private List<String> elements = Arrays.asList("roleAssignments","dataverseCreator", "dataversePublisher","datasetCreator", "datasetPublisher","dataFileCreator","dataFilePublisher","datasetVersionUsers","explicitGroups","guestbookEntries", "savedSearches");
    
    @GET
    @AuthRequired
    @Path("{identifier}/traces/{element}")
    @Produces("text/csv, application/json")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Returns a user trace element",
            description = "Returns one category of trace information for the specified authenticated user as JSON or CSV.")
    public Response getTracesElement(@Context ContainerRequestContext crc, @Context Request req,
            @Parameter(description = "Authenticated user identifier whose trace element is returned.", required = true)
            @PathParam("identifier") String identifier,
            @Parameter(description = "Trace category to return, such as roleAssignments, explicitGroups, guestbookEntries, or savedSearches.", required = true)
            @PathParam("element") String element) {
        try {
            AuthenticatedUser userToQuery = authSvc.getAuthenticatedUser(identifier);
            if(!elements.contains(element)) {
                throw new BadRequestException("Not a valid element");
            }
            JsonObjectBuilder jsonObj = execCommand(new GetUserTracesCommand(createDataverseRequest(getRequestUser(crc)), userToQuery, element));
            
            List<Variant> vars = Variant
                    .mediaTypes(MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE)
                    .add()
                    .build();
            MediaType requestedType = req.selectVariant(vars).getMediaType();
            if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
                return ok(jsonObj);
            
            }
            JsonArray items=null;
            try {
                items = jsonObj.build().getJsonObject("traces").getJsonObject(element).getJsonArray("items");
            } catch(Exception e) {
                return ok(jsonObj);
            }
            return ok(FileUtil.jsonArrayOfObjectsToCSV(items, items.getJsonObject(0).keySet().toArray(new String[0])), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), element + ".csv");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/allowedCollections/{permission}")
    @Produces("application/json")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Lists collections permitted for a user",
            description = "Returns collections where the specified user has the requested permission when the requester is that user or a superuser.")
    public Response getUserPermittedCollections(@Context ContainerRequestContext crc, @Context Request req,
            @Parameter(description = "Authenticated user identifier whose permitted collections are returned.", required = true)
            @PathParam("identifier") String identifier,
            @Parameter(description = "Permission name used to select permitted collections.", required = true)
            @PathParam("permission") String permission) {
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (!authenticatedUser.getUserIdentifier().equalsIgnoreCase(identifier) && !authenticatedUser.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by Users getting there own permitted collections or by superusers.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.UNAUTHORIZED, "Authentication is required.");
        }
        try {
            AuthenticatedUser userToQuery = authSvc.getAuthenticatedUser(identifier);
            List<Dataverse> collections = execCommand(new GetUserPermittedCollectionsCommand(createDataverseRequest(getRequestUser(crc)), userToQuery, permission));
            return ok(JsonPrinter.jsonArray(collections));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @Path("register")
    @Operation(summary = "Registers an OIDC user",
            description = "Registers an OIDC user from the supplied user JSON when API bearer authentication is enabled and a bearer token is present.")
    public Response registerOIDCUser(
            @RequestBody(description = "User JSON parsed into a new authenticated OIDC user registration.")
            String body) {
        if (!FeatureFlags.API_BEARER_AUTH.enabled()) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, BundleUtil.getStringFromBundle("users.api.errors.bearerAuthFeatureFlagDisabled"));
        }
        Optional<String> bearerToken = extractBearerTokenFromHeaderParam(httpRequest.getHeader(HttpHeaders.AUTHORIZATION));
        if (bearerToken.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("users.api.errors.bearerTokenRequired"));
        }
        try {
            JsonObject userJson = JsonUtil.getJsonObject(body);
            execCommand(new RegisterOIDCUserCommand(createDataverseRequest(GuestUser.get()), bearerToken.get(), jsonParser().parseUserDTO(userJson)));
        } catch (JsonParseException | JsonParsingException e) {
            return error(Response.Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("users.api.errors.jsonParseToUserDTO"), e.getMessage()));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
        return ok(BundleUtil.getStringFromBundle("users.api.userRegistered"));
    }
}
