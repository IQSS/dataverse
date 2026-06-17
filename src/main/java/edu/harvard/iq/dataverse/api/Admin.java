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
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
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
import edu.harvard.iq.dataverse.util.ListSplitUtil;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.nio.file.Paths;
import java.util.TreeMap;

/**
 * Where the secure, setup API calls live.
 * 
 * @author michael
 */
@Stateless
@Path("admin")
@Tag(name = "Admin", description = "Administrative settings, users, roles, indexing, validation, and maintenance operations.")
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
    @Operation(summary = "Enumerate database settings",
            description = "Lists all Dataverse database settings as a JSON object.")
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
    @Operation(summary = "Replace database settings",
            description = "Creates or replaces multiple Dataverse database settings from a JSON object.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "All database options successfully updated")
    })
    public Response putAllSettings(@RequestBody(description = "JSON object whose keys are setting names and whose values are setting values.")
            JsonObject settings) {
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
    @Operation(summary = "Store a database setting",
            description = "Creates or replaces a Dataverse database setting value.")
    public Response putSetting(@Parameter(description = "Database setting name.", required = true)
            @PathParam("name") String name,
            @RequestBody(description = "Setting value to store.")
            String content) {
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
    @Operation(summary = "Store a localized database setting",
            description = "Creates or replaces a language-specific Dataverse database setting value.")
    public Response putSettingLang(@Parameter(description = "Database setting name.", required = true)
            @PathParam("name") String name,
            @Parameter(description = "Language code for the localized value.", required = true)
            @PathParam("lang") String lang,
            @RequestBody(description = "Localized setting value to store.")
            String content) {
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
    @Operation(operationId = "Admin_getSettingByName",
            summary = "Read a database setting",
            description = "Returns the value for a Dataverse database setting.")
    public Response getSetting(@Parameter(description = "Database setting name.", required = true)
            @PathParam("name") String name) {
        try {
            SettingsServiceBean.validateSettingName(name);
            
            String content = settingsSvc.get(name);
            return (content != null) ? ok(content) : notFound("Setting " + name + " not found.");
        } catch (SettingsValidationException sve) {
            return error(Response.Status.BAD_REQUEST, sve.getMessage());
        }
    }
    
    @Path("settings/{name}/lang/{lang}")
    @GET
    @Operation(operationId = "Admin_getLocalizedSetting",
            summary = "Read a localized database setting",
            description = "Returns the language-specific value for a Dataverse database setting.")
    public Response getSetting(@Parameter(description = "Database setting name.", required = true)
            @PathParam("name") String name,
            @Parameter(description = "Language code for the localized value.", required = true)
            @PathParam("lang") String lang) {
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
    @Operation(summary = "Remove a database setting",
            description = "Deletes a Dataverse database setting by name.")
    public Response deleteSetting(@Parameter(description = "Database setting name.", required = true)
            @PathParam("name") String name) {
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
    @Operation(summary = "Remove a localized database setting",
            description = "Deletes a language-specific Dataverse database setting value.")
    public Response deleteSettingLang(@Parameter(description = "Database setting name.", required = true)
            @PathParam("name") String name,
            @Parameter(description = "Language code for the localized value.", required = true)
            @PathParam("lang") String lang) {
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
    @Operation(summary = "Remove a metadata template",
            description = "Deletes a metadata template and clears default-template references that point to it.")
    public Response deleteTemplate(@Parameter(description = "Template database id.", required = true)
            @PathParam("id") long id) {
        
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
    @Operation(summary = "Enumerate metadata templates",
            description = "Lists metadata templates with template id, name, and owner information.")
    public Response findAllTemplates() {
        return findTemplates("");
    }
    
    @Path("templates/{alias}")
    @GET
    @Operation(summary = "Enumerate metadata templates for a dataverse",
            description = "Lists metadata templates owned by the dataverse with the supplied alias.")
    public Response findTemplates(@Parameter(description = "Dataverse alias whose templates are listed.", required = true)
            @PathParam("alias") String alias) {
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
    @Operation(summary = "Enumerate authentication provider factories",
            description = "Lists authentication provider factory aliases and provider information.")
    public Response listAuthProviderFactories() {
        return ok(authSvc.listProviderFactories().stream()
                .map(f -> jsonObjectBuilder().add("alias", f.getAlias()).add("info", f.getInfo()))
                .collect(toJsonArray()));
    }

    @Path("authenticationProviders")
    @GET
    @Operation(summary = "Enumerate authentication providers",
            description = "Lists registered authentication provider rows.")
    public Response listAuthProviders() {
        return ok(em.createNamedQuery("AuthenticationProviderRow.findAll", AuthenticationProviderRow.class)
                .getResultList().stream().map(r -> json(r)).collect(toJsonArray()));
    }

    @Path("authenticationProviders")
    @POST
    @Operation(summary = "Register an authentication provider",
            description = "Creates or updates an authentication provider row and registers the provider when it is enabled.")
    public Response addProvider(@RequestBody(description = "Authentication provider row, including id, factory alias, enabled state, and factory configuration.")
            AuthenticationProviderRow row) {
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
    @Operation(summary = "Read an authentication provider",
            description = "Returns the registered authentication provider row for the supplied provider id.")
    public Response showProvider(@Parameter(description = "Authentication provider id.", required = true)
            @PathParam("id") String id) {
        AuthenticationProviderRow row = em.find(AuthenticationProviderRow.class, id);
        return (row != null) ? ok(json(row))
                : error(Status.NOT_FOUND, "Can't find authetication provider with id '" + id + "'");
    }

    @POST
    @Path("authenticationProviders/{id}/:enabled")
    @Operation(summary = "Set authentication provider enabled state through the deprecated route",
            description = "Enables or disables an authentication provider using the legacy route.")
    public Response enableAuthenticationProvider_deprecated(@Parameter(description = "Authentication provider id.", required = true)
            @PathParam("id") String id,
            @RequestBody(description = "Boolean value indicating whether the provider should be enabled.")
            String body) {
        return enableAuthenticationProvider(id, body);
    }

    @PUT
    @Path("authenticationProviders/{id}/enabled")
    @Produces("application/json")
    @Operation(summary = "Switch authentication provider enabled state",
            description = "Enables or disables a registered authentication provider.")
    public Response enableAuthenticationProvider(@Parameter(description = "Authentication provider id.", required = true)
            @PathParam("id") String id,
            @RequestBody(description = "Boolean value indicating whether the provider should be enabled.")
            String body) {
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
    @Operation(summary = "Check authentication provider enabled state",
            description = "Returns whether a registered authentication provider is enabled.")
    public Response checkAuthenticationProviderEnabled(@Parameter(description = "Authentication provider id.", required = true)
            @PathParam("id") String id) {
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
    @Operation(summary = "Remove an authentication provider",
            description = "Deletes an authentication provider row and deregisters the provider.")
    public Response deleteAuthenticationProvider(@Parameter(description = "Authentication provider id.", required = true)
            @PathParam("id") String id) {
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
    @Operation(summary = "Read an authenticated user",
            description = "Returns account details for an authenticated user identifier.")
    public Response getAuthenticatedUserByIdentifier(@Parameter(description = "Authenticated user identifier.", required = true)
            @PathParam("identifier") String identifier) {
        AuthenticatedUser authenticatedUser = authSvc.getAuthenticatedUser(identifier);
        if (authenticatedUser != null) {
            return ok(json(authenticatedUser));
        }
        return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
    }

    @DELETE
    @Path("authenticatedUsers/{identifier}/")
    @Operation(summary = "Remove an authenticated user",
            description = "Deletes an authenticated user after checking that the account can be safely removed.")
    public Response deleteAuthenticatedUser(@Parameter(description = "Authenticated user identifier.", required = true)
            @PathParam("identifier") String identifier) {
        AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
        if (user != null) {
            return deleteAuthenticatedUser(user);
        }
        return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
    }
    
    @DELETE
    @Path("authenticatedUsers/id/{id}/")
    @Operation(summary = "Remove an authenticated user by id",
            description = "Deletes an authenticated user by database id after checking that the account can be safely removed.")
    public Response deleteAuthenticatedUserById(@Parameter(description = "Authenticated user database id.", required = true)
            @PathParam("id") Long id) {
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
    @Operation(summary = "Deactivate an authenticated user",
            description = "Disables an authenticated user account by identifier.")
    public Response deactivateAuthenticatedUser(@Parameter(description = "Authenticated user identifier.", required = true)
            @PathParam("identifier") String identifier) {
        AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
        if (user != null) {
            return deactivateAuthenticatedUser(user);
        }
        return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
    }

    @POST
    @Path("authenticatedUsers/id/{id}/deactivate")
    @Operation(summary = "Deactivate an authenticated user by id",
            description = "Disables an authenticated user account by database id.")
    public Response deactivateAuthenticatedUserById(@Parameter(description = "Authenticated user database id.", required = true)
            @PathParam("id") Long id) {
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
    @Operation(summary = "Publish a dataverse as its creator",
            description = "Publishes a dataverse using the dataverse creator as the request user.")
    public Response publishDataverseAsCreator(@Parameter(description = "Dataverse database id.", required = true)
            @PathParam("id") long id) {
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
    @Operation(summary = "Enumerate authenticated users",
            description = "Lists all authenticated users for a superuser.")
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
    @Operation(summary = "Search authenticated users",
            description = "Searches authenticated users for the dashboard user list and returns paged JSON results.")
    public Response filterAuthenticatedUsers(
            @Context ContainerRequestContext crc,
            @Parameter(description = "Search text matched against authenticated users.")
            @QueryParam("searchTerm") String searchTerm,
            @Parameter(description = "One-based result page to return.")
            @QueryParam("selectedPage") Integer selectedPage,
            @Parameter(description = "Number of users to include on each page.")
            @QueryParam("itemsPerPage") Integer itemsPerPage,
            @Parameter(description = "Sort field used for the user list.")
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
    @Operation(summary = "Provision an authenticated user",
            description = "Creates an authenticated user account from provider id, persistent id, identifier, name, and email fields.")
    public Response createAuthenicatedUser(@RequestBody(description = "User creation JSON containing authenticationProviderId, persistentUserId, identifier, firstName, lastName, and email.")
            JsonObject jsonObject) {
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
    @Operation(summary = "Convert a Shibboleth user to a built-in account",
            description = "Converts a remote Shibboleth authenticated user to a built-in account using a new email address.")
    public Response convertShibUserToBuiltin(@Context ContainerRequestContext crc,
            @Parameter(description = "Authenticated user database id.", required = true)
            @PathParam("id") Long id,
            @RequestBody(description = "Email address for the converted built-in account.")
            String newEmailAddress) {
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
    @Operation(summary = "Convert a remote user to a built-in account",
            description = "Converts a remote authenticated user to a built-in account using a new email address.")
    public Response convertOAuthUserToBuiltin(@Context ContainerRequestContext crc,
            @Parameter(description = "Authenticated user database id.", required = true)
            @PathParam("id") Long id,
            @RequestBody(description = "Email address for the converted built-in account.")
            String newEmailAddress) {
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
    @Operation(summary = "Convert a built-in user to a Shibboleth account",
            description = "Converts a built-in authenticated user to a Shibboleth account using colon-separated conversion data.")
    public Response builtin2shib(@Context ContainerRequestContext crc,
            @RequestBody(description = "Colon-separated email, password, and replacement email values for the conversion.")
            String content) {
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
    @Operation(summary = "Convert a built-in user to a remote account",
            description = "Converts a built-in authenticated user to a remote account using colon-separated conversion data.")
    public Response builtin2oauth(@Context ContainerRequestContext crc,
            @RequestBody(description = "Colon-separated email, password, replacement email, provider id, and persistent user id values.")
            String content) {
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
    @Operation(summary = "Provision a built-in role",
            description = "Creates a built-in Dataverse role from the supplied role definition.")
    public Response createNewBuiltinRole(@RequestBody(description = "Role definition containing alias, name, description, and permissions.")
            RoleDTO roleDto) {
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
    @Operation(summary = "Revise a built-in role",
            description = "Updates an existing built-in Dataverse role from the supplied role definition.")
    public Response updateBuiltinRole(@RequestBody(description = "Updated role definition containing alias, name, description, and permissions.")
            RoleDTO roleDto,
            @Parameter(description = "Role database id.", required = true)
            @PathParam("id") long roleId) {
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
    @Operation(summary = "Enumerate built-in roles",
            description = "Lists built-in Dataverse roles.")
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
    @Operation(summary = "Remove a built-in role",
            description = "Deletes a Dataverse role by id or alias.")
    public Response deleteRole(@Context ContainerRequestContext crc,
            @Parameter(description = "Role id or alias.", required = true)
            @PathParam("id") String id) {

        return response(req -> {
            DataverseRole doomed = findRoleOrDie(id);
            execCommand(new DeleteRoleCommand(req, doomed));
            return ok("role " + doomed.getName() + " deleted.");
        }, getRequestUser(crc));
    }

    @Path("superuser/{identifier}")
    @Deprecated
    @POST
    @Operation(summary = "Toggle superuser status",
            description = "Switches an authenticated user between superuser and non-superuser status.")
    public Response toggleSuperuser(@Parameter(description = "Authenticated user identifier.", required = true)
            @PathParam("identifier") String identifier) {
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
    @Operation(summary = "Assign superuser status",
            description = "Sets whether an authenticated user has superuser privileges.")
    public Response setSuperuserStatus(@Parameter(description = "Authenticated user identifier.", required = true)
            @PathParam("identifier") String identifier,
            @RequestBody(description = "Boolean text indicating whether the user should be a superuser.")
            String isSuperuser) {
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
    @Operation(summary = "Validate all datasets",
            description = "Streams validation results for every local dataset.")
    public Response validateAllDatasets(@Parameter(description = "Whether to include variable-level validation details.")
            @QueryParam("variables") boolean includeVariables) {
        
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
    @Operation(summary = "Validate one dataset",
            description = "Validates a dataset and returns either valid status or the first constraint violation.")
    public Response validateDataset(@Parameter(description = "Dataset id or persistent identifier.", required = true)
            @PathParam("id") String id,
            @Parameter(description = "Whether to include variable-level validation details.")
            @QueryParam("variables") boolean includeVariables) {
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
    @Operation(summary = "Validate files in one dataset",
            description = "Streams checksum validation results for all data files in a dataset.")
    public Response validateDatasetDatafiles(@Parameter(description = "Dataset id or persistent identifier.", required = true)
            @PathParam("id") String id) {
        
        Dataset dataset;
        // First check if the dataset exists before starting the streaming output
        try {
            dataset = findDatasetOrDie(id);
        } catch (WrappedResponse wr) {
            return wr.getResponse(); // This will return the proper 404 Not Found response
        }
        
        // Streaming output: the API will start producing 
        // the output right away, as it goes through the list 
        // of the datafiles in the dataset.
        // The streaming mechanism is modeled after validate/datasets API.
        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException,
                    WebApplicationException {
                
                os.write("{\"dataFiles\": [\n".getBytes());
                
                boolean wroteObject = false;
                for (DataFile dataFile : dataset.getFiles()) {

                    boolean success = false;
                     
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
    @Operation(summary = "Enumerate assignments for a role assignee",
            description = "Lists role assignments held by the supplied role assignee identifier.")
    public Response getAssignmentsFor(@Parameter(description = "Role assignee identifier.", required = true)
            @PathParam("raIdtf") String raIdtf) {

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
    @Operation(summary = "Read a confirm-email token",
            description = "Returns the active confirm-email token for an authenticated user.")
    public Response getConfirmEmailToken(@Parameter(description = "Authenticated user database id.", required = true)
            @PathParam("userId") long userId) {
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
    @Operation(summary = "Start confirm-email processing",
            description = "Creates confirm-email data for an authenticated user and returns token metadata.")
    public Response startConfirmEmailProcess(@Parameter(description = "Authenticated user database id.", required = true)
            @PathParam("userId") long userId) {
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
    @Operation(summary = "Convert a test built-in password hash",
            description = "Rewrites a built-in user's password hash to the legacy SHA-1 test value.")
    public Response convertUserFromBcryptToSha1(@RequestBody(description = "JSON object containing builtinUserId.")
            String json) {
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
    @Operation(summary = "Inspect permissions on a Dataverse object",
            description = "Returns the requesting user's permissions on the selected dataverse object.")
    public Response findPermissonsOn(@Context final ContainerRequestContext crc,
            @Parameter(description = "Dataverse object id or persistent identifier.", required = true)
            @PathParam("dvo") final String dvo) {
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
    @Operation(summary = "Read a role assignee",
            description = "Returns display information for a role assignee identifier.")
    public Response findRoleAssignee(@Parameter(description = "Role assignee identifier.", required = true)
            @PathParam("idtf") String idtf) {
        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(idtf);
        return (ra == null) ? notFound("Role Assignee '" + idtf + "' not found.") : ok(json(ra.getDisplayInfo()));
    }

    @Path("datasets/integrity/{datasetVersionId}/fixmissingunf")
    @POST
    @Operation(summary = "Repair missing UNF values",
            description = "Recalculates missing UNF values for a dataset version.")
    public Response fixUnf(@Parameter(description = "Dataset version database id.", required = true)
            @PathParam("datasetVersionId") String datasetVersionId,
            @Parameter(description = "When true, recalculate UNF values even when existing values are present.")
            @QueryParam("forceRecalculate") boolean forceRecalculate) {
        JsonObjectBuilder info = datasetVersionSvc.fixMissingUnf(datasetVersionId, forceRecalculate);
        return ok(info);
    }

    @Path("datafiles/integrity/fixmissingoriginaltypes")
    @GET
    @Operation(summary = "Repair missing original file types",
            description = "Starts a background repair for tabular files missing original file type metadata.")
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
    @Operation(summary = "Repair missing original file sizes",
            description = "Starts a background repair for tabular files missing original file size metadata.")
    public Response fixMissingOriginalSizes(@Parameter(description = "Maximum number of affected files to repair.")
            @QueryParam("limit") Integer limit) {
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
    @Operation(summary = "Read dataset thumbnail metadata",
            description = "Returns thumbnail selection metadata and optional thumbnail image data for a dataset.")
    public Response getDatasetThumbnailMetadata(@Parameter(description = "Dataset database id.", required = true)
            @PathParam("id") Long idSupplied) {
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
    @Operation(summary = "Validate a password",
            description = "Checks a password against the configured password policy and returns validation errors.")
    public Response validatePassword(@RequestBody(description = "Password text to validate.")
            String password) {

        final List<String> errors = passwordValidatorService.validate(password, new Date(), false);
        final JsonArrayBuilder errorArray = Json.createArrayBuilder();
        errors.forEach(errorArray::add);
        return ok(Json.createObjectBuilder().add("password", password).add("errors", errorArray));
    }

    @GET
    @Path("/isOrcid")
    @Operation(summary = "Check ORCID authentication support",
            description = "Returns whether ORCID authentication is enabled.")
    public Response isOrcidEnabled() {
        return authSvc.isOrcidEnabled() ? ok("Orcid is enabled") : ok("no orcid for you.");
    }

    @POST
    @AuthRequired
    @Path("{id}/reregisterHDLToPID")
    @Operation(summary = "Migrate a dataset handle registration to DOI",
            description = "Registers a dataset PID again when migrating a released handle-based dataset to DOI.")
    public Response reregisterHdlToPID(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataset id or persistent identifier.", required = true)
            @PathParam("id") String id) {
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
    @Operation(summary = "Process PID assignment for a data file",
            description = "Attempts persistent identifier assignment for one published data file when file PIDs are enabled.")
    public Response registerDataFile(@Context ContainerRequestContext crc,
            @Parameter(description = "Data file id or persistent identifier.", required = true)
            @PathParam("id") String id) {
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
    @Operation(summary = "Process PID assignment for all eligible data files",
            description = "Scans all data files and assigns persistent identifiers to released, unregistered files in collections where file PIDs are enabled.")
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
    @Operation(summary = "Process PID assignment for collection data files",
            description = "Assigns persistent identifiers to released, unregistered files directly owned by a collection.")
    public Response registerDataFilesInCollection(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias for the collection whose files are processed.", required = true)
            @PathParam("alias") String alias,
            @Parameter(description = "Seconds to wait between file registration attempts.")
            @QueryParam("sleep") Integer sleepInterval) {
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
    @Operation(summary = "Updates file hash values",
            description = "Recalculates file hash values with the requested algorithm for eligible files when the requester is authorized.")
    public Response updateHashValues(@Context ContainerRequestContext crc, @Parameter(description = "Checksum algorithm.") @PathParam("alg") String alg, @Parameter(description = "Maximum number of objects to update.") @QueryParam("num") int num) {
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
    @Operation(summary = "Compute a data file hash value",
            description = "Calculates a new checksum for one non-harvested data file using the requested algorithm and saves it on the file.")
    public Response computeDataFileHashValue(@Context ContainerRequestContext crc,
            @Parameter(description = "Data file id or persistent identifier.", required = true)
            @PathParam("fileId") String fileId,
            @Parameter(description = "Checksum algorithm to calculate.", required = true)
            @PathParam("alg") String alg) {

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
    @Operation(summary = "Validate a data file hash value",
            description = "Recalculates a data file checksum and compares it with the stored checksum.")
    public Response validateDataFileHashValue(@Context ContainerRequestContext crc,
            @Parameter(description = "Data file id or persistent identifier.", required = true)
            @PathParam("fileId") String fileId) {

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
    @Operation(summary = "Submit a dataset version to an archive",
            description = "Starts archival submission for a dataset version that has not already been archived.")
    public Response submitDatasetVersionToArchive(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataset id or persistent identifier.", required = true)
            @PathParam("id") String dsid,
            @Parameter(description = "Dataset version number.", required = true)
            @PathParam("version") String versionNumber) {

        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);

            Dataset ds = findDatasetOrDie(dsid);

            DatasetVersion dv = datasetversionService.findByFriendlyVersionNumber(ds.getId(), versionNumber);
            if(dv==null) {
                return error(Status.BAD_REQUEST, "Requested version not found.");
            }
            //ToDo - allow forcing with a non-success status for archivers that supportsDelete()
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
    @Operation(summary = "Archive unarchived dataset versions",
            description = "Lists or starts archival submission for published dataset versions that do not yet have archival copies.")
    public Response archiveAllUnarchivedDatasetVersions(@Context ContainerRequestContext crc,
            @Parameter(description = "When true, list matching dataset versions without submitting them.")
            @QueryParam("listonly") boolean listonly,
            @Parameter(description = "Maximum number of dataset versions to list or submit.")
            @QueryParam("limit") Integer limit,
            @Parameter(description = "When true, include only each dataset's latest version for archival submission.")
            @QueryParam("latestonly") boolean latestonly) {

        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
            //ToDo - allow forcing with a non-success status for archivers that supportsDelete()
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
    @Operation(summary = "Clear all metrics cache entries",
            description = "Deletes every cached metric row.")
    public Response clearMetricsCache() {
        em.createNativeQuery("DELETE FROM metric").executeUpdate();
        return ok("all metric caches cleared.");
    }

    @DELETE
    @Path("/clearMetricsCache/{name}")
    @Operation(summary = "Clear metrics cache entries by name",
            description = "Deletes cached metric rows with the supplied metric name.")
    public Response clearMetricsCacheByName(@Parameter(description = "Metric cache name to clear.", required = true)
            @PathParam("name") String name) {
        Query deleteQuery = em.createNativeQuery("DELETE FROM metric where name = ?");
        deleteQuery.setParameter(1, name);
        deleteQuery.executeUpdate();
        return ok("metric cache " + name + " cleared.");
    }

    @GET
    @AuthRequired
    @Path("/dataverse/{alias}/addRoleAssignmentsToChildren")
    @Operation(summary = "Propagate dataverse role assignments to children",
            description = "Copies configured inherited role assignments from a dataverse to its child dataverses and datasets.")
    public Response addRoleAssignementsToChildren(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias whose child objects receive role assignments.", required = true)
            @PathParam("alias") String alias) throws WrappedResponse {
        Dataverse owner = dataverseSvc.findByAlias(alias);
        if (owner == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        AuthenticatedUser user = null;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        boolean inheritAllRoles = false;
        String rolesString = settingsSvc.getValueForKey(SettingsServiceBean.Key.InheritParentRoleAssignments, "");
        if (rolesString.length() > 0) {
            ArrayList<String> rolesToInherit = new ArrayList<>(ListSplitUtil.split(rolesString));
            if (!rolesToInherit.isEmpty()) {
                if (rolesToInherit.contains("*")) {
                    inheritAllRoles = true;
                }
                return ok(dataverseSvc.addRoleAssignmentsToChildren(owner, rolesToInherit, inheritAllRoles, createDataverseRequest(user)));
            }
        }
        return error(Response.Status.BAD_REQUEST,
                "InheritParentRoleAssignments does not list any roles on this instance");
    }
    
    @GET
    @AuthRequired
    @Path("/dataverse/{alias}/curationLabelSet")
    @Operation(summary = "Returns a dataverse curation label set",
            description = "Returns the curation label set configured on a dataverse when the requester is a superuser.")
    public Response getCurationLabelSet(@Context ContainerRequestContext crc, @Parameter(description = "Dataverse alias.") @PathParam("alias") String alias) throws WrappedResponse {
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
    @Operation(summary = "Assign a dataverse curation label set",
            description = "Assigns a configured curation label set name to a dataverse.")
    public Response setCurationLabelSet(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias.", required = true)
            @PathParam("alias") String alias,
            @Parameter(description = "Configured curation label set name.")
            @QueryParam("name") String name) throws WrappedResponse {
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
    @Operation(summary = "Reset a dataverse curation label set",
            description = "Restores a dataverse to the default curation label set setting.")
    public Response resetCurationLabelSet(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias.", required = true)
            @PathParam("alias") String alias) throws WrappedResponse {
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
    @Operation(summary = "Enumerate curation label sets",
            description = "Lists configured curation label sets and their labels.")
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
    @Operation(summary = "Publish a banner message",
            description = "Creates an active banner message with localized message text.")
    public Response addBannerMessage(@RequestBody(description = "Banner message JSON containing dismissibleByUser and messageTexts entries.")
            JsonObject jsonObject) throws WrappedResponse {

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
    @Operation(summary = "Remove a banner message",
            description = "Deletes a banner message by database id.")
    public Response deleteBannerMessage(@Parameter(description = "Banner message database id.", required = true)
            @PathParam("id") Long id) throws WrappedResponse {
 
        BannerMessage message = em.find(BannerMessage.class, id);
        if (message == null){
            return error(Response.Status.NOT_FOUND, "Message id = "  + id + " not found.");
        }
        bannerMessageService.deleteBannerMessage(id);
        
        return ok("Message id =  " + id + " deleted.");

    }
    
    @PUT
    @Path("/bannerMessage/{id}/deactivate")
    @Operation(summary = "Deactivate a banner message",
            description = "Marks a banner message inactive by database id.")
    public Response deactivateBannerMessage(@Parameter(description = "Banner message database id.", required = true)
            @PathParam("id") Long id) throws WrappedResponse {
        BannerMessage message = em.find(BannerMessage.class, id);
        if (message == null){
            return error(Response.Status.NOT_FOUND, "Message id = "  + id + " not found.");
        }
        bannerMessageService.deactivateBannerMessage(id);
        
        return ok("Message id =  " + id + " deactivated.");

    }
    
    @GET
    @Path("/bannerMessage")
    @Operation(summary = "Enumerate banner messages",
            description = "Lists banner message ids and display values.")
    public Response getBannerMessages(@Parameter(description = "Unused banner message id parameter.")
            @PathParam("id") Long id) throws WrappedResponse {

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
    @Operation(summary = "Sign a URL",
            description = "Creates a signed URL for a supplied URL and optional user token context.")
    public Response getSignedUrl(@Context ContainerRequestContext crc,
            @RequestBody(description = "JSON object containing url, optional user identifier, timeout, HTTP method, and optional credential key.")
            JsonObject urlInfo) {
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
    @Operation(summary = "Clear all thumbnail failure flags",
            description = "Resets thumbnail preview failure flags for all dataverse objects.")
    public Response clearThumbnailFailureFlag() {
        em.createNativeQuery("UPDATE dvobject SET previewimagefail = FALSE").executeUpdate();
        return ok("Thumbnail Failure Flags cleared.");
    }
    
    @DELETE
    @Path("/clearThumbnailFailureFlag/{id}")
    @Operation(summary = "Clear a data file thumbnail failure flag",
            description = "Resets the thumbnail preview failure flag for one data file.")
    public Response clearThumbnailFailureFlagByDatafile(@Parameter(description = "Data file id or persistent identifier.", required = true)
            @PathParam("id") String fileId) {
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
    @Operation(summary = "Download a temporary file",
            description = "Streams a file from the local /tmp directory for superuser testing.")
    public Response downloadTmpFile(@Context ContainerRequestContext crc,
            @Parameter(description = "Absolute path under /tmp to stream.")
            @QueryParam("fullyQualifiedPathToFile") String fullyQualifiedPathToFile) {
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
    @Operation(summary = "Enumerate feature flags",
            description = "Lists feature flag names with enabled or disabled status.")
    public Response getFeatureFlags() {
        Map<String, String> map = new TreeMap<>();
        for (FeatureFlags flag : FeatureFlags.values()) {
            map.put(flag.name(), flag.enabled() ? "enabled" : "disabled");
        }
        return ok(Json.createObjectBuilder(map));
    }

    @GET
    @Path("/featureFlags/{flag}")
    @Operation(summary = "Read a feature flag",
            description = "Returns enabled status for one feature flag.")
    public Response getFeatureFlag(@Parameter(description = "Feature flag enum name.", required = true)
            @PathParam("flag") String flagIn) {
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
    @Operation(summary = "Audit dataset file storage",
            description = "Checks selected datasets for missing physical files and missing file metadata.")
    public Response getAuditFiles(@Context ContainerRequestContext crc,
                                  @Parameter(description = "Lowest dataset database id to audit.")
                                  @QueryParam("firstId") Long firstId,
                                  @Parameter(description = "Highest dataset database id to audit.")
                                  @QueryParam("lastId") Long lastId,
                                  @Parameter(description = "Comma-separated dataset persistent identifiers to audit instead of an id range.")
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
    @Operation(summary = "Download rate-limit statistics",
            description = "Streams cached rate-limit statistics as CSV for a superuser.")
    public Response rateLimitStats(@Context ContainerRequestContext crc,
                                   @Parameter(description = "Limit statistics to entries within this many minutes.")
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
