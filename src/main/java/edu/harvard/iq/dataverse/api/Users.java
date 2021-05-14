/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.ChangeUserIdentifierCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetUserTracesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.MergeInAccountCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeAllRolesCommand;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 *
 * @author madunlap
 */
@Stateless
@Path("users")
public class Users extends AbstractApiBean {
    
    private static final Logger logger = Logger.getLogger(Users.class.getName());
    
    @POST
    @Path("{consumedIdentifier}/mergeIntoUser/{baseIdentifier}")
    public Response mergeInAuthenticatedUser(@PathParam("consumedIdentifier") String consumedIdentifier, @PathParam("baseIdentifier") String baseIdentifier) {
        User u;
        try {
            u = findUserOrDie();
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

        return ok("All account data for " + consumedIdentifier + " has been merged into " + baseIdentifier + " .");
    }

    @POST
    @Path("{identifier}/changeIdentifier/{newIdentifier}")
    public Response changeAuthenticatedUserIdentifier(@PathParam("identifier") String oldIdentifier, @PathParam("newIdentifier")  String newIdentifier) {
        User u;
        try {
            u = findUserOrDie();
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
    @DELETE
    public Response deleteToken() {
        User u;

        try {
            u = findUserOrDie();
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
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
    @GET
    public Response getTokenExpirationDate() {
        User u;
        
        try {
            u = findUserOrDie();
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }      
        
        ApiToken token = authSvc.findApiToken(getRequestApiKey());
        
        if (token == null) {
            return notFound("Token " + getRequestApiKey() + " not found.");
        }
        
        return ok("Token " + getRequestApiKey() + " expires on " + token.getExpireTime());
        
    }
    
    @Path("token/recreate")
    @POST
    public Response recreateToken() {
        User u;

        try {
            u = findUserOrDie();
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        
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
    @Path(":me")
    public Response getAuthenticatedUserByToken() {

        String tokenFromRequestAPI = getRequestApiKey();

        AuthenticatedUser authenticatedUser = findUserByApiToken(tokenFromRequestAPI);
        if (authenticatedUser == null) {
            return error(Response.Status.BAD_REQUEST, "User with token " + tokenFromRequestAPI + " not found.");
        } else {
            return ok(json(authenticatedUser));
        }

    }

    @POST
    @Path("{identifier}/removeRoles")
    public Response removeUserRoles(@PathParam("identifier") String identifier) {
        try {
            AuthenticatedUser userToModify = authSvc.getAuthenticatedUser(identifier);
            if (userToModify == null) {
                return error(Response.Status.BAD_REQUEST, "Cannot find user based on " + identifier + ".");
            }
            execCommand(new RevokeAllRolesCommand(userToModify, createDataverseRequest(findUserOrDie())));
            return ok("Roles removed for user " + identifier + ".");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @Path("{identifier}/traces")
    public Response getTraces(@PathParam("identifier") String identifier) {
        try {
            AuthenticatedUser userToQuery = authSvc.getAuthenticatedUser(identifier);
            JsonObjectBuilder jsonObj = execCommand(new GetUserTracesCommand(createDataverseRequest(findUserOrDie()), userToQuery));
            return ok(jsonObj);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

}
