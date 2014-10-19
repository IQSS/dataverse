package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Base class for API beans
 * @author michael
 */
public abstract class AbstractApiBean {
	
    /**
     * Utility class to convey a proper error response on failed commands.
     * @see #execCommand(edu.harvard.iq.dataverse.engine.command.Command, java.lang.String) 
     */
    public static class FailedCommandResult extends Exception {
        private final Response response;

        public FailedCommandResult(Response response) {
            this.response = response;
        }

        public Response getResponse() {
            return response;
        }
    }
    
	@EJB
	protected EjbDataverseEngine engineSvc;
	
    @EJB
    protected DatasetServiceBean datasetSvc;
    
	@EJB
	protected DataverseServiceBean dataverseSvc;
    
    @EJB 
    protected AuthenticationServiceBean authSvc;
    
    @EJB
    protected DatasetFieldServiceBean datasetFieldSvc;
	
    @EJB
    protected MetadataBlockServiceBean metadataBlockSvc;
    
    @EJB
    UserServiceBean userSvc;
    
	@EJB
	DataverseRoleServiceBean rolesSvc;
    
    @EJB
    SettingsServiceBean settingsSvc;
    
    @EJB
    RoleAssigneeServiceBean roleAssigneeSvc;
    
	@PersistenceContext(unitName = "VDCNet-ejbPU")
	EntityManager em;
	
    /**
     * For pretty printing (indenting) of JSON output.
     */
    public enum Format {

        PRETTY
    }

    private final LazyRef<JsonParser> jsonParserRef = new LazyRef<>(new Callable<JsonParser>() {
        @Override
        public JsonParser call() throws Exception {
            return new JsonParser(datasetFieldSvc, metadataBlockSvc);
        }
    });
    
	protected RoleAssignee findAssignee( String identifier ) {
    	return roleAssigneeSvc.getRoleAssignee(identifier);
	}
    
    protected AuthenticatedUser findUserByApiToken( String apiKey ) {
        return authSvc.lookupUser(apiKey);
    }
    
	protected Dataverse findDataverse( String idtf ) {
		return isNumeric(idtf) ? dataverseSvc.find(Long.parseLong(idtf))
	 							  : dataverseSvc.findByAlias(idtf);
	}
	
	protected DvObject findDvo( Long id ) {
		return em.createNamedQuery("DvObject.findById", DvObject.class)
				.setParameter("id", id)
				.getSingleResult();
	}
	
	protected DvObject findDvo( String id ) {
        if ( isNumeric(id) ) {
            return em.createNamedQuery("DvObject.findById", DvObject.class)
				.setParameter("id", Long.valueOf(id))
                	.getSingleResult();
        } else {
            Dataverse d = dataverseSvc.findByAlias(id);
            return ( d == null ) ?
                    d : datasetSvc.findByGlobalId(id);
            
        }
	}
	
    protected MetadataBlock findMetadataBlock(String idtf) throws NumberFormatException {
        return isNumeric(idtf) ? metadataBlockSvc.findById(Long.parseLong(idtf))
                : metadataBlockSvc.findByName(idtf);
    }
    
    protected Response okResponse( JsonArrayBuilder bld ) {
        return Response.ok( Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build() ).build();
    }
    
    protected Response okResponse(JsonArrayBuilder bld, Format format) {
        return Response.ok(Util.jsonObject2prettyString(
                Json.createObjectBuilder()
                .add("status", "OK")
                .add("data", bld).build()), MediaType.APPLICATION_JSON_TYPE
        ).build();
    }
    
    protected Response createdResponse( String uri, JsonObjectBuilder bld ) {
        return Response.created( URI.create(uri) )
                .entity( Json.createObjectBuilder()
                .add("status", "OK")
                .add("data", bld).build())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
    protected Response okResponse( JsonObjectBuilder bld ) {
        return Response.ok( Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build() )
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    protected Response okResponse( String msg ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", Json.createObjectBuilder().add("message",msg)).build() )
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    protected <T> T execCommand( Command<T> com, String messageSeed ) throws FailedCommandResult {
        try {
            return engineSvc.submit(com);
            
        } catch (IllegalCommandException ex) {
            throw new FailedCommandResult( errorResponse( Response.Status.FORBIDDEN, messageSeed + ": Not Allowed (" + ex.getMessage() + ")" ));
          
        } catch (PermissionException ex) {
            throw new FailedCommandResult(errorResponse(Response.Status.UNAUTHORIZED, messageSeed + " unauthorized."));
            
        } catch (CommandException ex) {
            Logger.getLogger(AbstractApiBean.class.getName()).log(Level.SEVERE, "Error while " + messageSeed, ex);
            throw new FailedCommandResult(errorResponse(Status.INTERNAL_SERVER_ERROR, messageSeed + " failed: " + ex.getMessage()));
        }
    }
    
    /**
     * Returns an OK response (HTTP 200, status:OK) with the passed value
     * in the data field.
     * @param value the value for the data field
     * @return a HTTP OK response with the passed value as data.
     */
    protected Response okResponseWithValue( String value ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", value).build() ).build();
    }

    protected Response okResponseWithValue( boolean value ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", value).build() ).build();
    }
    
    protected Response accepted() {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                        .add("status", "OK").build()
                ).build();
    }
    
    protected JsonParser jsonParser() {
        return jsonParserRef.get();
    }
    
    protected Response notFound( String msg ) {
        return errorResponse(Status.NOT_FOUND, msg);
    }
    
    protected Response badApiKey( String apiKey ) {
        return errorResponse(Status.UNAUTHORIZED, "Bad api key '" + apiKey +"'");
    }
    
    protected Response permissionError( PermissionException pe ) {
        return errorResponse( Status.UNAUTHORIZED, pe.getMessage() );
    }
    
    protected Response errorResponse( Status sts, String msg ) {
        return Response.status(sts)
                .entity( Json.createObjectBuilder().add("status", "ERROR")
                        .add( "message", msg ).build())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
    
    protected Response execute( Command c ) {
         try { 
            engineSvc.submit( c );
            return accepted();
            
        } catch ( PermissionException pex ) {
            return permissionError( pex );
            
        } catch ( CommandException ce ) {
            return errorResponse(Status.INTERNAL_SERVER_ERROR, ce.getLocalizedMessage());
        }
    }
 
    protected boolean isNumeric( String str ) { return Util.isNumeric(str); };
	protected String error( String msg ) { return Util.error(msg); }
	protected String ok( String msg ) { return Util.ok(msg); }
	protected String ok( JsonObject jo ) { return Util.ok(jo); }
	protected String ok( JsonArray jo ) { return Util.ok(jo); }
	protected String ok( JsonObjectBuilder jo ) { return ok(jo.build()); }
	protected String ok( JsonArrayBuilder jo ) { return ok(jo.build()); }
}

class LazyRef<T> {
    private interface Ref<T> {
        T get();
    }
    
    private Ref<T> ref;
    
    public LazyRef( final Callable<T> initer ) {
        ref = new Ref<T>(){
            @Override
            public T get() {
                try {
                    final T t = initer.call();
                    ref = new Ref<T>(){ @Override public T get() { return t;} };
                    return ref.get();
                } catch (Exception ex) {
                    Logger.getLogger(LazyRef.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
        }};
    }
    
    public T get()  {
        return ref.get();
    }
}