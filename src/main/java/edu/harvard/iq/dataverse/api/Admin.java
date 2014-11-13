package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.settings.Setting;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response.Status;
/**
 * Where the secure, setup API calls live.
 * @author michael
 */
@Stateless
@Path("s")
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
    
    @Path("settings/{name}/{content}")
    @PUT
    public Response addSetting( @PathParam("name") String name, @PathParam("content") String content ) {
        try {
            Setting s = settingsSvc.set(name, URLDecoder.decode(content, "UTF-8"));
            return okResponse( jsonObjectBuilder().add(s.getName(), s.getContent()) );
        } catch (UnsupportedEncodingException ex) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
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
        for ( AuthenticationProviderFactory f :
                authSvc.listProviderFactories() ){
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
            return Response.created( new URI("/s/authenticationProviders/"+managed.getId()))
                    .build();
        } catch ( AuthorizationSetupException | URISyntaxException e ) {
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
    
    @Path("roles")
    @POST
    public Response createNewBuiltinRole(RoleDTO roleDto) {
        try {
            rolesSvc.save(roleDto.asRole());
            return okResponse(json(roleDto.asRole()));
        } catch (Exception e) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    
}
