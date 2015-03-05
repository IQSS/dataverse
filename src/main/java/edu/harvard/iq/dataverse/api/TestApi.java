package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.UserIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.util.Map;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import org.mindrot.jbcrypt.BCrypt;

/**
 * An API to test internal models without the need to deal with UI etc.
 *
 * @author michael
 */
@Stateless
@Path("test")
public class TestApi extends AbstractApiBean {

//    @EJB
//    AuthenticationServiceBean authSvc;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    ShibServiceBean shibService;
    
    @Path("echo/{whatever}")
    @GET
    public Response echo( @PathParam("whatever") String body ) {
        return okResponse(body);
    }
    
    @Path("permissions/{dvo}")
    @GET
    public Response findPermissonsOn(@PathParam("dvo") String dvo,
            @QueryParam("key") String key) {
        DvObject dvObj = findDvo(dvo);
        if (dvObj == null) {
            return notFound("DvObject " + dvo + " not found");
        }
        try {
            AuthenticatedUser au = findUserOrDie(key);
            return okResponse(json(permissionSvc.permissionsFor(au, dvObj)));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @Path("assignee/{idtf}")
    @GET
    public Response findRoleAssignee(@PathParam("idtf") String idtf) {
        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
        return (ra == null) ? notFound("Role Assignee '" + idtf + "' not found.")
                : okResponse(json(ra.getDisplayInfo()));
    }
    
    @Path("bcrypt/encrypt/{word}")
    @GET
    public String encrypt( @PathParam("word")String word, @QueryParam("len") String len ) {
        int saltLen = (len==null || len.trim().isEmpty() ) ? 10 : Integer.parseInt(len);
        return BCrypt.hashpw(word, BCrypt.gensalt(saltLen)) + "\n";
    }
    
    @Path("password/{w1}")
    @GET
    public String test( @PathParam("w1") String w1 ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[0] ").append( PasswordEncryption.getVersion(0).encrypt(w1)).append("\n");
        sb.append("[1] ").append( PasswordEncryption.getVersion(1).encrypt(w1)).append("\n");
        
        return sb.toString();
    }
    
    @Path("user/convert/builtin2shib")
    @PUT
    public Response builtin2shib(String content) {
        boolean disabled = false;
        if (disabled) {
            return errorResponse(Response.Status.BAD_REQUEST, "API endpoint disabled.");
        }
        AuthenticatedUser builtInUserToConvert = null;
        String emailToFind;
        String password;
        String authuserId = "0"; // could let people specify id on authuser table. probably better to let them tell us their 
        try {
            String[] args = content.split(":");
            emailToFind = args[0];
            password = args[1];
//            authuserId = args[666];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return errorResponse(Response.Status.BAD_REQUEST, ex.toString());
        }
        AuthenticatedUser existingAuthUserFoundByEmail = shibService.findAuthUserByEmail(emailToFind);
        String existing = "NOT FOUND";
        if (existingAuthUserFoundByEmail != null) {
            builtInUserToConvert = existingAuthUserFoundByEmail;
            existing = existingAuthUserFoundByEmail.getIdentifier();
        } else {
            long longToLookup = Long.parseLong(authuserId);
            AuthenticatedUser specifiedUserToConvert = authSvc.findByID(longToLookup);
            if (specifiedUserToConvert != null) {
                builtInUserToConvert = specifiedUserToConvert;
            } else {
                return errorResponse(Response.Status.BAD_REQUEST, "No user to convert. We couldn't find a *single* existing user account based on " + emailToFind + " and no user was found using specified id " + longToLookup);
            }
        }
        ShibAuthenticationProvider shibProvider = new ShibAuthenticationProvider();
        String shibProviderId = shibProvider.getId();
        Map<String, String> randomUser = shibService.getRandomUser();
//        String eppn = UUID.randomUUID().toString().substring(0, 8);
        String eppn = randomUser.get("eppn");
        String idPEntityId = randomUser.get("idp");
        String notUsed = null;
        String separator = "|";
        UserIdentifier newUserIdentifierInLookupTable = new UserIdentifier(idPEntityId + separator + eppn, notUsed);
        String overwriteFirstName = randomUser.get("firstName");
        String overwriteLastName = randomUser.get("lastName");
        String overwriteEmail = randomUser.get("email");
        /**
         * @todo If affiliation is not null, put it in RoleAssigneeDisplayInfo
         * constructor.
         */
        String overwriteAffiliation = shibService.getFriendlyInstitutionName(idPEntityId);
        /**
         * @todo Find a place to put "position" in the authenticateduser table:
         * https://github.com/IQSS/dataverse/issues/1444#issuecomment-74134694
         */
        String overwritePosition = "staff;student";
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(overwriteFirstName, overwriteLastName, overwriteEmail, overwriteAffiliation,overwritePosition);
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder problems = Json.createArrayBuilder();
        if (password != null) {
            response.add("password supplied", password);
            boolean knowsExistingPassword = false;
            BuiltinUser oldBuiltInUser = builtinUserService.findByUserName(builtInUserToConvert.getUserIdentifier());
            if (oldBuiltInUser != null) {
                String usernameOfBuiltinAccountToConvert = oldBuiltInUser.getUserName();
                response.add("old username", usernameOfBuiltinAccountToConvert);
                AuthenticatedUser authenticatedUser = shibService.canLogInAsBuiltinUser(usernameOfBuiltinAccountToConvert, password);
                if (authenticatedUser != null) {
                    knowsExistingPassword = true;
                    AuthenticatedUser convertedUser = authSvc.convertBuiltInToShib(builtInUserToConvert, shibProviderId, newUserIdentifierInLookupTable);
                    if (convertedUser != null) {
                        /**
                         * @todo Display name is not being overwritten. Logic
                         * must be in Shib backing bean
                         */
                        AuthenticatedUser updatedInfoUser = authSvc.updateAuthenticatedUser(convertedUser, displayInfo);
                        if (updatedInfoUser != null) {
                            response.add("display name overwritten with", updatedInfoUser.getName());
                        } else {
                            problems.add("couldn't update display info");
                        }
                    } else {
                        problems.add("unable to convert user");
                    }
                }
            } else {
                problems.add("couldn't find old username");
            }
            if (!knowsExistingPassword) {
                problems.add("doesn't know password");
            }
//            response.add("knows existing password", knowsExistingPassword);
        }

        response.add("user to convert", builtInUserToConvert.getIdentifier());
        response.add("existing user found by email (prompt to convert)", existing);
        response.add("changing to this provider", shibProviderId);
        response.add("value to overwrite old first name", overwriteFirstName);
        response.add("value to overwrite old last name", overwriteLastName);
        response.add("value to overwrite old email address", overwriteEmail);
        response.add("problems", problems);
        return okResponse(response);
    }
}
