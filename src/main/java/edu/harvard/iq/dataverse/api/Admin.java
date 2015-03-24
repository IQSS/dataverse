package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.Setting;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response.Status;
/**
 * Where the secure, setup API calls live.
 * @author michael
 */
@Stateless
@Path("admin")
public class Admin extends AbstractApiBean {
    
    private static final Logger logger = Logger.getLogger(Admin.class.getName());
    
    @Path("settings")
    @GET
    public Response listAllSettings() {
        JsonObjectBuilder bld = jsonObjectBuilder();
        for ( Setting s : settingsSvc.listAll() ) {
            bld.add(s.getName(), s.getContent());
        }
        return okResponse(bld);
    }
    
    @Path("settings/{name}")
    @PUT
    public Response putSetting( @PathParam("name") String name, String content ) {
        Setting s = settingsSvc.set(name, content);
        return okResponse( jsonObjectBuilder().add(s.getName(), s.getContent()) );
    }
    
    @Path("settings/{name}")
    @GET
    public Response getSetting( @PathParam("name") String name ) {
        String s = settingsSvc.get(name);
        
        return ( s != null ) 
                ? okResponse( s ) 
                : notFound("Setting " + name + " not found");
    }
    
    @Path("settings/{name}")
    @DELETE
    public Response deleteSetting( @PathParam("name") String name ) {
        settingsSvc.delete(name);
        
        return okResponse("Setting " + name +  " deleted.");
    }
    
    @Path("authenticationProviderFactories")
    @GET
    public Response listAuthProviderFactories() {
        JsonArrayBuilder arb = Json.createArrayBuilder();
        for ( AuthenticationProviderFactory f : authSvc.listProviderFactories() ){
            arb.add( jsonObjectBuilder()
                        .add("alias", f.getAlias() )
                        .add("info", f.getInfo() ));
        }
        
        return okResponse(arb);
    }

    
    @Path("authenticationProviders")
    @GET
    public Response listAuthProviders() {
        JsonArrayBuilder arb = Json.createArrayBuilder();
        for ( AuthenticationProviderRow r :
                em.createNamedQuery("AuthenticationProviderRow.findAll", AuthenticationProviderRow.class).getResultList() ){
            arb.add( json(r) );
        }
        
        return okResponse(arb);
    }
    
    @Path("authenticationProviders")
    @POST
    public Response addProvider( AuthenticationProviderRow row ) {
        try {
            AuthenticationProviderRow managed = em.find(AuthenticationProviderRow.class,row.getId());
            if ( managed != null ) {
                managed = em.merge(row);
            } else  {
                em.persist(row);
                managed = row;
            }
            if ( managed.isEnabled() ) {
                AuthenticationProvider provider = authSvc.loadProvider(managed);
                authSvc.deregisterProvider(provider.getId());
                authSvc.registerProvider(provider);
            }
            return createdResponse("/s/authenticationProviders/"+managed.getId(), json(managed));
        } catch ( AuthorizationSetupException e ) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() );
        }
    }
    
    @Path("authenticationProviders/{id}")
    @GET
    public Response showProvider( @PathParam("id") String id ) {
        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        return (row != null ) ? okResponse( json(row) )
                                : errorResponse(Status.NOT_FOUND,"Can't find authetication provider with id '" + id + "'");
    }
    
    @POST
    @Path("authenticationProviders/{id}/:enabled")
    @Produces("application/json")
    public Response enableAuthenticationProvider( @PathParam("id")String id, String body ) {
        
        if ( ! Util.isBoolean(body) ) {
            return errorResponse(Response.Status.BAD_REQUEST, "Illegal value '" + body + "'. Try 'true' or 'false'");
        }
        boolean enable = Util.isTrue(body);
        
        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        if ( row == null ) {
            return errorResponse( Status.NOT_FOUND, "Can't find authentication provider with id '" + id + "'");
        }
        
        row.setEnabled(enable);
        em.merge(row);
        
        if ( enable ) {
            // enable a provider
            if ( authSvc.getAuthenticationProvider(id) != null ) {
                return okResponse( String.format("Authentication provider '%s' already enabled", id));
            }
            try {
                authSvc.registerProvider( authSvc.loadProvider(row) );
                return okResponse(String.format("Authentication Provider %s enabled", row.getId()));
                
            } catch (AuthenticationProviderFactoryNotFoundException ex) {
                return errorResponse(Response.Status.BAD_REQUEST, 
                                        String.format("Can't instantiate provider, as there's no factory with alias %s", row.getFactoryAlias()));
            } catch (AuthorizationSetupException ex) {
                logger.log(Level.WARNING, "Error instantiating authentication provider: " + ex.getMessage(), ex);
                return errorResponse(Response.Status.BAD_REQUEST, 
                                        String.format("Can't instantiate provider: %s", ex.getMessage()));
            }
            
        } else {
            // disable a provider
            authSvc.deregisterProvider(id);
            return okResponse("Authentication Provider '" + id + "' disabled. " 
                    + ( authSvc.getAuthenticationProviderIds().isEmpty() 
                            ? "WARNING: no enabled authentication providers left." : "") );
        }
    }
    
    @DELETE
    @Path("authenticationProviders/{id}/")
    public Response deleteAuthenticationProvider( @PathParam("id") String id ) {
        authSvc.deregisterProvider(id);
        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        if ( row != null ) {
            em.remove( row );
        }
        
        return okResponse("AuthenticationProvider " + id + " deleted. "
            + ( authSvc.getAuthenticationProviderIds().isEmpty() 
                            ? "WARNING: no enabled authentication providers left." : ""));
    }
    
    @DELETE
    @Path("authenticatedUsers/{identifier}/")
    public Response deleteAuthenticatedUser(@PathParam("identifier") String identifier) {
        AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
        if (user!=null) {
            authSvc.deleteAuthenticatedUser(user.getId());
            return okResponse("AuthenticatedUser " +identifier + " deleted. ");
        }
        return errorResponse(Response.Status.BAD_REQUEST, "User "+ identifier+" not found.");
    }

    @DELETE
    @Path("authenticatedUsers/id/{id}/")
    public Response deleteAuthenticatedUserById(@PathParam("id") Long id) {
        AuthenticatedUser user = authSvc.findByID(id);
        if (user != null) {
            authSvc.deleteAuthenticatedUser(user.getId());
            return okResponse("AuthenticatedUser " + id + " deleted. ");
        }
        return errorResponse(Response.Status.BAD_REQUEST, "User " + id + " not found.");
    }

    @Path("roles")
    @POST
    public Response createNewBuiltinRole(RoleDTO roleDto) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "createBuiltInRole")
                .setInfo(roleDto.getAlias() + ":" + roleDto.getDescription() );
        try {
            return okResponse(json(rolesSvc.save(roleDto.asRole())));
        } catch (Exception e) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo( alr.getInfo() + "// " + e.getMessage() );
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            actionLogSvc.log(alr);
        }
    }
    
    @Path("roles")
    @GET
    public Response listBuiltinRoles() {
        try {
            return okResponse( rolesToJson(rolesSvc.findBuiltinRoles()) );
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
    
    @Path("superuser/{identifier}")
    @POST
    public Response toggleSuperuser(@PathParam("identifier") String identifier) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "toggleSuperuser")
                .setInfo( identifier );
       try {
          AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
          
            user.setSuperuser(!user.isSuperuser());
            
            return okResponse("User " + user.getIdentifier() + " " + (user.isSuperuser() ? "set": "removed") + " as a superuser.");
        } catch (Exception e) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo( alr.getInfo() + "// " + e.getMessage() );
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
           actionLogSvc.log(alr);
       }
    }    

    @Path("validate")
    @GET
    public Response validate() {
        String msg = "UNKNOWN";
        try {
            beanValidationSvc.validateDatasets();
            msg = "valid";
        } catch (Exception ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> constraintViolation : constraintViolationException.getConstraintViolations()) {
                        String databaseRow = constraintViolation.getLeafBean().toString();
                        String field = constraintViolation.getPropertyPath().toString();
                        String invalidValue = constraintViolation.getInvalidValue().toString();
                            JsonObjectBuilder violation = Json.createObjectBuilder();
                            violation.add("entityClassDatabaseTableRowId", databaseRow);
                            violation.add("field", field);
                            violation.add("invalidValue", invalidValue);
                            return okResponse(violation);
                    }
                }
                cause = cause.getCause();
            }
        }
        return okResponse(msg);
    }

}
