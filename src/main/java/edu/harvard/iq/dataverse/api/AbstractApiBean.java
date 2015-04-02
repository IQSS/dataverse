package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.UserRequestMetadata;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.validation.BeanValidationServiceBean;
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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Base class for API beans
 * @author michael
 */
public abstract class AbstractApiBean {
	
    /**
     * Utility class to convey a proper error response using Java's exceptions.
     */
    public static class WrappedResponse extends Exception {
        private final Response response;

        public WrappedResponse(Response response) {
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
    protected UserServiceBean userSvc;
    
	@EJB
	protected DataverseRoleServiceBean rolesSvc;
    
    @EJB
    protected SettingsServiceBean settingsSvc;
    
    @EJB
    protected RoleAssigneeServiceBean roleAssigneeSvc;
    
    @EJB
    protected PermissionServiceBean permissionSvc;
    
    @EJB
    protected GroupServiceBean groupSvc;
    
    @EJB
    protected ActionLogServiceBean actionLogSvc;

    @EJB
    protected BeanValidationServiceBean beanValidationSvc;

    @EJB
    protected SavedSearchServiceBean savedSearchSvc;

	@PersistenceContext(unitName = "VDCNet-ejbPU")
	protected EntityManager em;
    
    @Context
    HttpServletRequest request;
	
    /**
     * For pretty printing (indenting) of JSON output.
     */
    public enum Format {

        PRETTY
    }

    private final LazyRef<JsonParser> jsonParserRef = new LazyRef<>(new Callable<JsonParser>() {
        @Override
        public JsonParser call() throws Exception {
            return new JsonParser(datasetFieldSvc, metadataBlockSvc,settingsSvc);
        }
    });
    
	protected RoleAssignee findAssignee( String identifier ) {
    	return roleAssigneeSvc.getRoleAssignee(identifier);
	}
    
    /**
     * 
     * @param apiKey the key to find the user with
     * @return the user, or null
     * @see #findUserOrDie(java.lang.String) 
     */
    protected AuthenticatedUser findUserByApiToken( String apiKey ) {
        return authSvc.lookupUser(apiKey);
    }
    
    protected AuthenticatedUser findUserOrDie( String apiKey ) throws WrappedResponse {
        AuthenticatedUser u = authSvc.lookupUser(apiKey);
        if ( u != null ) {
            u.setRequestMetadata( new UserRequestMetadata(request) );
            return u;
        }
        throw new WrappedResponse( badApiKey(apiKey) );
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
	
    /**
     * Tries to find a DvObject. If the passed id can be interpreted as a number,
     * it tries to get the DvObject by its id. Else, it tries to get a {@link Dataverse}
     * with that alias. If that fails, tries to get a {@link Dataset} with that global id.
     * @param id a value identifying the DvObject, either numeric of textual.
     * @return A DvObject, or {@code null}
     */
	protected DvObject findDvo( String id ) {
        if ( isNumeric(id) ) {
            return findDvo( Long.valueOf(id)) ;
        } else {
            Dataverse d = dataverseSvc.findByAlias(id);
            return ( d == null ) ?
                    d : datasetSvc.findByGlobalId(id);
            
        }
	}
	
    protected <T> T failIfNull( T t, String errorMessage ) throws WrappedResponse {
        if ( t != null ) return t;
        throw new WrappedResponse( errorResponse( Response.Status.BAD_REQUEST,errorMessage) );
    }
    
    protected MetadataBlock findMetadataBlock(Long id)  {
        return metadataBlockSvc.findById(id);
    }
    protected MetadataBlock findMetadataBlock(String idtf) throws NumberFormatException {
        return metadataBlockSvc.findByName(idtf);
    }
    
    protected DatasetFieldType findDatasetFieldType(String idtf) throws NumberFormatException {
        return isNumeric(idtf) ? datasetFieldSvc.find(Long.parseLong(idtf))
                : datasetFieldSvc.findByNameOpt(idtf);
    }    
    
    protected <T> T execCommand( Command<T> com, String messageSeed ) throws WrappedResponse {
        try {
            return engineSvc.submit(com);
            
        } catch (IllegalCommandException ex) {
            throw new WrappedResponse( errorResponse( Response.Status.BAD_REQUEST, messageSeed + ": Bad Request (" + ex.getMessage() + ")" ));
          
        } catch (PermissionException ex) {
            throw new WrappedResponse(errorResponse(Response.Status.UNAUTHORIZED, messageSeed + " unauthorized."));
            
        } catch (CommandException ex) {
            Logger.getLogger(AbstractApiBean.class.getName()).log(Level.SEVERE, "Error while " + messageSeed, ex);
            throw new WrappedResponse(errorResponse(Status.INTERNAL_SERVER_ERROR, messageSeed + " failed: " + ex.getMessage()));
        }
    }
    
    protected Response okResponse( JsonArrayBuilder bld ) {
        return Response.ok(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build()).build();
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
    
    /**
     * Returns an OK response (HTTP 200, status:OK) with the passed value
     * in the data field.
     * @param value the value for the data field
     * @return a HTTP OK response with the passed value as data.
     */
    protected Response okResponseWithValue( String value ) {
        return Response.ok(Util.jsonObject2prettyString(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", value).build()), MediaType.APPLICATION_JSON_TYPE ).build();
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
    
    protected Response badRequest( String msg ) {
        return errorResponse( Status.BAD_REQUEST, msg );
    }
    
    protected Response badApiKey( String apiKey ) {
        return errorResponse(Status.UNAUTHORIZED, (apiKey != null ) ? "Bad api key '" + apiKey +"'" : "Please provide a key query parameter (?key=XXX)");
    }
    
    protected Response permissionError( PermissionException pe ) {
        return errorResponse( Status.UNAUTHORIZED, pe.getMessage() );
    }
    
    protected Response errorResponse( Status sts, String msg ) {
        return Response.status(sts)
                .entity( Json.createObjectBuilder().add("status", "ERROR")
                        .add( "message", (msg!=null) ? msg : "<message was null>" ).build())
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