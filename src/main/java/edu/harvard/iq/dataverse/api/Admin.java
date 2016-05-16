package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.EMailValidator;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.errorResponse;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.UserIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
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

    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    ShibServiceBean shibService;

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

    @GET
    @Path("authenticatedUsers/{identifier}/")
    public Response getAuthenticatedUser(@PathParam("identifier") String identifier) {
        AuthenticatedUser authenticatedUser = authSvc.getAuthenticatedUser(identifier);
        if (authenticatedUser != null) {
            return okResponse(jsonForAuthUser(authenticatedUser));
        }
        return errorResponse(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
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

    @POST
    @Path("publishDataverseAsCreator/{id}")
    public Response publishDataverseAsCreator(@PathParam("id") long id) {
        try {
            Dataverse dataverse = dataverseSvc.find(id);
            if (dataverse != null) {
                AuthenticatedUser authenticatedUser = dataverse.getCreator();
                return okResponse(json(execCommand(new PublishDataverseCommand(createDataverseRequest(authenticatedUser), dataverse))));
            } else {
                return errorResponse(Status.BAD_REQUEST, "Could not find dataverse with id " + id);
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @Path("authenticatedUsers")
    public Response listAuthenticatedUsers() {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return errorResponse(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return errorResponse(Response.Status.FORBIDDEN, "Superusers only.");
        }
        JsonArrayBuilder userArray = Json.createArrayBuilder();
        authSvc.findAllAuthenticatedUsers().stream().forEach((user) -> {
            userArray.add(jsonForAuthUser(user));
        });
        return okResponse(userArray);
    }

    /**
     * curl -X PUT -d "shib@mailinator.com"
     * http://localhost:8080/api/admin/authenticatedUsers/id/11/convertShibToBuiltIn
     */
    @PUT
    @Path("authenticatedUsers/id/{id}/convertShibToBuiltIn")
    public Response convertShibUserToBuiltin(@PathParam("id") Long id, String newEmailAddress) {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return errorResponse(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return errorResponse(Response.Status.FORBIDDEN, "Superusers only.");
        }
        try {
            BuiltinUser builtinUser = authSvc.convertShibToBuiltIn(id, newEmailAddress);
            if (builtinUser == null) {
                return errorResponse(Response.Status.BAD_REQUEST, "User id " + id + " could not be converted from Shibboleth to BuiltIn. An Exception was not thrown.");
            }
            JsonObjectBuilder output = Json.createObjectBuilder();
            output.add("email", builtinUser.getEmail());
            output.add("username", builtinUser.getUserName());
            return okResponse(output);
        } catch (Throwable ex) {
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (ex.getCause() != null) {
                ex = ex.getCause();
                sb.append(ex + " ");
            }
            String msg = "User id " + id + " could not be converted from Shibboleth to BuiltIn. Details from Exception: " + sb;
            logger.info(msg);
            return errorResponse(Response.Status.BAD_REQUEST, msg);
        }
    }

    /**
     * This is used in testing via AdminIT.java but we don't expect sysadmins to
     * use this.
     */
    @Path("authenticatedUsers/convert/builtin2shib")
    @PUT
    public Response builtin2shib(String content) {
        logger.info("entering builtin2shib...");
        try {
            AuthenticatedUser userToRunThisMethod = findAuthenticatedUserOrDie();
            if (!userToRunThisMethod.isSuperuser()) {
                return errorResponse(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return errorResponse(Response.Status.FORBIDDEN, "Superusers only.");
        }
        boolean disabled = false;
        if (disabled) {
            return errorResponse(Response.Status.BAD_REQUEST, "API endpoint disabled.");
        }
        AuthenticatedUser builtInUserToConvert = null;
        String emailToFind;
        String password;
        String authuserId = "0"; // could let people specify id on authuser table. probably better to let them tell us their 
        String newEmailAddressToUse;
        try {
            String[] args = content.split(":");
            emailToFind = args[0];
            password = args[1];
            newEmailAddressToUse = args[2];
//            authuserId = args[666];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return errorResponse(Response.Status.BAD_REQUEST, "Problem with content <<<" + content + ">>>: " + ex.toString());
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
        String shibProviderId = ShibAuthenticationProvider.PROVIDER_ID;
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
        overwriteEmail = newEmailAddressToUse;
        logger.info("overwriteEmail: " + overwriteEmail);
        boolean validEmail = EMailValidator.isEmailValid(overwriteEmail, null);
        if (!validEmail) {
            // See https://github.com/IQSS/dataverse/issues/2998
            return errorResponse(Response.Status.BAD_REQUEST, "invalid email: " + overwriteEmail);
        }
        /**
         * @todo If affiliation is not null, put it in RoleAssigneeDisplayInfo
         * constructor.
         */
        /**
         * Here we are exercising (via an API test) shibService.getAffiliation
         * with the TestShib IdP and a non-production DevShibAccountType.
         */
        idPEntityId = ShibUtil.testShibIdpEntityId;
        String overwriteAffiliation = shibService.getAffiliation(idPEntityId, ShibServiceBean.DevShibAccountType.RANDOM);
        logger.info("overwriteAffiliation: " + overwriteAffiliation);
        /**
         * @todo Find a place to put "position" in the authenticateduser table:
         * https://github.com/IQSS/dataverse/issues/1444#issuecomment-74134694
         */
        String overwritePosition = "staff;student";
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(overwriteFirstName, overwriteLastName, overwriteEmail, overwriteAffiliation, overwritePosition);
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
                String message = "User doesn't know password.";
                problems.add(message);
                return errorResponse(Status.BAD_REQUEST, message);
            }
//            response.add("knows existing password", knowsExistingPassword);
        }

        response.add("user to convert", builtInUserToConvert.getIdentifier());
        response.add("existing user found by email (prompt to convert)", existing);
        response.add("changing to this provider", shibProviderId);
        response.add("value to overwrite old first name", overwriteFirstName);
        response.add("value to overwrite old last name", overwriteLastName);
        response.add("value to overwrite old email address", overwriteEmail);
        if (overwriteAffiliation != null) {
            response.add("affiliation", overwriteAffiliation);
        }
        response.add("problems", problems);
        return okResponse(response);
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
