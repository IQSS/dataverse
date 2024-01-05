package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.metrics.MetricsServiceBean;
import edu.harvard.iq.dataverse.locality.StorageSiteServiceBean;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * Base class for API beans
 * @author michael
 */
public abstract class AbstractApiBean {

    private static final Logger logger = Logger.getLogger(AbstractApiBean.class.getName());
    private static final String DATAVERSE_KEY_HEADER_NAME = "X-Dataverse-key";
    private static final String PERSISTENT_ID_KEY=":persistentId";
    private static final String ALIAS_KEY=":alias";
    public static final String STATUS_WF_IN_PROGRESS = "WORKFLOW_IN_PROGRESS";
    public static final String DATAVERSE_WORKFLOW_INVOCATION_HEADER_NAME = "X-Dataverse-invocationID";
    public static final String RESPONSE_MESSAGE_AUTHENTICATED_USER_REQUIRED = "Only authenticated users can perform the requested operation";

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
    protected DvObjectServiceBean dvObjectSvc;
    
    @EJB
    protected DatasetServiceBean datasetSvc;
    
    @EJB
    protected DataFileServiceBean fileService;

    @EJB
    protected DataverseServiceBean dataverseSvc;

    @EJB
    protected AuthenticationServiceBean authSvc;

    @EJB
    protected DatasetFieldServiceBean datasetFieldSvc;

    @EJB
    protected MetadataBlockServiceBean metadataBlockSvc;

    @EJB
    protected LicenseServiceBean licenseSvc;

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
    protected SavedSearchServiceBean savedSearchSvc;

    @EJB
    protected ConfirmEmailServiceBean confirmEmailSvc;

    @EJB
    protected UserNotificationServiceBean userNotificationSvc;

    @EJB
    protected DatasetVersionServiceBean datasetVersionSvc;

    @EJB
    protected SystemConfig systemConfig;

    @EJB
    protected DataCaptureModuleServiceBean dataCaptureModuleSvc;
    
    @EJB
    protected DatasetLinkingServiceBean dsLinkingService;
    
    @EJB
    protected DataverseLinkingServiceBean dvLinkingService;

    @EJB
    protected PasswordValidatorServiceBean passwordValidatorService;

    @EJB
    protected ExternalToolServiceBean externalToolService;

    @EJB
    DataFileServiceBean fileSvc;

    @EJB
    StorageSiteServiceBean storageSiteSvc;

    @EJB
    MetricsServiceBean metricsSvc;
    
    @EJB 
    DvObjectServiceBean dvObjSvc;
    
    @EJB 
    GuestbookResponseServiceBean gbRespSvc;

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
            return new JsonParser(datasetFieldSvc, metadataBlockSvc,settingsSvc, licenseSvc);
        }
    });

    /**
     * Functional interface for handling HTTP requests in the APIs.
     *
     * @see #response(edu.harvard.iq.dataverse.api.AbstractApiBean.DataverseRequestHandler, edu.harvard.iq.dataverse.authorization.users.User)
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

    protected User getRequestUser(ContainerRequestContext crc) {
        return (User) crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER);
    }

    /**
     * Gets the authenticated user from the ContainerRequestContext user property. If the user from the property
     * is not authenticated, throws a wrapped "authenticated user required" user (HTTP UNAUTHORIZED) response.
     * @param crc a ContainerRequestContext implementation
     * @return The authenticated user
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse in case the user is not authenticated.
     *
     * TODO:
     *  This method is designed to comply with existing authorization logic, based on the old findAuthenticatedUserOrDie method.
     *  Ideally, as for authentication, a filter could be implemented for authorization, which would extract and encapsulate the
     *  authorization logic from the AbstractApiBean.
     */
    protected AuthenticatedUser getRequestAuthenticatedUserOrDie(ContainerRequestContext crc) throws WrappedResponse {
        User requestUser = (User) crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER);
        if (requestUser.isAuthenticated()) {
            return (AuthenticatedUser) requestUser;
        } else {
            throw new WrappedResponse(authenticatedUserRequired());
        }
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
            logger.log(Level.INFO, "Exception caught looking up RoleAssignee based on identifier ''{0}'': {1}", new Object[]{identifier, cause.getMessage()});
            return null;
        }
    }

    /**
     * @param apiKey the key to find the user with
     * @return the user, or null
     */
    protected AuthenticatedUser findUserByApiToken( String apiKey ) {
        return authSvc.lookupUser(apiKey);
    }

    protected Dataverse findDataverseOrDie( String dvIdtf ) throws WrappedResponse {
        Dataverse dv = findDataverse(dvIdtf);
        if ( dv == null ) {
            throw new WrappedResponse(error( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'"));
        }
        return dv;
    }
    
    protected DataverseLinkingDataverse findDataverseLinkingDataverseOrDie(String dataverseId, String linkedDataverseId) throws WrappedResponse {
        DataverseLinkingDataverse dvld;
        Dataverse dataverse = findDataverseOrDie(dataverseId);
        Dataverse linkedDataverse = findDataverseOrDie(linkedDataverseId);
        try {
            dvld = dvLinkingService.findDataverseLinkingDataverse(dataverse.getId(), linkedDataverse.getId());
            if (dvld == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataverselinking.error.not.found.ids", Arrays.asList(dataverseId, linkedDataverseId))));
            }
            return dvld;
        } catch (NumberFormatException nfe) {
            throw new WrappedResponse(
                    badRequest(BundleUtil.getStringFromBundle("find.dataverselinking.error.not.found.bad.ids", Arrays.asList(dataverseId, linkedDataverseId))));
        }
    }

    protected Dataset findDatasetOrDie(String id) throws WrappedResponse {
        Dataset dataset;
        if (id.equals(PERSISTENT_ID_KEY)) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            if (persistentId == null) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", Collections.singletonList(PERSISTENT_ID_KEY.substring(1)))));
            }
            dataset = datasetSvc.findByGlobalId(persistentId);
            if (dataset == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.persistentId", Collections.singletonList(persistentId))));
            }
            return dataset;

        } else {
            try {
                dataset = datasetSvc.find(Long.parseLong(id));
                if (dataset == null) {
                    throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.id", Collections.singletonList(id))));
                }
                return dataset;
            } catch (NumberFormatException nfe) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.bad.id", Collections.singletonList(id))));
            }
        }
    }
    
    protected DataFile findDataFileOrDie(String id) throws WrappedResponse {
        
        DataFile datafile;
        if (id.equals(PERSISTENT_ID_KEY)) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            if (persistentId == null) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", Collections.singletonList(PERSISTENT_ID_KEY.substring(1)))));
            }
            datafile = fileService.findByGlobalId(persistentId);
            if (datafile == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.datafile.error.dataset.not.found.persistentId", Collections.singletonList(persistentId))));
            }
            return datafile;
        } else {
            try {
                datafile = fileService.find(Long.parseLong(id));
                if (datafile == null) {
                    throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.datafile.error.datafile.not.found.id", Collections.singletonList(id))));
                }
                return datafile;
            } catch (NumberFormatException nfe) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.datafile.error.datafile.not.found.bad.id", Collections.singletonList(id))));
            }
        }
    }
       
    protected DataverseRole findRoleOrDie(String id) throws WrappedResponse {
        DataverseRole role;
        if (id.equals(ALIAS_KEY)) {
            String alias = getRequestParameter(ALIAS_KEY.substring(1));
            try {
                return em.createNamedQuery("DataverseRole.findDataverseRoleByAlias", DataverseRole.class)
                        .setParameter("alias", alias)
                        .getSingleResult();

            //Should not be a multiple result exception due to table constraint
            } catch (NoResultException nre) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataverse.role.error.role.not.found.alias", Collections.singletonList(alias))));
            }

        } else {

            try {
                role = rolesSvc.find(Long.parseLong(id));
                if (role == null) {
                    throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataverse.role.error.role.not.found.id", Collections.singletonList(id))));
                } else {
                    return role;
                }

            } catch (NumberFormatException nfe) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataverse.role.error.role.not.found.bad.id", Collections.singletonList(id))));
            }
        }
    }
    
    protected DatasetLinkingDataverse findDatasetLinkingDataverseOrDie(String datasetId, String linkingDataverseId) throws WrappedResponse {
        DatasetLinkingDataverse dsld;
        Dataverse linkingDataverse = findDataverseOrDie(linkingDataverseId);

        if (datasetId.equals(PERSISTENT_ID_KEY)) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            if (persistentId == null) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", Collections.singletonList(PERSISTENT_ID_KEY.substring(1)))));
            }
            
            Dataset dataset = datasetSvc.findByGlobalId(persistentId);
            if (dataset == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.persistentId", Collections.singletonList(persistentId))));
            }
            datasetId = dataset.getId().toString();
        } 
        try {
            dsld = dsLinkingService.findDatasetLinkingDataverse(Long.parseLong(datasetId), linkingDataverse.getId());
            if (dsld == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.datasetlinking.error.not.found.ids", Arrays.asList(datasetId, linkingDataverse.getId().toString()))));
            }
            return dsld;
        } catch (NumberFormatException nfe) {
            throw new WrappedResponse(
                    badRequest(BundleUtil.getStringFromBundle("find.datasetlinking.error.not.found.bad.ids", Arrays.asList(datasetId, linkingDataverse.getId().toString()))));
        }
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
            //for 8859 for api calls that try to update datasets with TOA out of compliance
                if (ex.getMessage().toLowerCase().contains("terms of use")){
                    throw new WrappedResponse(ex, conflict(ex.getMessage()));
                }
            throw new WrappedResponse( ex, forbidden(ex.getMessage() ) );
        } catch (PermissionException ex) {
            /**
             * TODO Is there any harm in exposing ex.getLocalizedMessage()?
             * There's valuable information in there that can help people reason
             * about permissions! The formatting of the error would need to be
             * cleaned up but here's an example the helpful information:
             *
             * "User :guest is not permitted to perform requested action.Can't
             * execute command
             * edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand@50b150d9,
             * because request [DataverseRequest user:[GuestUser
             * :guest]@127.0.0.1] is missing permissions [AddDataset,
             * PublishDataset] on Object mra"
             *
             * Right now, the error that's visible via API (and via GUI
             * sometimes?) doesn't have much information in it:
             *
             * "User @jsmith is not permitted to perform requested action."
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
            return handleDataverseRequestHandlerException(ex);
        }
    }

    /***
     * The preferred way of handling a request that requires a user. The method
     * receives a user and handles it to the handler for doing the actual work.
     *
     * @param hdl handling code block.
     * @param user the associated request user.
     * @return HTTP Response appropriate for the way {@code hdl} executed.
     */
    protected Response response(DataverseRequestHandler hdl, User user) {
        try {
            return hdl.handle(createDataverseRequest(user));
        } catch ( WrappedResponse rr ) {
            return rr.getResponse();
        } catch ( Exception ex ) {
            return handleDataverseRequestHandlerException(ex);
        }
    }

    private Response handleDataverseRequestHandlerException(Exception ex) {
        String incidentId = UUID.randomUUID().toString();
        logger.log(Level.SEVERE, "API internal error " + incidentId +": " + ex.getMessage(), ex);
        return Response.status(500)
                .entity(Json.createObjectBuilder()
                        .add("status", "ERROR")
                        .add("code", 500)
                        .add("message", "Internal server error. More details available at the server logs.")
                        .add("incidentId", incidentId)
                        .build())
                .type("application/json").build();
    }

    /* ====================== *\
     *  HTTP Response methods *
    \* ====================== */

    protected Response ok( JsonArrayBuilder bld ) {
        return Response.ok(Json.createObjectBuilder()
            .add("status", ApiConstants.STATUS_OK)
            .add("data", bld).build())
            .type(MediaType.APPLICATION_JSON).build();
    }
    
    protected Response ok( JsonArray ja ) {
        return Response.ok(Json.createObjectBuilder()
            .add("status", ApiConstants.STATUS_OK)
            .add("data", ja).build())
            .type(MediaType.APPLICATION_JSON).build();
    }

    protected Response ok( JsonObjectBuilder bld ) {
        return Response.ok( Json.createObjectBuilder()
            .add("status", ApiConstants.STATUS_OK)
            .add("data", bld).build() )
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    protected Response ok( JsonObject jo ) {
        return Response.ok( Json.createObjectBuilder()
                .add("status", ApiConstants.STATUS_OK)
                .add("data", jo).build() )
                .type(MediaType.APPLICATION_JSON)
                .build();    
    }

    protected Response ok( String msg ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", ApiConstants.STATUS_OK)
            .add("data", Json.createObjectBuilder().add("message",msg)).build() )
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    protected Response ok( String msg, JsonObjectBuilder bld  ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", ApiConstants.STATUS_OK)
            .add("message", Json.createObjectBuilder().add("message",msg))     
            .add("data", bld).build())      
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    protected Response ok( boolean value ) {
        return Response.ok().entity(Json.createObjectBuilder()
            .add("status", ApiConstants.STATUS_OK)
            .add("data", value).build() ).build();
    }

    /**
     * @param data Payload to return.
     * @param mediaType Non-JSON media type.
     * @param downloadFilename - add Content-Disposition header to suggest filename if not null
     * @return Non-JSON response, such as a shell script.
     */
    protected Response ok(String data, MediaType mediaType, String downloadFilename) {
        ResponseBuilder res =Response.ok().entity(data).type(mediaType);
        if(downloadFilename != null) {
            res = res.header("Content-Disposition", "attachment; filename=" + downloadFilename);
        }
        return res.build();
    }

    protected Response created( String uri, JsonObjectBuilder bld ) {
        return Response.created( URI.create(uri) )
                .entity( Json.createObjectBuilder()
                .add("status", "OK")
                .add("data", bld).build())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
    protected Response accepted(JsonObjectBuilder bld) {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                        .add("status", STATUS_WF_IN_PROGRESS)
                        .add("data",bld).build()
                ).build();
    }
    
    protected Response accepted() {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                        .add("status", STATUS_WF_IN_PROGRESS).build()
                ).build();
    }

    protected Response notFound( String msg ) {
        return error(Status.NOT_FOUND, msg);
    }

    protected Response badRequest( String msg ) {
        return error( Status.BAD_REQUEST, msg );
    }
    
    protected Response forbidden( String msg ) {
        return error( Status.FORBIDDEN, msg );
    }
    
    protected Response conflict( String msg ) {
        return error( Status.CONFLICT, msg );
    }

    protected Response authenticatedUserRequired() {
        return error(Status.UNAUTHORIZED, RESPONSE_MESSAGE_AUTHENTICATED_USER_REQUIRED);
    }

    protected Response permissionError( PermissionException pe ) {
        return permissionError( pe.getMessage() );
    }

    protected Response permissionError( String message ) {
        return unauthorized( message );
    }
    
    protected Response unauthorized( String message ) {
        return error( Status.UNAUTHORIZED, message );
    }

    protected static Response error( Status sts, String msg ) {
        return Response.status(sts)
                .entity( NullSafeJsonBuilder.jsonObjectBuilder()
                        .add("status", ApiConstants.STATUS_ERROR)
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
        ref = () -> {
            try {
                final T t = initer.call();
                ref = () -> t;
                return ref.get();
            } catch (Exception ex) {
                Logger.getLogger(LazyRef.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        };
    }

    public T get()  {
        return ref.get();
    }
}
