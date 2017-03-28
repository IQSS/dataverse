package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
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
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonForAuthUser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.Date;

/**
 * REST API bean for managing {@link BuiltinUser}s.
 *
 * @author michael
 */
@Path("builtin-users")
public class BuiltinUsers extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(BuiltinUsers.class.getName());

    private static final String API_KEY_IN_SETTINGS = "BuiltinUsers.KEY";
    /**
     * Users have not requested the ability to retrieve their API token using
     * their email address but we could. Here's the issue for which we are
     * enabling login via email address:
     * https://github.com/IQSS/dataverse/issues/2115
     */
    public static boolean retrievingApiTokenViaEmailEnabled = false;

    @EJB
    protected BuiltinUserServiceBean builtinUserSvc;

    @GET
    @Path("{username}/api-token")
    public Response getApiToken( @PathParam("username") String username, @QueryParam("password") String password ) {
        BuiltinUser u = null;
        if (retrievingApiTokenViaEmailEnabled) {
            u = builtinUserSvc.findByUsernameOrEmail(username);
        } else {
            u = builtinUserSvc.findByUserName(username);
        }
        if ( u == null ) return badRequest("Bad username or password");
        
        boolean passwordOk = PasswordEncryption.getVersion(u.getPasswordEncryptionVersion())
                                            .check(password, u.getEncryptedPassword() );
        if ( ! passwordOk ) return badRequest("Bad username or password");
        
        AuthenticatedUser authUser = authSvc.lookupUser(BuiltinAuthenticationProvider.PROVIDER_ID, u.getUserName());
        
        ApiToken t = authSvc.findApiTokenByUser(authUser);
        
        return (t != null ) ? ok(t.getTokenString()) : notFound("User " + username + " does not have an API token");
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
    
    @POST
    public Response save(BuiltinUser user, @QueryParam("password") String password, @QueryParam("key") String key) {
        return internalSave(user, password, key);
    }

    private Response internalSave(BuiltinUser user, String password, String key) {
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
            
            // Make sure the identifier is unique
            if ( (builtinUserSvc.findByUserName(user.getUserName()) != null)
                    || ( authSvc.identifierExists(user.getUserName())) ) {
                return error(Status.BAD_REQUEST, "username '" + user.getUserName() + "' already exists");
            }
            user = builtinUserSvc.save(user);

            AuthenticatedUser au = authSvc.createAuthenticatedUser(
                    new UserRecordIdentifier(BuiltinAuthenticationProvider.PROVIDER_ID, user.getUserName()),
                    user.getUserName(), 
                    user.getDisplayInfo(),
                    false);

            /**
             * @todo Move this to
             * AuthenticationServiceBean.createAuthenticatedUser
             */
            userNotificationSvc.sendNotification(au,
                    new Timestamp(new Date().getTime()),
                    UserNotification.Type.CREATEACC, null);

            ApiToken token = new ApiToken();

            token.setTokenString(java.util.UUID.randomUUID().toString());
            token.setAuthenticatedUser(au);

            Calendar c = Calendar.getInstance();
            token.setCreateTime(new Timestamp(c.getTimeInMillis()));
            c.roll(Calendar.YEAR, 1);
            token.setExpireTime(new Timestamp(c.getTimeInMillis()));
            authSvc.save(token);

            JsonObjectBuilder resp = Json.createObjectBuilder();
            resp.add("user", json(user));
            resp.add("authenticatedUser", jsonForAuthUser(au));
            resp.add("apiToken", token.getTokenString());
            
            alr.setInfo("builtinUser:" + user.getUserName() + " authenticatedUser:" + au.getIdentifier() );
            return ok(resp);
            
        } catch ( EJBException ejbx ) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo( alr.getInfo() + "// " + ejbx.getMessage());
            if ( ejbx.getCausedByException() instanceof IllegalArgumentException ) {
                return error(Status.BAD_REQUEST, "Bad request: can't save user. " + ejbx.getCausedByException().getMessage());
            } else {
                logger.log(Level.WARNING, "Error saving user: ", ejbx);
                return error(Status.INTERNAL_SERVER_ERROR, "Can't save user: " + ejbx.getMessage());
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

}
