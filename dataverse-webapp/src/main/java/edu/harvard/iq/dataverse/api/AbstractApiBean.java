package edu.harvard.iq.dataverse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlockDao;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.dto.ApiResponseDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.dataverse.DataverseLinkingService;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DatasetLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import io.vavr.control.Try;
import org.apache.commons.lang.SerializationException;

import javax.ejb.EJB;
import javax.inject.Inject;
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
import java.io.StringReader;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isNumeric;

/**
 * Base class for API beans
 *
 * @author michael
 */
public abstract class AbstractApiBean {

    @EJB
    protected EjbDataverseEngine engineSvc;

    @EJB
    protected DatasetDao datasetSvc;

    @EJB
    protected DataFileServiceBean fileService;

    @EJB
    protected DataverseDao dataverseSvc;

    @EJB
    protected AuthenticationServiceBean authSvc;

    @EJB
    protected DatasetFieldServiceBean datasetFieldSvc;

    @EJB
    protected MetadataBlockDao metadataBlockSvc;

    @EJB
    protected UserServiceBean userSvc;

    @Inject
    protected SettingsServiceBean settingsSvc;

    @EJB
    protected PrivateUrlServiceBean privateUrlSvc;

    @EJB
    protected SystemConfig systemConfig;

    @EJB
    protected DatasetLinkingServiceBean dsLinkingService;

    @EJB
    protected DataverseLinkingService dvLinkingService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    @Context
    protected HttpServletRequest httpRequest;

    private static final Logger logger = Logger.getLogger(AbstractApiBean.class.getName());
    private static final String DATAVERSE_KEY_HEADER_NAME = "X-Dataverse-key";
    private static final String PERSISTENT_ID_KEY = ":persistentId";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_WF_IN_PROGRESS = "WORKFLOW_IN_PROGRESS";

    /**
     * Utility class to convey a proper error response using Java's exceptions.
     */
    public static class WrappedResponse extends Exception {
        private final Response response;

        public WrappedResponse(Response response) {
            this.response = response;
        }

        public WrappedResponse(Throwable cause, Response response) {
            super(cause);
            this.response = response;
        }

        public Response getResponse() {
            return response;
        }

        /**
         * Creates a new response, based on the original response and the passed message.
         * Typical use would be to add a better error message to the HTTP response.
         *
         * @param message additional message to be added to the response.
         * @return A Response with updated message field.
         */
        public Response refineResponse(String message) {
            final Status statusCode = Response.Status.fromStatusCode(response.getStatus());
            String baseMessage = getWrappedMessageWhenJson();

            if (baseMessage == null) {
                final Throwable cause = getCause();
                baseMessage = (cause != null ? cause.getMessage() : "");
            }
            return error(statusCode, message + " " + baseMessage);
        }

        /**
         * In the common case of the wrapped response being of type JSON,
         * return the message field it has (if any).
         *
         * @return the content of a message field, or {@code null}.
         */
        String getWrappedMessageWhenJson() {
            if (response.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                Object entity = response.getEntity();
                if (entity == null) {
                    return null;
                }

                String json = entity.toString();
                try (StringReader rdr = new StringReader(json)) {
                    JsonReader jrdr = Json.createReader(rdr);
                    JsonObject obj = jrdr.readObject();
                    if (obj.containsKey("message")) {
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


    private final LazyRef<JsonParser> jsonParserRef = new LazyRef<>(new Callable<JsonParser>() {
        @Override
        public JsonParser call() throws Exception {
            return new JsonParser(datasetFieldSvc, metadataBlockSvc, settingsSvc);
        }
    });

    /**
     * Functional interface for handling HTTP requests in the APIs.
     *
     * @see #response(edu.harvard.iq.dataverse.api.AbstractApiBean.DataverseRequestHandler)
     */
    protected interface DataverseRequestHandler {
        Response handle(DataverseRequest u) throws WrappedResponse;
    }


    /* ===================== *\
     *  Utility Methods      *
     *  Get that DSL feelin' *
    \* ===================== */

    protected JsonParser jsonParser() {
        return jsonParserRef.get();
    }

    protected boolean parseBooleanOrDie(String input) throws WrappedResponse {
        if (input == null) {
            throw new WrappedResponse(badRequest("Boolean value missing"));
        }
        input = input.trim();
        if (Util.isBoolean(input)) {
            return Util.isTrue(input);
        } else {
            throw new WrappedResponse(badRequest("Illegal boolean value '" + input + "'"));
        }
    }

    /**
     * Returns the {@code key} query parameter from the current request, or {@code null} if
     * the request has no such parameter.
     *
     * @param key Name of the requested parameter.
     * @return Value of the requested parameter in the current request.
     */
    protected String getRequestParameter(String key) {
        return httpRequest.getParameter(key);
    }

    protected String getRequestApiKey() {
        String headerParamApiKey = httpRequest.getHeader(DATAVERSE_KEY_HEADER_NAME);
        String queryParamApiKey = httpRequest.getParameter("key");

        return headerParamApiKey != null ? headerParamApiKey : queryParamApiKey;
    }

    /**
     * @param apiKey the key to find the user with
     * @return the user, or null
     * @see #findUserOrDie(java.lang.String)
     */
    protected AuthenticatedUser findUserByApiToken(String apiKey) {
        return authSvc.lookupUser(apiKey);
    }

    /**
     * Returns the user of pointed by the API key, or the guest user
     *
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
     * <li>The key in the HTTP header {@link #DATAVERSE_KEY_HEADER_NAME}</li>
     * <li>The key in the query parameter {@code key}
     * </ol>
     * <p>
     * If no user is found, throws a wrapped bad api key (HTTP UNAUTHORIZED) response.
     *
     * @return The authenticated user which owns the passed api key
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse in case said user is not found.
     */
    protected AuthenticatedUser findAuthenticatedUserOrDie() throws WrappedResponse {
        return findAuthenticatedUserOrDie(getRequestApiKey());
    }

    protected AuthenticatedUser findSuperuserOrDie() throws WrappedResponse {
        AuthenticatedUser user = findAuthenticatedUserOrDie();
        if (!user.isSuperuser()) {
            throw new WrappedResponse(forbidden("This API call can be used by superusers only"));
        }
        return user;
    }

    private AuthenticatedUser findAuthenticatedUserOrDie(String key) throws WrappedResponse {
        AuthenticatedUser authUser = authSvc.lookupUser(key);
        if (authUser != null) {
            if (!systemConfig.isReadonlyMode()) {
                authUser = userSvc.updateLastApiUseTime(authUser);
            }

            return authUser;
        }
        throw new WrappedResponse(badApiKey(key));
    }

    protected Dataverse findDataverseOrDie(String dvIdtf) throws WrappedResponse {
        Dataverse dv = isNumeric(dvIdtf) ? dataverseSvc.find(Long.parseLong(dvIdtf))
                : dataverseSvc.findByAlias(dvIdtf);
        if (dv == null) {
            throw new WrappedResponse(error(Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'"));
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
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataverselinking.error.not.found.ids", dataverseId, linkedDataverseId)));
            }
            return dvld;
        } catch (NumberFormatException nfe) {
            throw new WrappedResponse(
                    badRequest(BundleUtil.getStringFromBundle("find.dataverselinking.error.not.found.bad.ids", dataverseId, linkedDataverseId)));
        }
    }

    protected Dataset findDatasetOrDie(String id) throws WrappedResponse {
        Dataset dataset;
        if (id.equals(PERSISTENT_ID_KEY)) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            if (persistentId == null) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", PERSISTENT_ID_KEY.substring(1))));
            }
            dataset = datasetSvc.findByGlobalId(persistentId);
            if (dataset == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.persistentId", persistentId)));
            }
            return dataset;

        } else {
            try {
                dataset = datasetSvc.find(Long.parseLong(id));
                if (dataset == null) {
                    throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.id", id)));
                }
                return dataset;
            } catch (NumberFormatException nfe) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.bad.id", id)));
            }
        }
    }

    protected DataFile findDataFileOrDie(String id) throws WrappedResponse {
        DataFile datafile;
        if (id.equals(PERSISTENT_ID_KEY)) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            if (persistentId == null) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", PERSISTENT_ID_KEY.substring(1))));
            }
            datafile = fileService.findByGlobalId(persistentId);
            if (datafile == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.datafile.error.dataset.not.found.persistentId", persistentId)));
            }
            return datafile;
        } else {
            try {
                datafile = fileService.find(Long.parseLong(id));
                if (datafile == null) {
                    throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.datafile.error.datafile.not.found.id", id)));
                }
                return datafile;
            } catch (NumberFormatException nfe) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.datafile.error.datafile.not.found.bad.id", id)));
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
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", PERSISTENT_ID_KEY.substring(1))));
            }

            Dataset dataset = datasetSvc.findByGlobalId(persistentId);
            if (dataset == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.persistentId", persistentId)));
            }
            datasetId = dataset.getId().toString();
        }
        try {
            dsld = dsLinkingService.findDatasetLinkingDataverse(Long.parseLong(datasetId), linkingDataverse.getId());
            if (dsld == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.datasetlinking.error.not.found.ids", datasetId, linkingDataverse.getId().toString())));
            }
            return dsld;
        } catch (NumberFormatException nfe) {
            throw new WrappedResponse(
                    badRequest(BundleUtil.getStringFromBundle("find.datasetlinking.error.not.found.bad.ids", datasetId, linkingDataverse.getId().toString())));
        }
    }

    protected DataverseRequest createDataverseRequest(User u) {
        return new DataverseRequest(u, httpRequest);
    }

    /* =================== *\
     *  Command Execution  *
    \* =================== */

    /**
     * Executes a command, and returns the appropriate result/HTTP response.
     *
     * @param <T> Return type for the command
     * @param cmd The command to execute.
     * @return Value from the command
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse Unwrap and return.
     * @see #response(java.util.concurrent.Callable)
     */
    protected <T> T execCommand(Command<T> cmd) throws WrappedResponse {
        try {
            return engineSvc.submit(cmd);

        } catch (IllegalCommandException ex) {
            throw new WrappedResponse(ex, forbidden(ex.getMessage()));
        } catch (PermissionException ex) {
            /**
             * @todo Is there any harm in exposing ex.getLocalizedMessage()?
             * There's valuable information in there that can help people reason
             * about permissions!
             */
            String message = "User " + cmd.getRequest().getUser().getIdentifier() + " is not permitted to perform requested action.";
            if (systemConfig.isUnconfirmedMailRestrictionModeEnabled()) {
                message += " Alternatively user has not confirmed e-mail yet.";
            }
            throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, message));

        } catch (CommandException ex) {
            Logger.getLogger(AbstractApiBean.class.getName()).log(Level.SEVERE, "Error while executing command " + cmd, ex);
            throw new WrappedResponse(ex, error(Status.INTERNAL_SERVER_ERROR, ex.getMessage()));
        }
    }

    /**
     * A syntactically nicer way of using {@link #execCommand(edu.harvard.iq.dataverse.engine.command.Command)}.
     *
     * @param hdl The block to run.
     * @return HTTP Response appropriate for the way {@code hdl} executed.
     */
    protected Response response(Callable<Response> hdl) {
        try {
            return hdl.call();
        } catch (WrappedResponse rr) {
            return rr.getResponse();
        } catch (Exception ex) {
            String incidentId = UUID.randomUUID().toString();
            logger.log(Level.SEVERE, "API internal error " + incidentId + ": " + ex.getMessage(), ex);
            return Response.status(500)
                    .entity(Json.createObjectBuilder()
                                    .add("status", "ERROR")
                                    .add("code", 500)
                                    .add("message", "Internal server error. More details available at the server logs.")
                                    .add("incidentId", incidentId)
                                    .build())
                    .type("application/json").build();
        }
    }

    /**
     * The preferred way of handling a request that requires a user. The system
     * looks for the user and, if found, handles it to the handler for doing the
     * actual work.
     * <p>
     * This is a relatively secure way to handle things, since if the user is not
     * found, the response is about the bad API key, rather than something else
     * (say, 404 NOT FOUND which leaks information about the existence of the
     * sought object).
     *
     * @param hdl handling code block.
     * @return HTTP Response appropriate for the way {@code hdl} executed.
     */
    protected Response response(DataverseRequestHandler hdl) {
        try {
            return hdl.handle(createDataverseRequest(findUserOrDie()));
        } catch (WrappedResponse rr) {
            return rr.getResponse();
        } catch (Exception ex) {
            String incidentId = UUID.randomUUID().toString();
            logger.log(Level.SEVERE, "API internal error " + incidentId + ": " + ex.getMessage(), ex);
            return Response.status(500)
                    .entity(Json.createObjectBuilder()
                                    .add("status", "ERROR")
                                    .add("code", 500)
                                    .add("message", "Internal server error. More details available at the server logs.")
                                    .add("incidentId", incidentId)
                                    .build())
                    .type("application/json").build();
        }
    }

    /* ====================== *\
     *  HTTP Response methods *
    \* ====================== */

    protected Response ok(JsonValue value) {
        return Response.ok(Json.createObjectBuilder()
                .add("status", STATUS_OK)
                .add("data", value).build())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    protected Response ok(JsonObjectBuilder bld) {
        return ok(bld.build());
    }

    protected Response ok(JsonArrayBuilder bld) {
        return ok(bld.build());
    }

    protected Response ok(String msg) {
        return ok(Json.createObjectBuilder().add("message", msg));
    }

    protected <T> Response ok(T objectToBeSerialized) {
        ObjectMapper objectMapper = new ObjectMapper();

        ApiResponseDTO<T> response = new ApiResponseDTO<>(Status.OK, objectToBeSerialized);

        String serializedObj = Try.of(() -> objectMapper.writeValueAsString(response))
                .getOrElseThrow(throwable -> new SerializationException("There was a problem with serializing object",
                                                                        throwable));

       return Response.status(Status.OK)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(serializedObj)
                .build();
    }

    /**
     * @param data      Payload to return.
     * @param mediaType Non-JSON media type.
     * @return Non-JSON response, such as a shell script.
     */
    protected Response ok(String data, MediaType mediaType) {
        return Response.ok().entity(data).type(mediaType).build();
    }

    protected <T> Response created(String uri, T objectToBeSerialized) {
        ObjectMapper objectMapper = new ObjectMapper();

        ApiResponseDTO<T> response = new ApiResponseDTO<>(STATUS_OK, Status.CREATED.getStatusCode(), objectToBeSerialized);

        String serializedObj = Try.of(() -> objectMapper.writeValueAsString(response))
                .getOrElseThrow(throwable -> new SerializationException("There was a problem with serializing object",
                        throwable));

        return Response.created(URI.create(uri))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(serializedObj)
                .build();
    }


    protected Response created(String uri, JsonObjectBuilder bld) {
        return Response.created(URI.create(uri))
                .entity(Json.createObjectBuilder()
                                .add("status", "OK")
                                .add("data", bld).build())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    protected Response accepted(JsonObjectBuilder bld) {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                                .add("status", STATUS_WF_IN_PROGRESS)
                                .add("data", bld).build()
                ).build();
    }

    protected Response accepted() {
        return Response.accepted()
                .entity(Json.createObjectBuilder()
                                .add("status", STATUS_WF_IN_PROGRESS).build()
                ).build();
    }

    protected Response notFound(String msg) {
        return error(Status.NOT_FOUND, msg);
    }

    protected Response badRequest(String msg) {
        return error(Status.BAD_REQUEST, msg);
    }

    protected Response forbidden(String msg) {
        return error(Status.FORBIDDEN, msg);
    }

    protected Response badApiKey(String apiKey) {
        return error(Status.UNAUTHORIZED, (apiKey != null) ? "Bad api key " : "Please provide a key query parameter (?key=XXX) or via the HTTP header " + DATAVERSE_KEY_HEADER_NAME);
    }

    protected Response permissionError(PermissionException pe) {
        return permissionError(pe.getMessage());
    }

    protected Response permissionError(String message) {
        return unauthorized(message);
    }

    protected Response unauthorized(String message) {
        return error(Status.UNAUTHORIZED, message);
    }

    protected static Response error(Status sts, String msg) {
        return Response.status(sts)
                .entity(NullSafeJsonBuilder.jsonObjectBuilder()
                                .add("status", STATUS_ERROR)
                                .add("message", msg).build()
                ).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected Response allowCors(Response r) {
        r.getHeaders().add("Access-Control-Allow-Origin", "*");
        return r;
    }
}

class LazyRef<T> {
    private interface Ref<T> {
        T get();
    }

    private Ref<T> ref;

    public LazyRef(final Callable<T> initer) {
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

    public T get() {
        return ref.get();
    }
}
