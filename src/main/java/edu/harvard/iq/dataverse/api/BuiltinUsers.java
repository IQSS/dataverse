package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Date;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

/**
 * REST API bean for managing {@link BuiltinUser}s.
 *
 * @author michael
 */
@Path("builtin-users")
public class BuiltinUsers extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(BuiltinUsers.class.getName());

    private static final String API_KEY_IN_SETTINGS = "BuiltinUsers.KEY";

    @EJB
    protected BuiltinUserServiceBean builtinUserSvc;

    @GET
    @Path("{username}/api-token")
    public Response getApiToken( @PathParam("username") String username, @QueryParam("password") String password ) {
        boolean disabled = true;
        boolean lookupAllowed = settingsSvc.isTrueForKey(SettingsServiceBean.Key.AllowApiTokenLookupViaApi, false);
        if (lookupAllowed) {
            disabled = false;
        }
        if (disabled) {
            return error(Status.FORBIDDEN, "This API endpoint has been disabled.");
        }
        BuiltinUser u = null;

        u = builtinUserSvc.findByUserName(username);

        if ( u == null ) return badRequest("Bad username or password");
        
        boolean passwordOk = PasswordEncryption.getVersion(u.getPasswordEncryptionVersion())
                                            .check(password, u.getEncryptedPassword() );
        if ( ! passwordOk ) return badRequest("Bad username or password");
        
        AuthenticatedUser authUser = authSvc.lookupUser(BuiltinAuthenticationProvider.PROVIDER_ID, u.getUserName());
        
        ApiToken t = authSvc.findApiTokenByUser(authUser);
        
        return (t != null ) ? ok(t.getTokenString()) : notFound("User " + username + " does not have an API token");
    }
    
    
    //These two endpoints take in a BuiltinUser as json. To support these endpoints
    //with the removal of attributes from BuiltinUser in 4565, BuiltinUser supports
    //the extended attributes as transient (not stored to the database). They are
    //immediately used to create an AuthenticatedUser.
    //If this proves to be too confusing, we can parse the json more manually
    //and use the values to create BuiltinUser/AuthenticatedUser.
    //--MAD 4.9.3
    @POST
    public Response save(BuiltinUser user, @QueryParam("password") String password, @QueryParam("key") String key, @QueryParam("sendEmailNotification") Boolean sendEmailNotification) {   
        if( sendEmailNotification == null )
            sendEmailNotification = true;
        
        return internalSave(user, password, key, sendEmailNotification);
    }

    /**
     * Created this new API command because the save method could not be run
     * from the RestAssured API. RestAssured doesn't allow a Post request to
     * contain both a body and request parameters. TODO: replace current usage
     * of save() with create?
     *
     * @param user
     * @param password
     * @param key
     * @return
     */
    @POST
    @Path("{password}/{key}")
    public Response create(BuiltinUser user, @PathParam("password") String password, @PathParam("key") String key) {
        return internalSave(user, password, key);
    }
    
    /**
     * Created this new endpoint to resolve issue #6915, optionally preventing 
     * the email notification to the new user on account creation by adding 
     * "false" as the third path parameter.
     *
     * @param user
     * @param password
     * @param key
     * @param sendEmailNotification
     * @return
     */
    @POST
    @Path("{password}/{key}/{sendEmailNotification}")
    public Response create(BuiltinUser user, @PathParam("password") String password, @PathParam("key") String key, @PathParam("sendEmailNotification") Boolean sendEmailNotification) {
        return internalSave(user, password, key, sendEmailNotification);
    }
    
    // internalSave without providing an explicit "sendEmailNotification"
    private Response internalSave(BuiltinUser user, String password, String key) {
        return internalSave(user, password, key, true);
    }
    
    private Response internalSave(BuiltinUser user, String password, String key, Boolean sendEmailNotification) {
        String expectedKey = settingsSvc.get(API_KEY_IN_SETTINGS);
        
        if (expectedKey == null) {
            return error(Status.SERVICE_UNAVAILABLE, "Dataverse config issue: No API key defined for built in user management");
        }
        if (!expectedKey.equals(key)) {
            return badApiKey(key);
        }
        
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.BuiltinUser, "create");
        
        try {
            
            if (password != null) {
                user.updateEncryptedPassword(PasswordEncryption.get().encrypt(password), PasswordEncryption.getLatestVersionNumber());
            }
            
            // Make sure the identifier is unique, case insensitive. "DATAVERSEADMIN" is not allowed to be created if "dataverseAdmin" exists.
            if ((builtinUserSvc.findByUserName(user.getUserName()) != null)
                    || (authSvc.identifierExists(user.getUserName()))) {
                return error(Status.BAD_REQUEST, "username '" + user.getUserName() + "' already exists");
            }
            user = builtinUserSvc.save(user);

            AuthenticatedUser au = authSvc.createAuthenticatedUser(
                    new UserRecordIdentifier(BuiltinAuthenticationProvider.PROVIDER_ID, user.getUserName()),
                    user.getUserName(), 
                    user.getDisplayInfoForApiCreation(),
                    false);

            /**
             * @todo Move this to
             * AuthenticationServiceBean.createAuthenticatedUser
             */
            boolean rootDataversePresent = false;
            try {
                Dataverse rootDataverse = dataverseSvc.findRootDataverse();
                if (rootDataverse != null) {
                    rootDataversePresent = true;
                }
            } catch (Exception e) {
                logger.info("The root dataverse is not present. Don't send a notification to dataverseAdmin.");
            }
            if (rootDataversePresent && sendEmailNotification) {
                userNotificationSvc.sendNotification(au,
                        new Timestamp(new Date().getTime()),
                        UserNotification.Type.CREATEACC, null);
            }

            ApiToken token = authSvc.generateApiTokenForUser(au);
            authSvc.save(token);

            JsonObjectBuilder resp = Json.createObjectBuilder();
            resp.add("user", json(user));
            resp.add("authenticatedUser", json(au));
            resp.add("apiToken", token.getTokenString());
            
            alr.setInfo("builtinUser:" + user.getUserName() + " authenticatedUser:" + au.getIdentifier() );
            return ok(resp);
            
        } catch ( EJBException ejbx ) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            String errorMessage = ejbx.getCausedByException().getLocalizedMessage();
            alr.setInfo( alr.getInfo() + "// " + errorMessage);
            if ( ejbx.getCausedByException() instanceof IllegalArgumentException ) {
                return error(Status.BAD_REQUEST, "Bad request: can't save user. " + errorMessage);
            } else {
                logger.log(Level.WARNING, "Error saving user: ", ejbx);
                return error(Status.INTERNAL_SERVER_ERROR, "Can't save user: " + errorMessage);
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error saving user", e);
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo( alr.getInfo() + "// " + e.getMessage());
            return error(Status.INTERNAL_SERVER_ERROR, "Can't save user: " + e.getMessage());
        } finally {
            actionLogSvc.log(alr);
        }
    }

    /***
     * This method was moved here from AbstractApiBean during the filter-based auth
     * refactoring, in order to preserve the existing BuiltinUsers endpoints behavior.
     *
     * @param apiKey from request
     * @return error Response
     */
    private Response badApiKey(String apiKey) {
        return error(Status.UNAUTHORIZED, (apiKey != null) ? "Bad api key " : "Please provide a key query parameter (?key=XXX) or via the HTTP header " + ApiKeyAuthMechanism.DATAVERSE_API_KEY_REQUEST_HEADER_NAME);
    }
}
