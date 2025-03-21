/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

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
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.Stateless;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;

/**
 *
 * @author madunlap
 */
@Stateless
@Path("users")
public class Users extends AbstractApiBean {
    
    private static final Logger logger = Logger.getLogger(Users.class.getName());
    
    @POST
    @AuthRequired
    @Path("{consumedIdentifier}/mergeIntoUser/{baseIdentifier}")
    public Response mergeInAuthenticatedUser(@Context ContainerRequestContext crc, @PathParam("consumedIdentifier") String consumedIdentifier, @PathParam("baseIdentifier") String baseIdentifier) {
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
    public Response changeAuthenticatedUserIdentifier(@Context ContainerRequestContext crc, @PathParam("identifier") String oldIdentifier, @PathParam("newIdentifier")  String newIdentifier) {
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
    public Response recreateToken(@Context ContainerRequestContext crc, @QueryParam("returnExpiration") boolean returnExpiration) {
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
    public Response removeUserRoles(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier) {
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
    public Response getTraces(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier) {
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
    public Response getTracesElement(@Context ContainerRequestContext crc, @Context Request req, @PathParam("identifier") String identifier, @PathParam("element") String element) {
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
    public Response getUserPermittedCollections(@Context ContainerRequestContext crc, @Context Request req, @PathParam("identifier") String identifier, @PathParam("permission") String permission) {
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
            JsonObjectBuilder jsonObj = execCommand(new GetUserPermittedCollectionsCommand(createDataverseRequest(getRequestUser(crc)), userToQuery, permission));
            return ok(jsonObj);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @Path("register")
    public Response registerOIDCUser(String body) {
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
