package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.BannerMessage;
import edu.harvard.iq.dataverse.BannerMessageServiceBean;
import edu.harvard.iq.dataverse.BannerMessageText;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsValidationException;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.cache.CacheFactoryBean;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.validation.EMailValidator;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.TemplateServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.UserIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailData;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailException;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailInitResponse;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.settings.Setting;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import java.util.List;
import edu.harvard.iq.dataverse.authorization.AuthTestDataServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvidersRegistrationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeactivateUserCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteTemplateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RegisterDvObjectCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.URLTokenUtil;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.rolesToJson;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.persistence.Query;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.nio.file.Paths;
import java.util.TreeMap;

/**
 * Where the secure, setup API calls live.
 * 
 * @author michael
 */
@Stateless
@Path("admin")
public class Admin extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Admin.class.getName());

    @EJB
    AuthenticationProvidersRegistrationServiceBean authProvidersRegistrationSvc;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    ShibServiceBean shibService;
    @EJB
    AuthTestDataServiceBean authTestDataService;
    @EJB
    UserServiceBean userService;
    @EJB
    IngestServiceBean ingestService;
    @EJB
    DataFileServiceBean fileService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DatasetVersionServiceBean datasetversionService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    GroupServiceBean groupService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    ExplicitGroupServiceBean explicitGroupService;
    @EJB
    BannerMessageServiceBean bannerMessageService;
    @EJB
    TemplateServiceBean templateService;
    @EJB
    CacheFactoryBean cacheFactory;

    // Make the session available
    @Inject
    DataverseSession session;

    public static final String listUsersPartialAPIPath = "list-users";
    public static final String listUsersFullAPIPath = "/api/admin/" + listUsersPartialAPIPath;
    
    @Path("settings")
    @GET
    @APIResponses({
        @APIResponse(responseCode = "200",
            description = "All database options successfully queried",
            // The schema may be extended later to better describe what the JSON object looks like.
            content = @Content(schema = @Schema(implementation = JsonObject.class))),
    })
    public Response listAllSettings() {
        return ok(settingsSvc.listAllAsJson());
    }
    
    @Path("settings")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponses({
        @APIResponse(responseCode = "200", description = "All database options successfully updated")
    })
    public Response putAllSettings(JsonObject settings) {
        try {
            // Basic JSON structure validation only
            if (settings == null || settings.isEmpty()) {
                return error(Response.Status.BAD_REQUEST, "Empty or invalid JSON object");
            }
            
            // Transfer to domain objects and deeper validation to be handled by the service layer.
            JsonObjectBuilder successfullOperations = settingsSvc.setAllFromJson(settings);
            return ok("All database options successfully updated.", successfullOperations);
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }
    
    @Path("settings/{name}")
    @PUT
    public Response putSetting(@PathParam("name") String name, String content) {
        try {
            SettingsServiceBean.validateSettingName(name);
            
            Setting s = settingsSvc.set(name, content);
            return ok("Setting " + name + " added.");
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }

    @Path("settings/{name}/lang/{lang}")
    @PUT
    public Response putSettingLang(@PathParam("name") String name, @PathParam("lang") String lang, String content) {
        try {
            SettingsServiceBean.validateSettingName(name);
            SettingsServiceBean.validateSettingLang(lang);
            
            Setting s = settingsSvc.set(name, lang, content);
            return ok("Setting " + name + " added for language " + lang + ".");
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }

    @Path("settings/{name}")
    @GET
    public Response getSetting(@PathParam("name") String name) {
        try {
            SettingsServiceBean.validateSettingName(name);
            
            String content = settingsSvc.get(name);
            return (content != null) ? ok(content) : notFound("Setting " + name + " not found.");
        } catch (IllegalArgumentException iae) {
            return error(Response.Status.BAD_REQUEST, iae.getMessage());
        }
    }
    
    @Path("settings/{name}/lang/{lang}")
    @GET
    public Response getSetting(@PathParam("name") String name, @PathParam("lang") String lang) {
        try {
            SettingsServiceBean.validateSettingName(name);
            SettingsServiceBean.validateSettingLang(lang);
            
            String content = settingsSvc.get(name, lang, null);
            return (content != null) ? ok(content) : notFound("Setting " + name + " for language " + lang + " not found.");
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }

    @Path("settings/{name}")
    @DELETE
    public Response deleteSetting(@PathParam("name") String name) {
        try {
            SettingsServiceBean.validateSettingName(name);
            
            settingsSvc.delete(name);
            return ok("Setting " + name + " deleted.");
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }

    @Path("settings/{name}/lang/{lang}")
    @DELETE
    public Response deleteSettingLang(@PathParam("name") String name, @PathParam("lang") String lang) {
        try {
            SettingsServiceBean.validateSettingName(name);
            SettingsServiceBean.validateSettingLang(lang);
            
            settingsSvc.delete(name, lang);
            return ok("Setting " + name + " for language " + lang + " deleted.");
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }
        
    @Path("template/{id}")
    @DELETE
    public Response deleteTemplate(@PathParam("id") long id) {
        
        AuthenticatedUser superuser = authSvc.getAdminUser();
        if (superuser == null) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Cannot find superuser to execute DeleteTemplateCommand.");
        }

        Template doomed = templateService.find(id);
        if (doomed == null) {
            return error(Response.Status.NOT_FOUND, "Template with id " + id + " -  not found.");
        }

        Dataverse dv = doomed.getDataverse();
        List <Dataverse> dataverseWDefaultTemplate = templateService.findDataversesByDefaultTemplateId(doomed.getId());

        try {
            commandEngine.submit(new DeleteTemplateCommand(createDataverseRequest(superuser), dv, doomed, dataverseWDefaultTemplate));
        } catch (CommandException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
            return error(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
        }

        return ok("Template " + doomed.getName() + " deleted.");
    }
    
    
    @Path("templates")
    @GET
    public Response findAllTemplates() {
        return findTemplates("");
    }
    
    @Path("templates/{alias}")
    @GET
    public Response findTemplates(@PathParam("alias") String alias) {
        List<Template> templates;

            if (alias.isEmpty()) {
                templates = templateService.findAll();
            } else {
                try{
                    Dataverse owner = findDataverseOrDie(alias);
                    templates = templateService.findByOwnerId(owner.getId());
                } catch (WrappedResponse r){
                    return r.getResponse();
                }
            }

            JsonArrayBuilder container = Json.createArrayBuilder();
            for (Template t : templates) {
                JsonObjectBuilder bld = Json.createObjectBuilder();
                bld.add("templateId", t.getId());
                bld.add("templateName", t.getName());
                Dataverse loopowner = t.getDataverse();
                if (loopowner != null) {
                    bld.add("owner", loopowner.getDisplayName());
                } else {
                    bld.add("owner", "This an orphan template, it may be safely removed");
                }
                container.add(bld);
            }

            return ok(container);

        
    }

    @Path("authenticationProviderFactories")
    @GET
    public Response listAuthProviderFactories() {
        return ok(authSvc.listProviderFactories().stream()
                .map(f -> jsonObjectBuilder().add("alias", f.getAlias()).add("info", f.getInfo()))
                .collect(toJsonArray()));
    }

    @Path("authenticationProviders")
    @GET
    public Response listAuthProviders() {
        return ok(em.createNamedQuery("AuthenticationProviderRow.findAll", AuthenticationProviderRow.class)
                .getResultList().stream().map(r -> json(r)).collect(toJsonArray()));
    }

    @Path("authenticationProviders")
    @POST
    public Response addProvider(AuthenticationProviderRow row) {
        try {
            AuthenticationProviderRow managed = em.find(AuthenticationProviderRow.class, row.getId());
            if (managed != null) {
                managed = em.merge(row);
            } else {
                em.persist(row);
                managed = row;
            }
            if (managed.isEnabled()) {
                AuthenticationProvider provider = authProvidersRegistrationSvc.loadProvider(managed);
                authProvidersRegistrationSvc.deregisterProvider(provider.getId());
                authProvidersRegistrationSvc.registerProvider(provider);
            }
            return created("/api/admin/authenticationProviders/" + managed.getId(), json(managed));
        } catch (AuthorizationSetupException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Path("authenticationProviders/{id}")
    @GET
    public Response showProvider(@PathParam("id") String id) {
        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        return (row != null) ? ok(json(row))
                : error(Status.NOT_FOUND, "Can't find authetication provider with id '" + id + "'");
    }

    @POST
    @Path("authenticationProviders/{id}/:enabled")
    public Response enableAuthenticationProvider_deprecated(@PathParam("id") String id, String body) {
        return enableAuthenticationProvider(id, body);
    }

    @PUT
    @Path("authenticationProviders/{id}/enabled")
    @Produces("application/json")
    public Response enableAuthenticationProvider(@PathParam("id") String id, String body) {
        body = body.trim();
        if (!Util.isBoolean(body)) {
            return error(Response.Status.BAD_REQUEST, "Illegal value '" + body + "'. Use 'true' or 'false'");
        }
        boolean enable = Util.isTrue(body);

        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        if (row == null) {
            return notFound("Can't find authentication provider with id '" + id + "'");
        }

        row.setEnabled(enable);
        em.merge(row);

        if (enable) {
            // enable a provider
            if (authSvc.getAuthenticationProvider(id) != null) {
                return ok(String.format("Authentication provider '%s' already enabled", id));
            }
            try {
                authProvidersRegistrationSvc.registerProvider(authProvidersRegistrationSvc.loadProvider(row));
                return ok(String.format("Authentication Provider %s enabled", row.getId()));

            } catch (AuthenticationProviderFactoryNotFoundException ex) {
                return notFound(String.format("Can't instantiate provider, as there's no factory with alias %s",
                        row.getFactoryAlias()));
            } catch (AuthorizationSetupException ex) {
                logger.log(Level.WARNING, "Error instantiating authentication provider: " + ex.getMessage(), ex);
                return error(Status.INTERNAL_SERVER_ERROR,
                        String.format("Can't instantiate provider: %s", ex.getMessage()));
            }

        } else {
            // disable a provider
            authProvidersRegistrationSvc.deregisterProvider(id);
            return ok("Authentication Provider '" + id + "' disabled. "
                    + (authSvc.getAuthenticationProviderIds().isEmpty()
                            ? "WARNING: no enabled authentication providers left."
                            : ""));
        }
    }

    @GET
    @Path("authenticationProviders/{id}/enabled")
    public Response checkAuthenticationProviderEnabled(@PathParam("id") String id) {
        List<AuthenticationProviderRow> prvs = em
                .createNamedQuery("AuthenticationProviderRow.findById", AuthenticationProviderRow.class)
                .setParameter("id", id).getResultList();
        if (prvs.isEmpty()) {
            return notFound("Can't find a provider with id '" + id + "'.");
        } else {
            return ok(Boolean.toString(prvs.get(0).isEnabled()));
        }
    }

    @DELETE
    @Path("authenticationProviders/{id}/")
    public Response deleteAuthenticationProvider(@PathParam("id") String id) {
        authProvidersRegistrationSvc.deregisterProvider(id);
        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        if (row != null) {
            em.remove(row);
        }

        return ok("AuthenticationProvider " + id + " deleted. "
                + (authSvc.getAuthenticationProviderIds().isEmpty()
                        ? "WARNING: no enabled authentication providers left."
                        : ""));
    }

    @GET
    @Path("authenticatedUsers/{identifier}/")
    public Response getAuthenticatedUserByIdentifier(@PathParam("identifier") String identifier) {
        AuthenticatedUser authenticatedUser = authSvc.getAuthenticatedUser(identifier);
        if (authenticatedUser != null) {
            return ok(json(authenticatedUser));
        }
        return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
    }

    @DELETE
    @Path("authenticatedUsers/{identifier}/")
    public Response deleteAuthenticatedUser(@PathParam("identifier") String identifier) {
        AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
        if (user != null) {
            return deleteAuthenticatedUser(user);
        }
        return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
    }
    
    @DELETE
    @Path("authenticatedUsers/id/{id}/")
    public Response deleteAuthenticatedUserById(@PathParam("id") Long id) {
        AuthenticatedUser user = authSvc.findByID(id);
        if (user != null) {
            return deleteAuthenticatedUser(user);
        }
        return error(Response.Status.BAD_REQUEST, "User " + id + " not found.");
    }

    private Response deleteAuthenticatedUser(AuthenticatedUser au) {
        
        //getDeleteUserErrorMessages does all of the tests to see
        //if the user is 'deletable' if it returns an empty string the user 
        //can be safely deleted.
        
        String errorMessages = authSvc.getDeleteUserErrorMessages(au);
        
        if (!errorMessages.isEmpty()) {
            return badRequest(errorMessages);
        }
        
        //if the user is deletable we will delete access requests and group membership
        // many-to-many relationships that couldn't be cascade deleted
        authSvc.removeAuthentictedUserItems(au);
        
        authSvc.deleteAuthenticatedUser(au.getId());
        return ok("AuthenticatedUser " + au.getIdentifier() + " deleted.");
    }

    @POST
    @Path("authenticatedUsers/{identifier}/deactivate")
    public Response deactivateAuthenticatedUser(@PathParam("identifier") String identifier) {
        AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
        if (user != null) {
            return deactivateAuthenticatedUser(user);
        }
        return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
    }

    @POST
    @Path("authenticatedUsers/id/{id}/deactivate")
    public Response deactivateAuthenticatedUserById(@PathParam("id") Long id) {
        AuthenticatedUser user = authSvc.findByID(id);
        if (user != null) {
            return deactivateAuthenticatedUser(user);
        }
        return error(Response.Status.BAD_REQUEST, "User " + id + " not found.");
    }

    private Response deactivateAuthenticatedUser(AuthenticatedUser userToDisable) {
        AuthenticatedUser superuser = authSvc.getAdminUser();
        if (superuser == null) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Cannot find superuser to execute DeactivateUserCommand.");
        }
        try {
            execCommand(new DeactivateUserCommand(createDataverseRequest(superuser), userToDisable));
            return ok("User " + userToDisable.getIdentifier() + " deactivated.");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @Path("publishDataverseAsCreator/{id}")
    public Response publishDataverseAsCreator(@PathParam("id") long id) {
        try {
            Dataverse dataverse = dataverseSvc.find(id);
            if (dataverse != null) {
                AuthenticatedUser authenticatedUser = dataverse.getCreator();
                return ok(json(execCommand(
                        new PublishDataverseCommand(createDataverseRequest(authenticatedUser), dataverse))));
            } else {
                return error(Status.BAD_REQUEST, "Could not find dataverse with id " + id);
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @Deprecated
    @GET
    @AuthRequired
    @Path("authenticatedUsers")
    public Response listAuthenticatedUsers(@Context ContainerRequestContext crc) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        JsonArrayBuilder userArray = Json.createArrayBuilder();
        authSvc.findAllAuthenticatedUsers().stream().forEach((user) -> {
            userArray.add(json(user));
        });
        return ok(userArray);
    }

    @GET
    @AuthRequired
    @Path(listUsersPartialAPIPath)
    @Produces({ "application/json" })
    public Response filterAuthenticatedUsers(
            @Context ContainerRequestContext crc,
            @QueryParam("searchTerm") String searchTerm,
            @QueryParam("selectedPage") Integer selectedPage,
            @QueryParam("itemsPerPage") Integer itemsPerPage,
            @QueryParam("sortKey") String sortKey
    ) {

        User authUser = getRequestUser(crc);

        if (!authUser.isSuperuser()) {
            return error(Response.Status.FORBIDDEN,
                    BundleUtil.getStringFromBundle("dashboard.list_users.api.auth.not_superuser"));
        }

        UserListMaker userListMaker = new UserListMaker(userService);

        // String sortKey = null;
        UserListResult userListResult = userListMaker.runUserSearch(searchTerm, itemsPerPage, selectedPage, sortKey);

        return ok(userListResult.toJSON());
    }

    /**
     * @todo Make this support creation of BuiltInUsers.
     *
     * @todo Add way more error checking. Only the happy path is tested by AdminIT.
     */
    @POST
    @Path("authenticatedUsers")
    public Response createAuthenicatedUser(JsonObject jsonObject) {
        logger.fine("JSON in: " + jsonObject);
        String persistentUserId = jsonObject.getString("persistentUserId");
        String identifier = jsonObject.getString("identifier");
        String proposedAuthenticatedUserIdentifier = identifier.replaceFirst("@", "");
        String firstName = jsonObject.getString("firstName");
        String lastName = jsonObject.getString("lastName");
        String emailAddress = jsonObject.getString("email");
        String position = null;
        String affiliation = null;
        UserRecordIdentifier userRecordId = new UserRecordIdentifier(jsonObject.getString("authenticationProviderId"),
                persistentUserId);
        AuthenticatedUserDisplayInfo userDisplayInfo = new AuthenticatedUserDisplayInfo(firstName, lastName,
                emailAddress, affiliation, position);
        boolean generateUniqueIdentifier = true;
        AuthenticatedUser authenticatedUser = authSvc.createAuthenticatedUser(userRecordId,
                proposedAuthenticatedUserIdentifier, userDisplayInfo, true);
        return ok(json(authenticatedUser));
    }

        //TODO: Delete this endpoint after 4.9.3. Was updated with change in docs. --MAD
    /**
     * curl -X PUT -d "shib@mailinator.com"
     * http://localhost:8080/api/admin/authenticatedUsers/id/11/convertShibToBuiltIn
     *
     * @deprecated We have documented this API endpoint so we'll keep in around for
     *             a while but we should encourage everyone to switch to the
     *             "convertRemoteToBuiltIn" endpoint and then remove this
     *             Shib-specfic one.
     */
    @PUT
    @AuthRequired
    @Path("authenticatedUsers/id/{id}/convertShibToBuiltIn")
    @Deprecated
    public Response convertShibUserToBuiltin(@Context ContainerRequestContext crc, @PathParam("id") Long id, String newEmailAddress) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        try {
            BuiltinUser builtinUser = authSvc.convertRemoteToBuiltIn(id, newEmailAddress);
            if (builtinUser == null) {
                return error(Response.Status.BAD_REQUEST, "User id " + id
                        + " could not be converted from Shibboleth to BuiltIn. An Exception was not thrown.");
            }
                        AuthenticatedUser authUser = authSvc.getAuthenticatedUser(builtinUser.getUserName());
            JsonObjectBuilder output = Json.createObjectBuilder();
            output.add("email", authUser.getEmail());
            output.add("username", builtinUser.getUserName());
            return ok(output);
        } catch (Throwable ex) {
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (ex.getCause() != null) {
                ex = ex.getCause();
                sb.append(ex + " ");
            }
            String msg = "User id " + id
                    + " could not be converted from Shibboleth to BuiltIn. Details from Exception: " + sb;
            logger.info(msg);
            return error(Response.Status.BAD_REQUEST, msg);
        }
    }

    @PUT
    @AuthRequired
    @Path("authenticatedUsers/id/{id}/convertRemoteToBuiltIn")
    public Response convertOAuthUserToBuiltin(@Context ContainerRequestContext crc, @PathParam("id") Long id, String newEmailAddress) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        try {
            BuiltinUser builtinUser = authSvc.convertRemoteToBuiltIn(id, newEmailAddress);
                        //AuthenticatedUser authUser = authService.getAuthenticatedUser(aUser.getUserName());
            if (builtinUser == null) {
                return error(Response.Status.BAD_REQUEST, "User id " + id
                        + " could not be converted from remote to BuiltIn. An Exception was not thrown.");
            }
                        AuthenticatedUser authUser = authSvc.getAuthenticatedUser(builtinUser.getUserName());
            JsonObjectBuilder output = Json.createObjectBuilder();
            output.add("email", authUser.getEmail());
            output.add("username", builtinUser.getUserName());
            return ok(output);
        } catch (Throwable ex) {
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (ex.getCause() != null) {
                ex = ex.getCause();
                sb.append(ex + " ");
            }
            String msg = "User id " + id + " could not be converted from remote to BuiltIn. Details from Exception: "
                    + sb;
            logger.info(msg);
            return error(Response.Status.BAD_REQUEST, msg);
        }
    }

    /**
     * This is used in testing via AdminIT.java but we don't expect sysadmins to use
     * this.
     */
    @PUT
    @AuthRequired
    @Path("authenticatedUsers/convert/builtin2shib")
    public Response builtin2shib(@Context ContainerRequestContext crc, String content) {
        logger.info("entering builtin2shib...");
        try {
            AuthenticatedUser userToRunThisMethod = getRequestAuthenticatedUserOrDie(crc);
            if (!userToRunThisMethod.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        boolean disabled = false;
        if (disabled) {
            return error(Response.Status.BAD_REQUEST, "API endpoint disabled.");
        }
        AuthenticatedUser builtInUserToConvert = null;
        String emailToFind;
        String password;
        String authuserId = "0"; // could let people specify id on authuser table. probably better to let them
                                    // tell us their
        String newEmailAddressToUse;
        try {
            String[] args = content.split(":");
            emailToFind = args[0];
            password = args[1];
            newEmailAddressToUse = args[2];
            // authuserId = args[666];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return error(Response.Status.BAD_REQUEST, "Problem with content <<<" + content + ">>>: " + ex.toString());
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
                return error(Response.Status.BAD_REQUEST,
                        "No user to convert. We couldn't find a *single* existing user account based on " + emailToFind
                                + " and no user was found using specified id " + longToLookup);
            }
        }
        String shibProviderId = ShibAuthenticationProvider.PROVIDER_ID;
        Map<String, String> randomUser = authTestDataService.getRandomUser();
        // String eppn = UUID.randomUUID().toString().substring(0, 8);
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
        boolean validEmail = EMailValidator.isEmailValid(overwriteEmail);
        if (!validEmail) {
            // See https://github.com/IQSS/dataverse/issues/2998
            return error(Response.Status.BAD_REQUEST, "invalid email: " + overwriteEmail);
        }
        /**
         * @todo If affiliation is not null, put it in RoleAssigneeDisplayInfo
         *       constructor.
         */
        /**
         * Here we are exercising (via an API test) shibService.getAffiliation with the
         * TestShib IdP and a non-production DevShibAccountType.
         */
        idPEntityId = ShibUtil.testShibIdpEntityId;
        String overwriteAffiliation = shibService.getAffiliation(idPEntityId,
                ShibServiceBean.DevShibAccountType.RANDOM);
        logger.info("overwriteAffiliation: " + overwriteAffiliation);
        /**
         * @todo Find a place to put "position" in the authenticateduser table:
         *       https://github.com/IQSS/dataverse/issues/1444#issuecomment-74134694
         */
        String overwritePosition = "staff;student";
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(overwriteFirstName,
                overwriteLastName, overwriteEmail, overwriteAffiliation, overwritePosition);
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder problems = Json.createArrayBuilder();
        if (password != null) {
            response.add("password supplied", password);
            boolean knowsExistingPassword = false;
            BuiltinUser oldBuiltInUser = builtinUserService.findByUserName(builtInUserToConvert.getUserIdentifier());
            if (oldBuiltInUser != null) {
                                if (builtInUserToConvert.isDeactivated()) {
                                        problems.add("builtin account has been deactivated");
                                        return error(Status.BAD_REQUEST, problems.build().toString());
                                }
                String usernameOfBuiltinAccountToConvert = oldBuiltInUser.getUserName();
                response.add("old username", usernameOfBuiltinAccountToConvert);
                AuthenticatedUser authenticatedUser = authSvc.canLogInAsBuiltinUser(usernameOfBuiltinAccountToConvert,
                        password);
                if (authenticatedUser != null) {
                    knowsExistingPassword = true;
                    AuthenticatedUser convertedUser = authSvc.convertBuiltInToShib(builtInUserToConvert, shibProviderId,
                            newUserIdentifierInLookupTable);
                    if (convertedUser != null) {
                        /**
                         * @todo Display name is not being overwritten. Logic must be in Shib backing
                         *       bean
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
                /**
                 * @todo Someday we should make a errorResponse method that takes JSON arrays
                 *       and objects.
                 */
                return error(Status.BAD_REQUEST, problems.build().toString());
            }
            // response.add("knows existing password", knowsExistingPassword);
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
        return ok(response);
    }

    /**
     * This is used in testing via AdminIT.java but we don't expect sysadmins to use
     * this.
     */
    @PUT
    @AuthRequired
    @Path("authenticatedUsers/convert/builtin2oauth")
    public Response builtin2oauth(@Context ContainerRequestContext crc, String content) {
        logger.info("entering builtin2oauth...");
        try {
            AuthenticatedUser userToRunThisMethod = getRequestAuthenticatedUserOrDie(crc);
            if (!userToRunThisMethod.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        boolean disabled = false;
        if (disabled) {
            return error(Response.Status.BAD_REQUEST, "API endpoint disabled.");
        }
        AuthenticatedUser builtInUserToConvert = null;
        String emailToFind;
        String password;
        String authuserId = "0"; // could let people specify id on authuser table. probably better to let them
                                    // tell us their
        String newEmailAddressToUse;
        String newProviderId;
        String newPersistentUserIdInLookupTable;
        logger.info("content: " + content);
        try {
            String[] args = content.split(":");
            emailToFind = args[0];
            password = args[1];
            newEmailAddressToUse = args[2];
            newProviderId = args[3];
            newPersistentUserIdInLookupTable = args[4];
            // authuserId = args[666];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return error(Response.Status.BAD_REQUEST, "Problem with content <<<" + content + ">>>: " + ex.toString());
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
                return error(Response.Status.BAD_REQUEST,
                        "No user to convert. We couldn't find a *single* existing user account based on " + emailToFind
                                + " and no user was found using specified id " + longToLookup);
            }
        }
        // String shibProviderId = ShibAuthenticationProvider.PROVIDER_ID;
        Map<String, String> randomUser = authTestDataService.getRandomUser();
        // String eppn = UUID.randomUUID().toString().substring(0, 8);
        String eppn = randomUser.get("eppn");
        String idPEntityId = randomUser.get("idp");
        String notUsed = null;
        String separator = "|";
        // UserIdentifier newUserIdentifierInLookupTable = new
        // UserIdentifier(idPEntityId + separator + eppn, notUsed);
        UserIdentifier newUserIdentifierInLookupTable = new UserIdentifier(newPersistentUserIdInLookupTable, notUsed);
        String overwriteFirstName = randomUser.get("firstName");
        String overwriteLastName = randomUser.get("lastName");
        String overwriteEmail = randomUser.get("email");
        overwriteEmail = newEmailAddressToUse;
        logger.info("overwriteEmail: " + overwriteEmail);
        boolean validEmail = EMailValidator.isEmailValid(overwriteEmail);
        if (!validEmail) {
            // See https://github.com/IQSS/dataverse/issues/2998
            return error(Response.Status.BAD_REQUEST, "invalid email: " + overwriteEmail);
        }
        /**
         * @todo If affiliation is not null, put it in RoleAssigneeDisplayInfo
         *       constructor.
         */
        /**
         * Here we are exercising (via an API test) shibService.getAffiliation with the
         * TestShib IdP and a non-production DevShibAccountType.
         */
        // idPEntityId = ShibUtil.testShibIdpEntityId;
        // String overwriteAffiliation = shibService.getAffiliation(idPEntityId,
        // ShibServiceBean.DevShibAccountType.RANDOM);
        String overwriteAffiliation = null;
        logger.info("overwriteAffiliation: " + overwriteAffiliation);
        /**
         * @todo Find a place to put "position" in the authenticateduser table:
         *       https://github.com/IQSS/dataverse/issues/1444#issuecomment-74134694
         */
        String overwritePosition = "staff;student";
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(overwriteFirstName,
                overwriteLastName, overwriteEmail, overwriteAffiliation, overwritePosition);
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder problems = Json.createArrayBuilder();
        if (password != null) {
            response.add("password supplied", password);
            boolean knowsExistingPassword = false;
            BuiltinUser oldBuiltInUser = builtinUserService.findByUserName(builtInUserToConvert.getUserIdentifier());
            if (oldBuiltInUser != null) {
                String usernameOfBuiltinAccountToConvert = oldBuiltInUser.getUserName();
                response.add("old username", usernameOfBuiltinAccountToConvert);
                AuthenticatedUser authenticatedUser = authSvc.canLogInAsBuiltinUser(usernameOfBuiltinAccountToConvert,
                        password);
                if (authenticatedUser != null) {
                    knowsExistingPassword = true;
                    AuthenticatedUser convertedUser = authSvc.convertBuiltInUserToRemoteUser(builtInUserToConvert,
                            newProviderId, newUserIdentifierInLookupTable);
                    if (convertedUser != null) {
                        /**
                         * @todo Display name is not being overwritten. Logic must be in Shib backing
                         *       bean
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
                /**
                 * @todo Someday we should make a errorResponse method that takes JSON arrays
                 *       and objects.
                 */
                return error(Status.BAD_REQUEST, problems.build().toString());
            }
            // response.add("knows existing password", knowsExistingPassword);
        }

        response.add("user to convert", builtInUserToConvert.getIdentifier());
        response.add("existing user found by email (prompt to convert)", existing);
        response.add("changing to this provider", newProviderId);
        response.add("value to overwrite old first name", overwriteFirstName);
        response.add("value to overwrite old last name", overwriteLastName);
        response.add("value to overwrite old email address", overwriteEmail);
        if (overwriteAffiliation != null) {
            response.add("affiliation", overwriteAffiliation);
        }
        response.add("problems", problems);
        return ok(response);
    }




    @Path("roles")
    @POST
    public Response createNewBuiltinRole(RoleDTO roleDto) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "createBuiltInRole")
                .setInfo(roleDto.getAlias() + ":" + roleDto.getDescription());
        try {
            return ok(json(rolesSvc.save(roleDto.asRole())));
        } catch (Exception e) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo(alr.getInfo() + "// " + e.getMessage());
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            actionLogSvc.log(alr);
        }
    }
    @Path("roles/{id}")
    @PUT
    public Response updateBuiltinRole(RoleDTO roleDto, @PathParam("id") long roleId) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "updateBuiltInRole")
                .setInfo(roleDto.getAlias() + ":" + roleDto.getDescription());
        try {
            DataverseRole role = roleDto.updateRoleFromDTO(rolesSvc.find(roleId));
            return ok(json(rolesSvc.save(role)));
        } catch (Exception e) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo(alr.getInfo() + "// " + e.getMessage());
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            actionLogSvc.log(alr);
        }
    }

    @Path("roles")
    @GET
    public Response listBuiltinRoles() {
        try {
            return ok(rolesToJson(rolesSvc.findBuiltinRoles()));
        } catch (Exception e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DELETE
    @AuthRequired
    @Path("roles/{id}")
    public Response deleteRole(@Context ContainerRequestContext crc, @PathParam("id") String id) {

        return response(req -> {
            DataverseRole doomed = findRoleOrDie(id);
            execCommand(new DeleteRoleCommand(req, doomed));
            return ok("role " + doomed.getName() + " deleted.");
        }, getRequestUser(crc));
    }

    @Path("superuser/{identifier}")
    @Deprecated
    @POST
    public Response toggleSuperuser(@PathParam("identifier") String identifier) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "toggleSuperuser")
                .setInfo(identifier);
        try {
            final AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
            return setSuperuserStatus(user, !user.isSuperuser());
        } catch (Exception e) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo(alr.getInfo() + "// " + e.getMessage());
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            actionLogSvc.log(alr);
        }
    }

    private Response setSuperuserStatus(AuthenticatedUser user, Boolean isSuperuser) {
        if (user.isDeactivated()) {
            return error(Status.BAD_REQUEST, "You cannot make a deactivated user a superuser.");
        }
        user.setSuperuser(isSuperuser);
        return ok("User " + user.getIdentifier() + " " + (user.isSuperuser() ? "set" : "removed")
                + " as a superuser.");
    }

    @Path("superuser/{identifier}")
    @PUT
    // Using string instead of boolean so user doesn't need to add a Content-type header in their request
    public Response setSuperuserStatus(@PathParam("identifier") String identifier, String isSuperuser) {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "setSuperuserStatus")
                .setInfo(identifier + ":" + isSuperuser);
        try {
            return setSuperuserStatus(authSvc.getAuthenticatedUser(identifier), StringUtil.isTrue(isSuperuser));
        } catch (Exception e) {
            alr.setActionResult(ActionLogRecord.Result.InternalError);
            alr.setInfo(alr.getInfo() + "// " + e.getMessage());
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            actionLogSvc.log(alr);
        }
    }

    @GET
    @Path("validate/datasets")
    @Produces({"application/json"})
    public Response validateAllDatasets(@QueryParam("variables") boolean includeVariables) {
        
        // Streaming output: the API will start producing 
        // the output right away, as it goes through the list 
        // of the datasets; there's potentially a lot of content 
        // to validate, so we don't want to wait for the process 
        // to finish. Or to wait to encounter the first invalid 
        // object - so we'll be reporting both the success and failure
        // outcomes for all the validated datasets, to give the user
        // an indication of the progress. 
        // This is the first streaming API that produces json that 
        // we have; there may be better ways to stream json - but 
        // what I have put together below works. -- L.A. 
        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException,
                    WebApplicationException {
                os.write("{\"datasets\": [\n".getBytes());
                
                boolean wroteObject = false;
                for (Long datasetId : datasetService.findAllLocalDatasetIds()) {
                    // Potentially, there's a godzillion datasets in this Dataverse. 
                    // This is why we go through the list of ids here, and instantiate 
                    // only one dataset at a time. 
                    boolean success = false;
                    boolean constraintViolationDetected = false;
                     
                    JsonObjectBuilder output = Json.createObjectBuilder();
                    output.add("datasetId", datasetId);

                    
                    try {
                        datasetService.instantiateDatasetInNewTransaction(datasetId, includeVariables);
                        success = true;
                    } catch (Exception ex) {
                        Throwable cause = ex;
                        while (cause != null) {
                            if (cause instanceof ConstraintViolationException) {
                                ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                                for (ConstraintViolation<?> constraintViolation : constraintViolationException
                                        .getConstraintViolations()) {
                                    String databaseRow = constraintViolation.getLeafBean().toString();
                                    String field = constraintViolation.getPropertyPath().toString();
                                    String invalidValue = null;
                                    if (constraintViolation.getInvalidValue() != null) {
                                        invalidValue = constraintViolation.getInvalidValue().toString();
                                    }
                                    output.add("status", "invalid");
                                    output.add("entityClassDatabaseTableRowId", databaseRow);
                                    output.add("field", field);
                                    output.add("invalidValue", invalidValue == null ? "NULL" : invalidValue);
                                    
                                    constraintViolationDetected = true; 
                                    
                                    break; 
                                    
                                }
                            }
                            cause = cause.getCause();
                        }
                    }
                    
                    
                    if (success) {
                        output.add("status", "valid");
                    } else if (!constraintViolationDetected) {
                        output.add("status", "unknown");
                    }
                    
                    // write it out:
                    
                    if (wroteObject) {
                        os.write(",\n".getBytes());
                    }

                    os.write(output.build().toString().getBytes(StandardCharsets.UTF_8));
                    
                    if (!wroteObject) {
                        wroteObject = true;
                    }
                }
                
                
                os.write("\n]\n}\n".getBytes());
            }
            
        };
        return Response.ok(stream).build();
    }
        
    @Path("validate/dataset/{id}")
    @GET
    public Response validateDataset(@PathParam("id") String id, @QueryParam("variables") boolean includeVariables) {
        Dataset dataset;
        try {
            dataset = findDatasetOrDie(id);
        } catch (Exception ex) {
            return error(Response.Status.NOT_FOUND, "No Such Dataset");
        }

        Long dbId = dataset.getId();

        String msg = "unknown";
        try {
            datasetService.instantiateDatasetInNewTransaction(dbId, includeVariables);
            msg = "valid";
        } catch (Exception ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> constraintViolation : constraintViolationException
                            .getConstraintViolations()) {
                        String databaseRow = constraintViolation.getLeafBean().toString();
                        String field = constraintViolation.getPropertyPath().toString();
                        String invalidValue = null; 
                        if (constraintViolation.getInvalidValue() != null) {
                            invalidValue = constraintViolation.getInvalidValue().toString();
                        }
                        JsonObjectBuilder violation = Json.createObjectBuilder();
                        violation.add("entityClassDatabaseTableRowId", databaseRow);
                        violation.add("field", field);
                        violation.add("invalidValue", invalidValue == null ? "NULL" : invalidValue);
                        return ok(violation);
                    }
                }
                cause = cause.getCause();
            }
        }
        return ok(msg);
    }
    
    // This API does the same thing as /validateDataFileHashValue/{fileId}, 
    // but for all the files in the dataset, with streaming output.
    @GET
    @Path("validate/dataset/files/{id}")
    @Produces({"application/json"})
    public Response validateDatasetDatafiles(@PathParam("id") String id) {
        
        // Streaming output: the API will start producing 
        // the output right away, as it goes through the list 
        // of the datafiles in the dataset.
        // The streaming mechanism is modeled after validate/datasets API.
        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException,
                    WebApplicationException {
                Dataset dataset;
        
                try {
                    dataset = findDatasetOrDie(id);
                } catch (Exception ex) {
                    throw new IOException(ex.getMessage());
                }
                
                os.write("{\"dataFiles\": [\n".getBytes());
                
                boolean wroteObject = false;
                for (DataFile dataFile : dataset.getFiles()) {
                    // Potentially, there's a godzillion datasets in this Dataverse. 
                    // This is why we go through the list of ids here, and instantiate 
                    // only one dataset at a time. 
                    boolean success = false;
                    boolean constraintViolationDetected = false;
                     
                    JsonObjectBuilder output = Json.createObjectBuilder();
                    output.add("datafileId", dataFile.getId());
                    output.add("storageIdentifier", dataFile.getStorageIdentifier());

                    
                    try {
                        FileUtil.validateDataFileChecksum(dataFile);
                        success = true;
                    } catch (IOException ex) {
                        output.add("status", "invalid");
                        output.add("errorMessage", ex.getMessage());
                    }
                    
                    if (success) {
                        output.add("status", "valid");
                    } 
                    
                    // write it out:
                    
                    if (wroteObject) {
                        os.write(",\n".getBytes());
                    }

                    os.write(output.build().toString().getBytes(StandardCharsets.UTF_8));
                    
                    if (!wroteObject) {
                        wroteObject = true;
                    }
                }
                
                os.write("\n]\n}\n".getBytes());
            }
            
        };
        return Response.ok(stream).build();
    }

    @Path("assignments/assignees/{raIdtf: .*}")
    @GET
    public Response getAssignmentsFor(@PathParam("raIdtf") String raIdtf) {

        JsonArrayBuilder arr = Json.createArrayBuilder();
        roleAssigneeSvc.getAssignmentsFor(raIdtf).forEach(a -> arr.add(json(a)));

        return ok(arr);
    }

    /**
     * This method is used in integration tests.
     *
     * @param userId
     *            The database id of an AuthenticatedUser.
     * @return The confirm email token.
     */
    @Path("confirmEmail/{userId}")
    @GET
    public Response getConfirmEmailToken(@PathParam("userId") long userId) {
        AuthenticatedUser user = authSvc.findByID(userId);
        if (user != null) {
            ConfirmEmailData confirmEmailData = confirmEmailSvc.findSingleConfirmEmailDataByUser(user);
            if (confirmEmailData != null) {
                return ok(Json.createObjectBuilder().add("token", confirmEmailData.getToken()));
            }
        }
        return error(Status.BAD_REQUEST, "Could not find confirm email token for user " + userId);
    }

    /**
     * This method is used in integration tests.
     *
     * @param userId
     *            The database id of an AuthenticatedUser.
     */
    @Path("confirmEmail/{userId}")
    @POST
    public Response startConfirmEmailProcess(@PathParam("userId") long userId) {
        AuthenticatedUser user = authSvc.findByID(userId);
        if (user != null) {
            try {
                ConfirmEmailInitResponse confirmEmailInitResponse = confirmEmailSvc.beginConfirm(user);
                ConfirmEmailData confirmEmailData = confirmEmailInitResponse.getConfirmEmailData();
                return ok(Json.createObjectBuilder().add("tokenCreated", confirmEmailData.getCreated().toString())
                        .add("identifier", user.getUserIdentifier()));
            } catch (ConfirmEmailException ex) {
                return error(Status.BAD_REQUEST,
                        "Could not start confirm email process for user " + userId + ": " + ex.getLocalizedMessage());
            }
        }
        return error(Status.BAD_REQUEST, "Could not find user based on " + userId);
    }

    /**
     * This method is used by an integration test in UsersIT.java to exercise bug
     * https://github.com/IQSS/dataverse/issues/3287 . Not for use by users!
     */
    @Path("convertUserFromBcryptToSha1")
    @POST
    public Response convertUserFromBcryptToSha1(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        BuiltinUser builtinUser = builtinUserService.find(new Long(object.getInt("builtinUserId")));
        builtinUser.updateEncryptedPassword("4G7xxL9z11/JKN4jHPn4g9iIQck=", 0); // password is "sha-1Pass", 0 means
                                                                                // SHA-1
        BuiltinUser savedUser = builtinUserService.save(builtinUser);
        return ok("foo: " + savedUser);

    }

    @Path("permissions/{dvo}")
    @AuthRequired
    @GET
    public Response findPermissonsOn(@Context final ContainerRequestContext crc, @PathParam("dvo") final String dvo) {
        try {
            final DvObject dvObj = findDvo(dvo);
            final User aUser = getRequestUser(crc);
            final JsonObjectBuilder bld = Json.createObjectBuilder();
            bld.add("user", aUser.getIdentifier());
            bld.add("permissions", json(permissionSvc.permissionsFor(createDataverseRequest(aUser), dvObj)));
            return ok(bld);
        } catch (WrappedResponse r) {
            return r.getResponse();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while testing permissions", e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Path("assignee/{idtf}")
    @GET
    public Response findRoleAssignee(@PathParam("idtf") String idtf) {
        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
        return (ra == null) ? notFound("Role Assignee '" + idtf + "' not found.") : ok(json(ra.getDisplayInfo()));
    }

    @Path("datasets/integrity/{datasetVersionId}/fixmissingunf")
    @POST
    public Response fixUnf(@PathParam("datasetVersionId") String datasetVersionId,
            @QueryParam("forceRecalculate") boolean forceRecalculate) {
        JsonObjectBuilder info = datasetVersionSvc.fixMissingUnf(datasetVersionId, forceRecalculate);
        return ok(info);
    }

    @Path("datafiles/integrity/fixmissingoriginaltypes")
    @GET
    public Response fixMissingOriginalTypes() {
        JsonObjectBuilder info = Json.createObjectBuilder();

        List<Long> affectedFileIds = fileService.selectFilesWithMissingOriginalTypes();

        if (affectedFileIds.isEmpty()) {
            info.add("message",
                    "All the tabular files in the database already have the original types set correctly; exiting.");
        } else {
            for (Long fileid : affectedFileIds) {
                logger.fine("found file id: " + fileid);
            }
            info.add("message", "Found " + affectedFileIds.size()
                    + " tabular files with missing original types. Kicking off an async job that will repair the files in the background.");
        }

        ingestService.fixMissingOriginalTypes(affectedFileIds);

        return ok(info);
    }
        
    @Path("datafiles/integrity/fixmissingoriginalsizes")
    @GET
    public Response fixMissingOriginalSizes(@QueryParam("limit") Integer limit) {
        JsonObjectBuilder info = Json.createObjectBuilder();

        List<Long> affectedFileIds = fileService.selectFilesWithMissingOriginalSizes();

        if (affectedFileIds.isEmpty()) {
            info.add("message",
                    "All the tabular files in the database already have the original sizes set correctly; exiting.");
        } else {
            
            int howmany = affectedFileIds.size();
            String message = "Found " + howmany + " tabular files with missing original sizes. "; 
            
            if (limit == null || howmany <= limit) {
                message = message.concat(" Kicking off an async job that will repair the files in the background.");
            } else {
                affectedFileIds.subList(limit, howmany-1).clear();
                message = message.concat(" Kicking off an async job that will repair the " + limit + " files in the background.");                        
            }
            info.add("message", message);
        }

        ingestService.fixMissingOriginalSizes(affectedFileIds);
        return ok(info);
    }

    /**
     * This method is used in API tests, called from UtilIt.java.
     */
    @GET
    @Path("datasets/thumbnailMetadata/{id}")
    public Response getDatasetThumbnailMetadata(@PathParam("id") Long idSupplied) {
        Dataset dataset = datasetSvc.find(idSupplied);
        if (dataset == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataset based on id supplied: " + idSupplied + ".");
        }
        JsonObjectBuilder data = Json.createObjectBuilder();
        DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail(ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
        data.add("isUseGenericThumbnail", dataset.isUseGenericThumbnail());
        data.add("datasetLogoPresent", DatasetUtil.isDatasetLogoPresent(dataset, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE));
        if (datasetThumbnail != null) {
            data.add("datasetThumbnailBase64image", datasetThumbnail.getBase64image());
            DataFile dataFile = datasetThumbnail.getDataFile();
            if (dataFile != null) {
                /**
                 * @todo Change this from a String to a long.
                 */
                data.add("dataFileId", dataFile.getId().toString());
            }
        }
        return ok(data);
    }

    /**
     * validatePassword
     * <p>
     * Validate a password with an API call
     *
     * @param password
     *            The password
     * @return A response with the validation result.
     */
    @Path("validatePassword")
    @POST
    public Response validatePassword(String password) {

        final List<String> errors = passwordValidatorService.validate(password, new Date(), false);
        final JsonArrayBuilder errorArray = Json.createArrayBuilder();
        errors.forEach(errorArray::add);
        return ok(Json.createObjectBuilder().add("password", password).add("errors", errorArray));
    }

    @GET
    @Path("/isOrcid")
    public Response isOrcidEnabled() {
        return authSvc.isOrcidEnabled() ? ok("Orcid is enabled") : ok("no orcid for you.");
    }

    @POST
    @AuthRequired
    @Path("{id}/reregisterHDLToPID")
    public Response reregisterHdlToPID(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        logger.info("Starting to reregister  " + id + " Dataset Id. (from hdl to doi)" + new Date());
        try {

            
            User u = getRequestUser(crc);
            if (!u.isSuperuser()) {
                logger.info("Bad Request Unauthor " );
                return error(Status.UNAUTHORIZED, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
            }

            DataverseRequest r = createDataverseRequest(u);
            Dataset ds = findDatasetOrDie(id);
            
            if (HandlePidProvider.HDL_PROTOCOL.equals(dvObjectService.getEffectivePidGenerator(ds).getProtocol())) {
                logger.info("Bad Request protocol set to handle  " );
                return error(Status.BAD_REQUEST, BundleUtil.getStringFromBundle("admin.api.migrateHDL.failure.must.be.set.for.doi"));
            }
            if (ds.getIdentifier() != null && !ds.getIdentifier().isEmpty() && ds.getProtocol().equals(HandlePidProvider.HDL_PROTOCOL)) {
                execCommand(new RegisterDvObjectCommand(r, ds, true));
            } else {
                return error(Status.BAD_REQUEST, BundleUtil.getStringFromBundle("admin.api.migrateHDL.failure.must.be.hdl.dataset"));
            }

        } catch (WrappedResponse r) {
            logger.info("Failed to migrate Dataset Handle id: " + id);
            return badRequest(BundleUtil.getStringFromBundle("admin.api.migrateHDL.failure", Arrays.asList(id)));
        } catch (Exception e) {
            logger.info("Failed to migrate Dataset Handle id: " + id + " Unexpected Exception " + e.getMessage());
            List<String> args = Arrays.asList(id,e.getMessage());
            return badRequest(BundleUtil.getStringFromBundle("admin.api.migrateHDL.failureWithException", args));
        }
        
        return ok(BundleUtil.getStringFromBundle("admin.api.migrateHDL.success"));
    }

    @GET
    @AuthRequired
    @Path("{id}/registerDataFile")
    public Response registerDataFile(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        logger.info("Starting to register  " + id + " file id. " + new Date());

        try {
            User u = getRequestUser(crc);
            DataverseRequest r = createDataverseRequest(u);
            DataFile df = findDataFileOrDie(id);
            if(!systemConfig.isFilePIDsEnabledForCollection(df.getOwner().getOwner())) {
                return forbidden("PIDs are not enabled for this file's collection.");
            }
            if (df.getIdentifier() == null || df.getIdentifier().isEmpty()) {
                execCommand(new RegisterDvObjectCommand(r, df));
            } else {
                return ok("File was already registered. ");
            }

        } catch (WrappedResponse r) {
            logger.info("Failed to register file id: " + id);
        } catch (Exception e) {
            logger.info("Failed to register file id: " + id + " Unexpecgted Exception " + e.getMessage());
        }
        return ok("Datafile registration complete. File registered successfully.");
    }

    @GET
    @AuthRequired
    @Path("/registerDataFileAll")
    public Response registerDataFileAll(@Context ContainerRequestContext crc) {
        Integer count = fileService.findAll().size();
        Integer successes = 0;
        Integer alreadyRegistered = 0;
        Integer released = 0;
        Integer draft = 0;
        Integer skipped = 0;
        logger.info("Starting to register: analyzing " + count + " files. " + new Date());
        logger.info("Only unregistered, published files will be registered.");
        User u = null;
        try {
            u = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse e1) {
            return error(Status.UNAUTHORIZED, "api key required");
        }
        DataverseRequest r = createDataverseRequest(u);
        for (DataFile df : fileService.findAll()) {
            try {
                if ((df.getIdentifier() == null || df.getIdentifier().isEmpty())) {
                    if(!systemConfig.isFilePIDsEnabledForCollection(df.getOwner().getOwner())) {
                        skipped++;
                        if (skipped % 100 == 0) {
                            logger.info(skipped + " of  " + count + " files not in collections that allow file PIDs. " + new Date());
                        }
                    } else if (df.isReleased()) {
                        released++;
                        execCommand(new RegisterDvObjectCommand(r, df));
                        successes++;
                        if (successes % 100 == 0) {
                            logger.info(successes + " of  " + count + " files registered successfully. " + new Date());
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            logger.warning("Interrupted Exception when attempting to execute Thread.sleep()!");
                        }
                    } else {
                        draft++;
                        if (draft % 100 == 0) {
                          logger.info(draft + " of  " + count + " files not yet published");
                        }
                    }
                } else {
                    alreadyRegistered++;
                    if(alreadyRegistered % 100 == 0) {
                      logger.info(alreadyRegistered + " of  " + count + " files are already registered. " + new Date());
                    }
                }
            } catch (WrappedResponse ex) {
                logger.info("Failed to register file id: " + df.getId());
                Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
                logger.info("Unexpected Exception: " + e.getMessage());
            }
            

        }
        logger.info("Final Results:");
        logger.info(alreadyRegistered + " of  " + count + " files were already registered. " + new Date());
        logger.info(draft + " of  " + count + " files are not yet published. " + new Date());
        logger.info(released + " of  " + count + " unregistered, published files to register. " + new Date());
        logger.info(successes + " of  " + released + " unregistered, published files registered successfully. "
                + new Date());
        logger.info(skipped + " of  " + count + " files not in collections that allow file PIDs. " + new Date());

        return ok("Datafile registration complete." + successes + " of  " + released
                + " unregistered, published files registered successfully.");
    }
    
    @GET
    @AuthRequired
    @Path("/registerDataFiles/{alias}")
    public Response registerDataFilesInCollection(@Context ContainerRequestContext crc, @PathParam("alias") String alias, @QueryParam("sleep") Integer sleepInterval) {
        Dataverse collection;
        try {
            collection = findDataverseOrDie(alias);
        } catch (WrappedResponse r) {
            return r.getResponse();
        }
        
        AuthenticatedUser superuser = authSvc.getAdminUser();
        if (superuser == null) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Cannot find the superuser to execute /admin/registerDataFiles.");
        }
        
        if (!systemConfig.isFilePIDsEnabledForCollection(collection)) {
            return ok("Registration of file-level pid is disabled in collection "+alias+"; nothing to do");
        }
        
        List<DataFile> dataFiles = fileService.findByDirectCollectionOwner(collection.getId());
        Integer count = dataFiles.size();
        Integer countSuccesses = 0;
        Integer countAlreadyRegistered = 0;
        Integer countReleased = 0;
        Integer countDrafts = 0;
        
        if (sleepInterval == null) {
            sleepInterval = 1; 
        } else if (sleepInterval.intValue() < 1) {
            return error(Response.Status.BAD_REQUEST, "Invalid sleep interval: "+sleepInterval);
        }
        
        logger.info("Starting to register: analyzing " + count + " files. " + new Date());
        logger.info("Only unregistered, published files will be registered.");
        
        
        
        for (DataFile df : dataFiles) {
            try {
                if ((df.getIdentifier() == null || df.getIdentifier().isEmpty())) {
                    if (df.isReleased()) {
                        countReleased++;
                        DataverseRequest r = createDataverseRequest(superuser);
                        execCommand(new RegisterDvObjectCommand(r, df));
                        countSuccesses++;
                        if (countSuccesses % 100 == 0) {
                            logger.info(countSuccesses + " out of " + count + " files registered successfully. " + new Date());
                        }
                        try {
                            Thread.sleep(sleepInterval * 1000);
                        } catch (InterruptedException ie) {
                            logger.warning("Interrupted Exception when attempting to execute Thread.sleep()!");
                        }
                    } else {
                        countDrafts++;
                        logger.fine(countDrafts + " out of " + count + " files not yet published");
                    }
                } else {
                    countAlreadyRegistered++;
                    logger.fine(countAlreadyRegistered + " out of " + count + " files are already registered. " + new Date());
                }
            } catch (WrappedResponse ex) {
                countReleased++;
                logger.info("Failed to register file id: " + df.getId());
                Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
                logger.info("Unexpected Exception: " + e.getMessage());
            }
        }
        
        logger.info(countAlreadyRegistered + " out of " + count + " files were already registered. " + new Date());
        logger.info(countDrafts + " out of " + count + " files are not yet published. " + new Date());
        logger.info(countReleased + " out of " + count + " unregistered, published files to register. " + new Date());
        logger.info(countSuccesses + " out of " + countReleased + " unregistered, published files registered successfully. "
                + new Date());

        return ok("Datafile registration complete. " + countSuccesses + " out of " + countReleased
                + " unregistered, published files registered successfully.");
    }

    @GET
    @AuthRequired
    @Path("/updateHashValues/{alg}")
    public Response updateHashValues(@Context ContainerRequestContext crc, @PathParam("alg") String alg, @QueryParam("num") int num) {
        Integer count = fileService.findAll().size();
        Integer successes = 0;
        Integer alreadyUpdated = 0;
        Integer rehashed = 0;
        Integer harvested = 0;

        if (num <= 0)
            num = Integer.MAX_VALUE;
        DataFile.ChecksumType cType = null;
        try {
            cType = DataFile.ChecksumType.fromString(alg);
        } catch (IllegalArgumentException iae) {
            return error(Status.BAD_REQUEST, "Unknown algorithm");
        }
        logger.info("Starting to rehash: analyzing " + count + " files. " + new Date());
        logger.info("Hashes not created with " + alg + " will be verified, and, if valid, replaced with a hash using "
                + alg);
        try {
            User u = getRequestAuthenticatedUserOrDie(crc);
            if (!u.isSuperuser())
                return error(Status.UNAUTHORIZED, "must be superuser");
        } catch (WrappedResponse e1) {
            return error(Status.UNAUTHORIZED, "api key required");
        }

        for (DataFile df : fileService.findAll()) {
            if (rehashed.intValue() >= num)
                break;
            InputStream in = null;
            InputStream in2 = null;
            try {
                if (df.isHarvested()) {
                    harvested++;
                } else {
                    if (!df.getChecksumType().equals(cType)) {

                        rehashed++;
                        logger.fine(rehashed + ": Datafile: " + df.getFileMetadata().getLabel() + ", "
                                + df.getIdentifier());
                        // verify hash and calc new one to replace it
                        StorageIO<DataFile> storage = df.getStorageIO();
                        storage.open(DataAccessOption.READ_ACCESS);
                        if (!df.isTabularData()) {
                            in = storage.getInputStream();
                        } else {
                            // if this is a tabular file, read the preserved original "auxiliary file"
                            // instead:
                            in = storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                        }
                        if (in == null)
                            logger.warning("Cannot retrieve file.");
                        String currentChecksum = FileUtil.calculateChecksum(in, df.getChecksumType());
                        if (currentChecksum.equals(df.getChecksumValue())) {
                            logger.fine("Current checksum for datafile: " + df.getFileMetadata().getLabel() + ", "
                                    + df.getIdentifier() + " is valid");
                            // Need to reset so we don't get the same stream (StorageIO class inputstreams
                            // are normally only used once)
                            storage.setInputStream(null);
                            storage.open(DataAccessOption.READ_ACCESS);
                            if (!df.isTabularData()) {
                                in2 = storage.getInputStream();
                            } else {
                                // if this is a tabular file, read the preserved original "auxiliary file"
                                // instead:
                                in2 = storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                            }
                            if (in2 == null)
                                logger.warning("Cannot retrieve file to calculate new checksum.");
                            String newChecksum = FileUtil.calculateChecksum(in2, cType);

                            df.setChecksumType(cType);
                            df.setChecksumValue(newChecksum);
                            successes++;
                            if (successes % 100 == 0) {
                                logger.info(
                                        successes + " of  " + count + " files rehashed successfully. " + new Date());
                            }
                        } else {
                            logger.warning("Problem: Current checksum for datafile: " + df.getFileMetadata().getLabel()
                                    + ", " + df.getIdentifier() + " is INVALID");
                        }
                    } else {
                        alreadyUpdated++;
                        if (alreadyUpdated % 100 == 0) {
                            logger.info(alreadyUpdated + " of  " + count
                                    + " files are already have hashes with the new algorithm. " + new Date());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning("Unexpected Exception: " + e.getMessage());

            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(in2);
            }
        }
        logger.info("Final Results:");
        logger.info(harvested + " harvested files skipped.");
        logger.info(
                alreadyUpdated + " of  " + count + " files already had hashes with the new algorithm. " + new Date());
        logger.info(rehashed + " of  " + count + " files to rehash. " + new Date());
        logger.info(
                successes + " of  " + rehashed + " files successfully rehashed with the new algorithm. " + new Date());

        return ok("Datafile rehashing complete." + successes + " of  " + rehashed + " files successfully rehashed.");
    }
        
    @POST
    @AuthRequired
    @Path("/computeDataFileHashValue/{fileId}/algorithm/{alg}")
    public Response computeDataFileHashValue(@Context ContainerRequestContext crc, @PathParam("fileId") String fileId, @PathParam("alg") String alg) {

        try {
            User u = getRequestAuthenticatedUserOrDie(crc);
            if (!u.isSuperuser()) {
                return error(Status.UNAUTHORIZED, "must be superuser");
            }
        } catch (WrappedResponse e1) {
            return error(Status.UNAUTHORIZED, "api key required");
        }

        DataFile fileToUpdate = null;
        try {
            fileToUpdate = findDataFileOrDie(fileId);
        } catch (WrappedResponse r) {
            logger.info("Could not find file with the id: " + fileId);
            return error(Status.BAD_REQUEST, "Could not find file with the id: " + fileId);
        }

        if (fileToUpdate.isHarvested()) {
            return error(Status.BAD_REQUEST, "File with the id: " + fileId + " is harvested.");
        }

        DataFile.ChecksumType cType = null;
        try {
            cType = DataFile.ChecksumType.fromString(alg);
        } catch (IllegalArgumentException iae) {
            return error(Status.BAD_REQUEST, "Unknown algorithm: " + alg);
        }

        String newChecksum = "";

        InputStream in = null;
        try {

            StorageIO<DataFile> storage = fileToUpdate.getStorageIO();
            storage.open(DataAccessOption.READ_ACCESS);
            if (!fileToUpdate.isTabularData()) {
                in = storage.getInputStream();
            } else {
                in = storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
            }
            if (in == null) {
                return error(Status.NOT_FOUND, "Could not retrieve file with the id: " + fileId);
            }
            newChecksum = FileUtil.calculateChecksum(in, cType);
            fileToUpdate.setChecksumType(cType);
            fileToUpdate.setChecksumValue(newChecksum);

        } catch (Exception e) {
            logger.warning("Unexpected Exception: " + e.getMessage());

        } finally {
            IOUtils.closeQuietly(in);
        }

        return ok("Datafile rehashing complete. " + fileId + "  successfully rehashed. New hash value is: " + newChecksum);
    }
    
    @POST
    @AuthRequired
    @Path("/validateDataFileHashValue/{fileId}")
    public Response validateDataFileHashValue(@Context ContainerRequestContext crc, @PathParam("fileId") String fileId) {

        try {
            User u = getRequestAuthenticatedUserOrDie(crc);
            if (!u.isSuperuser()) {
                return error(Status.UNAUTHORIZED, "must be superuser");
            }
        } catch (WrappedResponse e1) {
            return error(Status.UNAUTHORIZED, "api key required");
        }

        DataFile fileToValidate = null;
        try {
            fileToValidate = findDataFileOrDie(fileId);
        } catch (WrappedResponse r) {
            logger.info("Could not find file with the id: " + fileId);
            return error(Status.BAD_REQUEST, "Could not find file with the id: " + fileId);
        }

        if (fileToValidate.isHarvested()) {
            return error(Status.BAD_REQUEST, "File with the id: " + fileId + " is harvested.");
        }

        DataFile.ChecksumType cType = null;
        try {
            String checkSumTypeFromDataFile = fileToValidate.getChecksumType().toString();
            cType = DataFile.ChecksumType.fromString(checkSumTypeFromDataFile);
        } catch (IllegalArgumentException iae) {
            return error(Status.BAD_REQUEST, "Unknown algorithm");
        }

        String currentChecksum = fileToValidate.getChecksumValue();
        String calculatedChecksum = "";
        InputStream in = null;
        try {

            StorageIO<DataFile> storage = fileToValidate.getStorageIO();
            storage.open(DataAccessOption.READ_ACCESS);
            if (!fileToValidate.isTabularData()) {
                in = storage.getInputStream();
            } else {
                in = storage.getAuxFileAsInputStream(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
            }
            if (in == null) {
                return error(Status.NOT_FOUND, "Could not retrieve file with the id: " + fileId);
            }
            calculatedChecksum = FileUtil.calculateChecksum(in, cType);

        } catch (Exception e) {
            logger.warning("Unexpected Exception: " + e.getMessage());
            return error(Status.BAD_REQUEST, "Checksum Validation Unexpected Exception: " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(in);

        }

        if (currentChecksum.equals(calculatedChecksum)) {
            return ok("Datafile validation complete for " + fileId + ". The hash value is: " + calculatedChecksum);
        } else {
            return error(Status.EXPECTATION_FAILED, "Datafile validation failed for " + fileId + ". The saved hash value is: " + currentChecksum + " while the recalculated hash value for the stored file is: " + calculatedChecksum);
        }

    }

    @POST
    @AuthRequired
    @Path("/submitDatasetVersionToArchive/{id}/{version}")
    public Response submitDatasetVersionToArchive(@Context ContainerRequestContext crc, @PathParam("id") String dsid,
            @PathParam("version") String versionNumber) {

        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);

            Dataset ds = findDatasetOrDie(dsid);

            DatasetVersion dv = datasetversionService.findByFriendlyVersionNumber(ds.getId(), versionNumber);
            if(dv==null) {
                return error(Status.BAD_REQUEST, "Requested version not found.");
            }
            if (dv.getArchivalCopyLocation() == null) {
                String className = settingsService.getValueForKey(SettingsServiceBean.Key.ArchiverClassName);
                // Note - the user is being sent via the createDataverseRequest(au) call to the
                // back-end command where it is used to get the API Token which is
                // then used to retrieve files (e.g. via S3 direct downloads) to create the Bag
                AbstractSubmitToArchiveCommand cmd = ArchiverUtil.createSubmitToArchiveCommand(className,
                        createDataverseRequest(au), dv);
                // createSubmitToArchiveCommand() tries to find and instantiate an non-abstract
                // implementation of AbstractSubmitToArchiveCommand based on the provided
                // className. If a class with that name isn't found (or can't be instatiated), it
                // will return null
                if (cmd != null) {
                    if(ArchiverUtil.onlySingleVersionArchiving(cmd.getClass(), settingsService)) {
                        for (DatasetVersion version : ds.getVersions()) {
                            if ((dv != version) && version.getArchivalCopyLocation() != null) {
                                return error(Status.CONFLICT, "Dataset already archived.");
                            }
                        } 
                    }
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                DatasetVersion dv = commandEngine.submit(cmd);
                                if (!dv.getArchivalCopyLocationStatus().equals(DatasetVersion.ARCHIVAL_STATUS_FAILURE)) {
                                    logger.info(
                                            "DatasetVersion id=" + ds.getGlobalId().toString() + " v" + versionNumber
                                                    + " submitted to Archive, status: " + dv.getArchivalCopyLocationStatus());
                                } else {
                                    logger.severe("Error submitting version due to conflict/error at Archive");
                                }
                            } catch (CommandException ex) {
                                logger.log(Level.SEVERE, "Unexpected Exception calling  submit archive command", ex);
                            }
                        }
                    }).start();
                    return ok("Archive submission using " + cmd.getClass().getCanonicalName()
                            + " started. Processing can take significant time for large datasets and requires that the user have permission to publish the dataset. View log and/or check archive for results.");
                } else {
                    logger.log(Level.SEVERE, "Could not find Archiver class: " + className);
                    return error(Status.INTERNAL_SERVER_ERROR, "Could not find Archiver class: " + className);
                }
            } else {
                return error(Status.BAD_REQUEST, "Version was already submitted for archiving.");
            }
        } catch (WrappedResponse e1) {
            return e1.getResponse();
        }
    }

    
    /**
     * Iteratively archives all unarchived dataset versions
     * @param
     * listonly - don't archive, just list unarchived versions
     * limit - max number to process
     * lastestonly - only archive the latest versions
     * @return
     */
    @POST
    @AuthRequired
    @Path("/archiveAllUnarchivedDatasetVersions")
    public Response archiveAllUnarchivedDatasetVersions(@Context ContainerRequestContext crc, @QueryParam("listonly") boolean listonly, @QueryParam("limit") Integer limit, @QueryParam("latestonly") boolean latestonly) {

        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);

            List<DatasetVersion> dsl = datasetversionService.getUnarchivedDatasetVersions();
            if (dsl != null) {
                if (listonly) {
                    JsonArrayBuilder jab = Json.createArrayBuilder();
                    logger.fine("Unarchived versions found: ");
                    int current = 0;
                    for (DatasetVersion dv : dsl) {
                        if (limit != null && current >= limit) {
                            break;
                        }
                        if (!latestonly || dv.equals(dv.getDataset().getLatestVersionForCopy())) {
                            jab.add(dv.getDataset().getGlobalId().toString() + ", v" + dv.getFriendlyVersionNumber());
                            logger.fine("    " + dv.getDataset().getGlobalId().toString() + ", v" + dv.getFriendlyVersionNumber());
                            current++;
                        }
                    }
                    return ok(jab); 
                }
                String className = settingsService.getValueForKey(SettingsServiceBean.Key.ArchiverClassName);
                // Note - the user is being sent via the createDataverseRequest(au) call to the
                // back-end command where it is used to get the API Token which is
                // then used to retrieve files (e.g. via S3 direct downloads) to create the Bag
                final DataverseRequest request = createDataverseRequest(au);
                // createSubmitToArchiveCommand() tries to find and instantiate an non-abstract
                // implementation of AbstractSubmitToArchiveCommand based on the provided
                // className. If a class with that name isn't found (or can't be instatiated, it
                // will return null
                AbstractSubmitToArchiveCommand cmd = ArchiverUtil.createSubmitToArchiveCommand(className, request, dsl.get(0));
                if (cmd != null) {
                    //Found an archiver to use
                    new Thread(new Runnable() {
                        public void run() {
                            int total = dsl.size();
                            int successes = 0;
                            int failures = 0;
                            for (DatasetVersion dv : dsl) {
                                if (limit != null && (successes + failures) >= limit) {
                                    break;
                                }
                                if (!latestonly || dv.equals(dv.getDataset().getLatestVersionForCopy())) {
                                    try {
                                        AbstractSubmitToArchiveCommand cmd = ArchiverUtil.createSubmitToArchiveCommand(className, request, dv);

                                        dv = commandEngine.submit(cmd);
                                        if (!dv.getArchivalCopyLocationStatus().equals(DatasetVersion.ARCHIVAL_STATUS_FAILURE)) {
                                            successes++;
                                            logger.info("DatasetVersion id=" + dv.getDataset().getGlobalId().toString() + " v" + dv.getFriendlyVersionNumber() + " submitted to Archive, status: "
                                                    + dv.getArchivalCopyLocationStatus());
                                        } else {
                                            failures++;
                                            logger.severe("Error submitting version due to conflict/error at Archive for " + dv.getDataset().getGlobalId().toString() + " v" + dv.getFriendlyVersionNumber());
                                        }
                                    } catch (CommandException ex) {
                                        failures++;
                                        logger.log(Level.SEVERE, "Unexpected Exception calling  submit archive command", ex);
                                    }
                                }
                                logger.fine(successes + failures + " of " + total + " archive submissions complete");
                            }
                            logger.info("Archiving complete: " + successes + " Successes, " + failures + " Failures. See prior log messages for details.");
                        }
                    }).start();
                    return ok("Starting to archive all unarchived published dataset versions using " + cmd.getClass().getCanonicalName() + ". Processing can take significant time for large datasets/ large numbers of dataset versions  and requires that the user have permission to publish the dataset(s). View log and/or check archive for results.");
                } else {
                    logger.log(Level.SEVERE, "Could not find Archiver class: " + className);
                    return error(Status.INTERNAL_SERVER_ERROR, "Could not find Archiver class: " + className);
                }
            } else {
                return error(Status.BAD_REQUEST, "No unarchived published dataset versions found");
            }
        } catch (WrappedResponse e1) {
            return e1.getResponse();
        }
    }
    
    @DELETE
    @Path("/clearMetricsCache")
    public Response clearMetricsCache() {
        em.createNativeQuery("DELETE FROM metric").executeUpdate();
        return ok("all metric caches cleared.");
    }

    @DELETE
    @Path("/clearMetricsCache/{name}")
    public Response clearMetricsCacheByName(@PathParam("name") String name) {
        Query deleteQuery = em.createNativeQuery("DELETE FROM metric where name = ?");
        deleteQuery.setParameter(1, name);
        deleteQuery.executeUpdate();
        return ok("metric cache " + name + " cleared.");
    }

    @GET
    @AuthRequired
    @Path("/dataverse/{alias}/addRoleAssignmentsToChildren")
    public Response addRoleAssignementsToChildren(@Context ContainerRequestContext crc, @PathParam("alias") String alias) throws WrappedResponse {
        Dataverse owner = dataverseSvc.findByAlias(alias);
        if (owner == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        boolean inheritAllRoles = false;
        String rolesString = settingsSvc.getValueForKey(SettingsServiceBean.Key.InheritParentRoleAssignments, "");
        if (rolesString.length() > 0) {
            ArrayList<String> rolesToInherit = new ArrayList<String>(Arrays.asList(rolesString.split("\\s*,\\s*")));
            if (!rolesToInherit.isEmpty()) {
                if (rolesToInherit.contains("*")) {
                    inheritAllRoles = true;
                }
                return ok(dataverseSvc.addRoleAssignmentsToChildren(owner, rolesToInherit, inheritAllRoles));
            }
        }
        return error(Response.Status.BAD_REQUEST,
                "InheritParentRoleAssignments does not list any roles on this instance");
    }
    
    @GET
    @AuthRequired
    @Path("/dataverse/{alias}/storageDriver")
    public Response getStorageDriver(@Context ContainerRequestContext crc, @PathParam("alias") String alias) throws WrappedResponse {
        Dataverse dataverse = dataverseSvc.findByAlias(alias);
        if (dataverse == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        //Note that this returns what's set directly on this dataverse. If null/DataAccess.UNDEFINED_STORAGE_DRIVER_IDENTIFIER, the user would have to recurse the chain of parents to find the effective storageDriver
        return ok(dataverse.getStorageDriverId());
    }
    
    @PUT
    @AuthRequired
    @Path("/dataverse/{alias}/storageDriver")
    public Response setStorageDriver(@Context ContainerRequestContext crc, @PathParam("alias") String alias, String label) throws WrappedResponse {
        Dataverse dataverse = dataverseSvc.findByAlias(alias);
        if (dataverse == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        for (Entry<String, String> store: DataAccess.getStorageDriverLabels().entrySet()) {
            if(store.getKey().equals(label)) {
                dataverse.setStorageDriverId(store.getValue());
                return ok("Storage set to: " + store.getKey() + "/" + store.getValue());
            }
        }
        return error(Response.Status.BAD_REQUEST,
                "No Storage Driver found for : " + label);
    }

    @DELETE
    @AuthRequired
    @Path("/dataverse/{alias}/storageDriver")
    public Response resetStorageDriver(@Context ContainerRequestContext crc, @PathParam("alias") String alias) throws WrappedResponse {
        Dataverse dataverse = dataverseSvc.findByAlias(alias);
        if (dataverse == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        dataverse.setStorageDriverId("");
        return ok("Storage reset to default: " + DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
    }
    
    @GET
    @AuthRequired
    @Path("/dataverse/storageDrivers")
    public Response listStorageDrivers(@Context ContainerRequestContext crc) throws WrappedResponse {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        JsonObjectBuilder bld = jsonObjectBuilder();
        DataAccess.getStorageDriverLabels().entrySet().forEach(s -> bld.add(s.getKey(), s.getValue()));
        return ok(bld);
    }
    
    @GET
    @AuthRequired
    @Path("/dataverse/{alias}/curationLabelSet")
    public Response getCurationLabelSet(@Context ContainerRequestContext crc, @PathParam("alias") String alias) throws WrappedResponse {
        Dataverse dataverse = dataverseSvc.findByAlias(alias);
        if (dataverse == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        // Note that this returns what's set directly on this dataverse. If
        // null/SystemConfig.DEFAULTCURATIONLABELSET, the user would have to recurse the
        // chain of parents to find the effective curationLabelSet
        return ok(dataverse.getCurationLabelSetName());
    }

    @PUT
    @AuthRequired
    @Path("/dataverse/{alias}/curationLabelSet")
    public Response setCurationLabelSet(@Context ContainerRequestContext crc, @PathParam("alias") String alias, @QueryParam("name") String name) throws WrappedResponse {
        Dataverse dataverse = dataverseSvc.findByAlias(alias);
        if (dataverse == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (SystemConfig.CURATIONLABELSDISABLED.equals(name) || SystemConfig.DEFAULTCURATIONLABELSET.equals(name)) {
            dataverse.setCurationLabelSetName(name);
            return ok("Curation Label Set Name set to: " + name);
        } else {
            for (String setName : systemConfig.getCurationLabels().keySet()) {
                if (setName.equals(name)) {
                    dataverse.setCurationLabelSetName(name);
                    return ok("Curation Label Set Name set to: " + setName);
                }
            }
        }
        return error(Response.Status.BAD_REQUEST,
                "No Curation Label Set found for : " + name);
    }

    @DELETE
    @AuthRequired
    @Path("/dataverse/{alias}/curationLabelSet")
    public Response resetCurationLabelSet(@Context ContainerRequestContext crc, @PathParam("alias") String alias) throws WrappedResponse {
        Dataverse dataverse = dataverseSvc.findByAlias(alias);
        if (dataverse == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        dataverse.setCurationLabelSetName(SystemConfig.DEFAULTCURATIONLABELSET);
        return ok("Curation Label Set reset to default: " + SystemConfig.DEFAULTCURATIONLABELSET);
    }

    @GET
    @AuthRequired
    @Path("/dataverse/curationLabelSets")
    public Response listCurationLabelSets(@Context ContainerRequestContext crc) throws WrappedResponse {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        JsonObjectBuilder bld = Json.createObjectBuilder();

        systemConfig.getCurationLabels().entrySet().forEach(s -> {
            JsonArrayBuilder labels = Json.createArrayBuilder();
            Arrays.asList(s.getValue()).forEach(l -> labels.add(l));
            bld.add(s.getKey(), labels);
        });
        return ok(bld);
    }
    
    @POST
    @Path("/bannerMessage")
    public Response addBannerMessage(JsonObject jsonObject) throws WrappedResponse {

        BannerMessage toAdd = new BannerMessage();
        try {

            String dismissible = jsonObject.getString("dismissibleByUser");

            boolean dismissibleByUser = false;
            if (dismissible.equals("true")) {
                dismissibleByUser = true;
            }
            toAdd.setDismissibleByUser(dismissibleByUser);
            toAdd.setBannerMessageTexts(new ArrayList());
            toAdd.setActive(true);
            JsonArray jsonArray = jsonObject.getJsonArray("messageTexts");
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject obj = (JsonObject) jsonArray.get(i);
                String message = obj.getString("message");
                String lang = obj.getString("lang");
                BannerMessageText messageText = new BannerMessageText();
                messageText.setMessage(message);
                messageText.setLang(lang);
                messageText.setBannerMessage(toAdd);
                toAdd.getBannerMessageTexts().add(messageText);
            }
            bannerMessageService.save(toAdd);

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("message", "Banner Message added successfully.")
                .add("id", toAdd.getId());

            return ok(jsonObjectBuilder);

        } catch (Exception e) {
            logger.warning("Unexpected Exception: " + e.getMessage());
            return error(Status.BAD_REQUEST, "Add Banner Message unexpected exception: invalid or missing JSON object.");
        }

    }
    
    @DELETE
    @Path("/bannerMessage/{id}")
    public Response deleteBannerMessage(@PathParam("id") Long id) throws WrappedResponse {
 
        BannerMessage message = em.find(BannerMessage.class, id);
        if (message == null){
            return error(Response.Status.NOT_FOUND, "Message id = "  + id + " not found.");
        }
        bannerMessageService.deleteBannerMessage(id);
        
        return ok("Message id =  " + id + " deleted.");

    }
    
    @PUT
    @Path("/bannerMessage/{id}/deactivate")
    public Response deactivateBannerMessage(@PathParam("id") Long id) throws WrappedResponse {
        BannerMessage message = em.find(BannerMessage.class, id);
        if (message == null){
            return error(Response.Status.NOT_FOUND, "Message id = "  + id + " not found.");
        }
        bannerMessageService.deactivateBannerMessage(id);
        
        return ok("Message id =  " + id + " deactivated.");

    }
    
    @GET
    @Path("/bannerMessage")
    public Response getBannerMessages(@PathParam("id") Long id) throws WrappedResponse {

        List<BannerMessage> messagesList = bannerMessageService.findAllBannerMessages();

        for (BannerMessage message : messagesList) {
            if ("".equals(message.getDisplayValue())) {
               return error(Response.Status.INTERNAL_SERVER_ERROR, "No banner messages found for this locale.");
            }
        }

        JsonArrayBuilder messages = messagesList.stream()
        .map(m -> jsonObjectBuilder().add("id", m.getId()).add("displayValue", m.getDisplayValue()))
        .collect(toJsonArray());
        
        return ok(messages);
    }
    
    @POST
    @AuthRequired
    @Consumes("application/json")
    @Path("/requestSignedUrl")
    public Response getSignedUrl(@Context ContainerRequestContext crc, JsonObject urlInfo) {
        AuthenticatedUser superuser = null;
        try {
            superuser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (superuser == null || !superuser.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Requesting signed URLs is restricted to superusers.");
        }
        
        String userId = urlInfo.getString("user");
        String key=null;
        if (userId != null) {
            AuthenticatedUser user = authSvc.getAuthenticatedUser(userId);
            // If a user param was sent, we sign the URL for them, otherwise on behalf of
            // the superuser who made this api call
            if (user != null) {
                ApiToken apiToken = authSvc.findApiTokenByUser(user);
                if (apiToken != null && !apiToken.isExpired() && !apiToken.isDisabled()) {
                    key = apiToken.getTokenString();
                }
            } else {
                userId = superuser.getUserIdentifier();
                // We ~know this exists - the superuser just used it and it was unexpired/not
                // disabled. (ToDo - if we want this to work with workflow tokens (or as a
                // signed URL), we should do more checking as for the user above))
                key = authSvc.findApiTokenByUser(superuser).getTokenString();
            }
            if (key == null) {
                return error(Response.Status.CONFLICT, "Do not have a valid user with apiToken");
            }
            key = JvmSettings.API_SIGNING_SECRET.lookupOptional().orElse("") + key;
        }
        
        String baseUrl = urlInfo.getString("url");
        int timeout = urlInfo.getInt(URLTokenUtil.TIMEOUT, 10);
        String method = urlInfo.getString(URLTokenUtil.HTTP_METHOD, "GET");
        
        String signedUrl = UrlSignerUtil.signUrl(baseUrl, timeout, userId, method, key); 
        
        return ok(Json.createObjectBuilder().add(URLTokenUtil.SIGNED_URL, signedUrl));
    }
 
    @DELETE
    @Path("/clearThumbnailFailureFlag")
    public Response clearThumbnailFailureFlag() {
        em.createNativeQuery("UPDATE dvobject SET previewimagefail = FALSE").executeUpdate();
        return ok("Thumbnail Failure Flags cleared.");
    }
    
    @DELETE
    @Path("/clearThumbnailFailureFlag/{id}")
    public Response clearThumbnailFailureFlagByDatafile(@PathParam("id") String fileId) {
        try {
            DataFile df = findDataFileOrDie(fileId);
            Query deleteQuery = em.createNativeQuery("UPDATE dvobject SET previewimagefail = FALSE where id = ?");
            deleteQuery.setParameter(1, df.getId());
            deleteQuery.executeUpdate();
            return ok("Thumbnail Failure Flag cleared for file id=: " + df.getId() + ".");
        } catch (WrappedResponse r) {
            logger.info("Could not find file with the id: " + fileId);
            return error(Status.BAD_REQUEST, "Could not find file with the id: " + fileId);
        }
    }

    /**
     * For testing only. Download a file from /tmp.
     */
    @GET
    @AuthRequired
    @Path("/downloadTmpFile")
    public Response downloadTmpFile(@Context ContainerRequestContext crc, @QueryParam("fullyQualifiedPathToFile") String fullyQualifiedPathToFile) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        java.nio.file.Path normalizedPath = Paths.get(fullyQualifiedPathToFile).normalize();
        if (!normalizedPath.toString().startsWith("/tmp")) {
            return error(Status.BAD_REQUEST, "Path must begin with '/tmp' but after normalization was '" + normalizedPath +"'.");
        }
        try {
            return ok(new FileInputStream(fullyQualifiedPathToFile));
        } catch (IOException ex) {
            return error(Status.BAD_REQUEST, ex.toString());
        }
    }

    @GET
    @Path("/featureFlags")
    public Response getFeatureFlags() {
        Map<String, String> map = new TreeMap<>();
        for (FeatureFlags flag : FeatureFlags.values()) {
            map.put(flag.name(), flag.enabled() ? "enabled" : "disabled");
        }
        return ok(Json.createObjectBuilder(map));
    }

    @GET
    @Path("/featureFlags/{flag}")
    public Response getFeatureFlag(@PathParam("flag") String flagIn) {
        try {
            FeatureFlags flag = FeatureFlags.valueOf(flagIn);
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("enabled", flag.enabled());
            return ok(job);
        } catch (IllegalArgumentException ex) {
            return error(Status.NOT_FOUND, "Feature flag not found. Try listing all feature flags.");
        }
    }

    @GET
    @AuthRequired
    @Path("/datafiles/auditFiles")
    public Response getAuditFiles(@Context ContainerRequestContext crc,
                                  @QueryParam("firstId") Long firstId, @QueryParam("lastId") Long lastId,
                                  @QueryParam("datasetIdentifierList") String datasetIdentifierList) throws WrappedResponse {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        int datasetsChecked = 0;
        long startId = (firstId == null ? 0 : firstId);
        long endId = (lastId == null ? Long.MAX_VALUE : lastId);

        List<String> datasetIdentifiers;
        if (datasetIdentifierList == null || datasetIdentifierList.isEmpty()) {
            datasetIdentifiers = Collections.emptyList();
        } else {
            startId = 0;
            endId = Long.MAX_VALUE;
            datasetIdentifiers = List.of(datasetIdentifierList.split(","));
        }
        if (endId < startId) {
            return badRequest("Invalid Parameters: lastId must be equal to or greater than firstId");
        }

        NullSafeJsonBuilder jsonObjectBuilder = NullSafeJsonBuilder.jsonObjectBuilder();
        JsonArrayBuilder jsonDatasetsArrayBuilder = Json.createArrayBuilder();
        JsonArrayBuilder jsonFailuresArrayBuilder = Json.createArrayBuilder();

        if (startId > 0) {
            jsonObjectBuilder.add("firstId", startId);
        }
        if (endId < Long.MAX_VALUE) {
            jsonObjectBuilder.add("lastId", endId);
        }

        // compile the list of ids to process
        List<Long> datasetIds;
        if (datasetIdentifiers.isEmpty()) {
            datasetIds = datasetService.findAllLocalDatasetIds();
        } else {
            datasetIds = new ArrayList<>(datasetIdentifiers.size());
            JsonArrayBuilder jab = Json.createArrayBuilder();
            datasetIdentifiers.forEach(id -> {
                String dId = id.trim();
                jab.add(dId);
                Dataset d = datasetService.findByGlobalId(dId);
                if (d != null) {
                    datasetIds.add(d.getId());
                } else {
                    NullSafeJsonBuilder job = NullSafeJsonBuilder.jsonObjectBuilder();
                    job.add("datasetIdentifier",dId);
                    job.add("reason","Not Found");
                    jsonFailuresArrayBuilder.add(job);
                }
            });
            jsonObjectBuilder.add("datasetIdentifierList", jab);
        }

        for (Long datasetId : datasetIds) {
            if (datasetId < startId) {
                continue;
            } else if (datasetId > endId) {
                break;
            }
            Dataset dataset;
            try {
                dataset = findDatasetOrDie(String.valueOf(datasetId));
                datasetsChecked++;
            } catch (WrappedResponse e) {
                NullSafeJsonBuilder job = NullSafeJsonBuilder.jsonObjectBuilder();
                job.add("datasetId", datasetId);
                job.add("reason", e.getMessage());
                jsonFailuresArrayBuilder.add(job);
                continue;
            }

            List<String> missingFiles = new ArrayList<>();
            List<String> missingFileMetadata = new ArrayList<>();
            try {
                Predicate<String> filter = s -> true;
                StorageIO<DvObject> datasetIO = DataAccess.getStorageIO(dataset);
                final List<String> result = datasetIO.cleanUp(filter, true);
                // add files that are in dataset files but not in cleanup result or DataFiles with missing FileMetadata
                dataset.getFiles().forEach(df -> {
                    try {
                        StorageIO<DataFile> datafileIO = df.getStorageIO();
                        String storageId = df.getStorageIdentifier();
                        FileMetadata fm = df.getFileMetadata();
                        if (!datafileIO.exists()) {
                            missingFiles.add(storageId + "," + (fm != null ?
                                    (fm.getDirectoryLabel() != null || !fm.getDirectoryLabel().isEmpty() ? "directoryLabel,"+fm.getDirectoryLabel()+"," : "")
                                            +"label,"+fm.getLabel() : "type,"+df.getContentType()));
                        }
                        if (fm == null) {
                            missingFileMetadata.add(storageId + ",dataFileId," + df.getId());
                        }
                    } catch (IOException e) {
                        NullSafeJsonBuilder job = NullSafeJsonBuilder.jsonObjectBuilder();
                        job.add("dataFileId", df.getId());
                        job.add("reason", e.getMessage());
                        jsonFailuresArrayBuilder.add(job);
                    }
                });
            } catch (IOException e) {
                NullSafeJsonBuilder job = NullSafeJsonBuilder.jsonObjectBuilder();
                job.add("datasetId", datasetId);
                job.add("reason", e.getMessage());
                jsonFailuresArrayBuilder.add(job);
            }

            JsonObjectBuilder job = Json.createObjectBuilder();
            if (!missingFiles.isEmpty() || !missingFileMetadata.isEmpty()) {
                job.add("id", dataset.getId());
                job.add("pid", dataset.getProtocol() + ":" + dataset.getAuthority() + "/" + dataset.getIdentifier());
                job.add("persistentURL", dataset.getPersistentURL());
                if (!missingFileMetadata.isEmpty()) {
                    JsonArrayBuilder jabMissingFileMetadata = Json.createArrayBuilder();
                    missingFileMetadata.forEach(mm -> {
                        String[] missingMetadata = mm.split(",");
                        NullSafeJsonBuilder jobj = NullSafeJsonBuilder.jsonObjectBuilder()
                                .add("storageIdentifier", missingMetadata[0])
                                .add(missingMetadata[1], missingMetadata[2]);
                        jabMissingFileMetadata.add(jobj);
                    });
                    job.add("missingFileMetadata", jabMissingFileMetadata);
                }
                if (!missingFiles.isEmpty()) {
                    JsonArrayBuilder jabMissingFiles = Json.createArrayBuilder();
                    missingFiles.forEach(mf -> {
                        String[] missingFile = mf.split(",");
                        NullSafeJsonBuilder jobj = NullSafeJsonBuilder.jsonObjectBuilder()
                                .add("storageIdentifier", missingFile[0]);
                        for (int i = 2; i < missingFile.length; i+=2) {
                            jobj.add(missingFile[i-1], missingFile[i]);
                        }
                        jabMissingFiles.add(jobj);
                    });
                    job.add("missingFiles", jabMissingFiles);
                }
                jsonDatasetsArrayBuilder.add(job);
            }
        }

        jsonObjectBuilder.add("datasetsChecked", datasetsChecked);
        jsonObjectBuilder.add("datasets", jsonDatasetsArrayBuilder);
        jsonObjectBuilder.add("failures", jsonFailuresArrayBuilder);

        return ok(jsonObjectBuilder);
    }

    @GET
    @AuthRequired
    @Path("/rateLimitStats")
    @Produces("text/csv")
    public Response rateLimitStats(@Context ContainerRequestContext crc,
                                   @QueryParam("deltaMinutesFilter") Long deltaMinutesFilter) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        String csvData = cacheFactory.getStats(CacheFactoryBean.RATE_LIMIT_CACHE, deltaMinutesFilter != null ? String.valueOf(deltaMinutesFilter) : null);
        return Response.ok(csvData).header("Content-Disposition", "attachment; filename=\"data.csv\"").build();
    }
}
