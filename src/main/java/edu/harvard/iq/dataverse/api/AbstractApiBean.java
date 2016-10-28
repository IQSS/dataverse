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
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.validation.BeanValidationServiceBean;
import java.io.StringReader;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
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
    
    private static final Logger logger = Logger.getLogger(AbstractApiBean.class.getName());
    private static final String DATAVERSE_KEY_HEADER_NAME = "X-Dataverse-key";
    
    /**
     * Utility class to convey a proper error response using Java's exceptions.
     */
    public static class WrappedResponse extends Exception {
        private final Response response;
        
        public WrappedResponse(Response response) {
            this.response = response;
        }
        
        public WrappedResponse( Throwable cause, Response response ) {
            super( cause );
            this.response = response;
        }
        
        public Response getResponse() {
            return response;
        }
        
        /**
         * Creates a new response, based on the original response and the passed message.
         * Typical use would be to add a better error message to the HTTP response.
         * @param message additional message to be added to the response.
         * @return A Response with updated message field.
         */
        public Response refineResponse( String message ) {
            final Status statusCode = Response.Status.fromStatusCode(response.getStatus());
            String baseMessage = getWrappedMessageWhenJson();
            
            if ( baseMessage == null ) {
                final Throwable cause = getCause();
                baseMessage = (cause!=null ? cause.getMessage() : "");
            }
            return error(statusCode, message+" "+baseMessage);
        }
        
        /**
         * In the common case of the wrapped response being of type JSON,
         * return the message field it has (if any).
         * @return the content of a message field, or {@code null}.
         */
        String getWrappedMessageWhenJson() {
            if ( response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) ) {
                Object entity = response.getEntity();
                if ( entity == null ) return null;
                
                String json = entity.toString();
                try ( StringReader rdr = new StringReader(json) ){
                    JsonReader jrdr = Json.createReader(rdr);
                    JsonObject obj = jrdr.readObject();
                    if ( obj.containsKey("message") ) {
                        JsonValue message = obj.get("message");
                        return message.getValueType() == ValueType.STRING ? obj.getString("message") : message.toString();
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
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

    @EJB
    protected PrivateUrlServiceBean privateUrlSvc;

    @EJB
    protected ConfirmEmailServiceBean confirmEmailSvc;

    @EJB
    protected UserNotificationServiceBean userNotificationSvc;

	@PersistenceContext(unitName = "VDCNet-ejbPU")
	protected EntityManager em;
    
    @Context
    protected HttpServletRequest httpRequest;
	
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
    
    /**
     * Functional interface for handling HTTP requests in the APIs.
     * 
     * @see #response(edu.harvard.iq.dataverse.api.AbstractApiBean.DataverseRequestHandler)
     */
    protected static interface DataverseRequestHandler {
        Response handle( DataverseRequest u ) throws WrappedResponse; 
    }
    
    
    /* ===================== *\
     *  Utility Methods      *
     *  Get that DSL feelin' *
    \* ===================== */
    
    protected JsonParser jsonParser() {
        return jsonParserRef.get();
    }
    
    protected boolean isNumeric( String str ) { 
        return Util.isNumeric(str); 
    }
    
    protected boolean parseBooleanOrDie( String input ) throws WrappedResponse {
        if (input == null ) throw new WrappedResponse( badRequest("Boolean value missing"));
        input = input.trim();
        if ( Util.isBoolean(input) ) {
            return Util.isTrue(input);
        } else {
            throw new WrappedResponse( badRequest("Illegal boolean value '" + input + "'"));
        }
    } 
    
     /**
     * Returns the {@code key} query parameter from the current request, or {@code null} if
     * the request has no such parameter.
     * @param key Name of the requested parameter.
     * @return Value of the requested parameter in the current request.
     */
    protected String getRequestParameter( String key ) {
        return httpRequest.getParameter(key);
    }
    
    protected String getRequestApiKey() {
        String headerParamApiKey = httpRequest.getHeader(DATAVERSE_KEY_HEADER_NAME);
        String queryParamApiKey = httpRequest.getParameter("key");
        return headerParamApiKey!=null ? headerParamApiKey : queryParamApiKey;
    }
    
    /* ========= *\
     *  Finders  *
    \* ========= */
    protected RoleAssignee findAssignee(String identifier) {
        try {
            RoleAssignee roleAssignee = roleAssigneeSvc.getRoleAssignee(identifier);
            return roleAssignee;
        } catch (EJBException ex) {
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            logger.info("Exception caught looking up RoleAssignee based on identifier '" + identifier + "': " + cause.getMessage());
            return null;
        }
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
    
    /**
     * Returns the user of pointed by the API key, or the guest user
     * @return a user, may be a guest user.
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse iff there is an api key present, but it is invalid.
     */
    protected User findUserOrDie() throws WrappedResponse {
        final String requestApiKey = getRequestApiKey();
        if (requestApiKey == null) {
            return GuestUser.get();
        }
        PrivateUrlUser privateUrlUser = privateUrlSvc.getPrivateUrlUserFromToken(requestApiKey);
        if (privateUrlUser != null) {
            return privateUrlUser;
        }
        return findAuthenticatedUserOrDie(requestApiKey);
    }
    
    /**
     * Finds the authenticated user, based on (in order):
     * <ol>
     *  <li>The key in the HTTP header {@link #DATAVERSE_KEY_HEADER_NAME}</li>
     *  <li>The key in the query parameter {@code key}
     * </ol>
     * 
     * If no user is found, throws a wrapped bad api key (HTTP UNAUTHORIZED) response.
     * 
     * @return The authenticated user which owns the passed api key
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse in case said user is not found.
     */
    protected AuthenticatedUser findAuthenticatedUserOrDie() throws WrappedResponse {
        return findAuthenticatedUserOrDie(getRequestApiKey());
    }
    
    private AuthenticatedUser findAuthenticatedUserOrDie( String key ) throws WrappedResponse {
        AuthenticatedUser u = authSvc.lookupUser(key);
        if ( u != null ) {
            return u;
        }
        throw new WrappedResponse( badApiKey(key) );
    }
    
    protected Dataverse findDataverseOrDie( String dvIdtf ) throws WrappedResponse {
        Dataverse dv = findDataverse(dvIdtf);
        if ( dv == null ) {
            throw new WrappedResponse(error( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'"));
        }
        return dv;
    }
    
    protected DataverseRequest createDataverseRequest( User u )  {
        return new DataverseRequest(u, httpRequest);
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
            return ( d != null ) ?
                    d : datasetSvc.findByGlobalId(id);
            
        }
	}
	
    protected <T> T failIfNull( T t, String errorMessage ) throws WrappedResponse {
        if ( t != null ) return t;
        throw new WrappedResponse( error( Response.Status.BAD_REQUEST,errorMessage) );
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
    
    /* =================== *\
     *  Command Execution  *
    \* =================== */
    
    /**
     * Executes a command, and returns the appropriate result/HTTP response.
     * @param <T> Return type for the command
     * @param cmd The command to execute.
     * @return Value from the command
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse Unwrap and return.
     * @see #response(java.util.concurrent.Callable) 
     */
    protected <T> T execCommand( Command<T> cmd ) throws WrappedResponse {
        try {
            return engineSvc.submit(cmd);
            
        } catch (IllegalCommandException ex) {
            throw new WrappedResponse( ex, error(Response.Status.FORBIDDEN, ex.getMessage() ) );
          
        } catch (PermissionException ex) {
            /**
             * @todo Is there any harm in exposing ex.getLocalizedMessage()?
             * There's valuable information in there that can help people reason
             * about permissions!
             */
            throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, 
                                                    "User " + cmd.getRequest().getUser().getIdentifier() + " is not permitted to perform requested action.") );
            
        } catch (CommandException ex) {
            Logger.getLogger(AbstractApiBean.class.getName()).log(Level.SEVERE, "Error while executing command " + cmd, ex);
            throw new WrappedResponse(ex, error(Status.INTERNAL_SERVER_ERROR, ex.getMessage()));
        }
    }
    
    /**
     * A syntactically nicer way of using {@link #execCommand(edu.harvard.iq.dataverse.engine.command.Command)}.
     * @param hdl The block to run.
     * @return HTTP Response appropriate for the way {@code hdl} executed.
     */
    protected Response response( Callable<Response> hdl ) {
        try {
            return hdl.call();
        } catch ( WrappedResponse rr ) {
            return rr.getResponse();
        } catch ( Exception ex ) {
            logger.log( Level.WARNING, "Error executing callable: " + ex.getMessage(), ex );
            return error(Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    
    /**
     * The preferred way of handling a request that requires a user. The system
     * looks for the user and, if found, handles it to the handler for doing the
     * actual work.
     * 
     * This is a relatively secure way to handle things, since if the user is not
     * found, the response is about the bad API key, rather than something else
     * (say, 404 NOT FOUND which leaks information about the existence of the 
     * sought object).
     * 
     * @param hdl handling code block.
     * @return HTTP Response appropriate for the way {@code hdl} executed.
     */
    protected Response response( DataverseRequestHandler hdl ) {
        try {
            return hdl.handle(createDataverseRequest(findUserOrDie()));
        } catch ( WrappedResponse rr ) {
            return rr.getResponse();
        } catch ( Exception ex ) {
            logger.log( Level.WARNING, "Error executing callable: " + ex.getMessage(), ex );
            return error(Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    
    /* ====================== *\
     *  HTTP Response methods *
    \* ====================== */
    
    protected Response ok( JsonArrayBuilder bld ) {
        return Response.ok(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build()).build();
    }
    
    protected Response ok( JsonObjectBuilder bld ) {
        return Response.ok( Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", bld).build() )
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    protected Response ok( String msg ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", Json.createObjectBuilder().add("message",msg)).build() )
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    protected Response ok( boolean value ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", "OK")
            .add("data", value).build() ).build();
    }
    
    protected Response created( String uri, JsonObjectBuilder bld ) {
        return Response.created( URI.create(uri) )
                .entity( Json.createObjectBuilder()
                .add("status", "OK")
                .add("data", bld).build())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
    protected Response accepted() {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                        .add("status", "OK").build()
                ).build();
    }
    
    protected Response notFound( String msg ) {
        return error(Status.NOT_FOUND, msg);
    }
    
    protected Response badRequest( String msg ) {
        return error( Status.BAD_REQUEST, msg );
    }
    
    protected Response badApiKey( String apiKey ) {
        return error(Status.UNAUTHORIZED, (apiKey != null ) ? "Bad api key '" + apiKey +"'" : "Please provide a key query parameter (?key=XXX) or via the HTTP header " + DATAVERSE_KEY_HEADER_NAME );
    }
    
    protected Response permissionError( PermissionException pe ) {
        return permissionError( pe.getMessage() );
    }

    protected Response permissionError( String message ) {
        return error( Status.UNAUTHORIZED, message );
    }
    
    protected static Response error( Status sts, String msg ) {
        return Response.status(sts)
                .entity( NullSafeJsonBuilder.jsonObjectBuilder()
                        .add("status", "ERROR")
                        .add( "message", msg ).build()
                ).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
  
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
