/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.ChangeUserIdentifierCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetUserTracesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.MergeInAccountCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeAllRolesCommand;
import edu.harvard.iq.dataverse.util.FileUtil;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;

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
    public Response getTokenExpirationDate() {
        ApiToken token = authSvc.findApiToken(getRequestApiKey());
        
        if (token == null) {
            return notFound("Token " + getRequestApiKey() + " not found.");
        }
        
        return ok("Token " + getRequestApiKey() + " expires on " + token.getExpireTime());
        
    }
    
    @Path("token/recreate")
    @AuthRequired
    @POST
    public Response recreateToken(@Context ContainerRequestContext crc) {
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

        return ok("New token for " + au.getUserIdentifier() + " is " + newToken.getTokenString());

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
    public Response getTraces(@Context ContainerRequestContext crc, @Context Request req, @PathParam("identifier") String identifier, @PathParam("element") String element) {
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

}
