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
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EMailValidator;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.RoleAssignment;
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

import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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
import edu.harvard.iq.dataverse.engine.command.impl.RegisterDvObjectCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.persistence.Query;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

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
        

	// Make the session available
	@Inject
	DataverseSession session;

	public static final String listUsersPartialAPIPath = "list-users";
	public static final String listUsersFullAPIPath = "/api/admin/" + listUsersPartialAPIPath;

	@Path("settings")
	@GET
	public Response listAllSettings() {
		JsonObjectBuilder bld = jsonObjectBuilder();
		settingsSvc.listAll().forEach(s -> bld.add(s.getName(), s.getContent()));
		return ok(bld);
	}

	@Path("settings/{name}")
	@PUT
	public Response putSetting(@PathParam("name") String name, String content) {
		Setting s = settingsSvc.set(name, content);
		return ok(jsonObjectBuilder().add(s.getName(), s.getContent()));
	}

	@Path("settings/{name}/lang/{lang}")
	@PUT
	public Response putSetting(@PathParam("name") String name, @PathParam("lang") String lang, String content) {
		Setting s = settingsSvc.set(name, lang, content);
		return ok("Setting " + name + " - " + lang + " - added.");
	}

	@Path("settings/{name}")
	@GET
	public Response getSetting(@PathParam("name") String name) {
		String s = settingsSvc.get(name);

		return (s != null) ? ok(s) : notFound("Setting " + name + " not found");
	}

	@Path("settings/{name}")
	@DELETE
	public Response deleteSetting(@PathParam("name") String name) {
		settingsSvc.delete(name);

		return ok("Setting " + name + " deleted.");
	}

	@Path("settings/{name}/lang/{lang}")
	@DELETE
	public Response deleteSetting(@PathParam("name") String name, @PathParam("lang") String lang) {
		settingsSvc.delete(name, lang);
		return ok("Setting " + name + " - " + lang + " deleted.");
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
        return ok("AuthenticatedUser " + au.getIdentifier() + " deleted. ");

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
	@Path("authenticatedUsers")
	public Response listAuthenticatedUsers() {
		try {
			AuthenticatedUser user = findAuthenticatedUserOrDie();
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
	@Path(listUsersPartialAPIPath)
	@Produces({ "application/json" })
	public Response filterAuthenticatedUsers(
			@QueryParam("searchTerm") String searchTerm,
			@QueryParam("selectedPage") Integer selectedPage,
			@QueryParam("itemsPerPage") Integer itemsPerPage,
			@QueryParam("sortKey") String sortKey
	) {

		User authUser;
		try {
			authUser = this.findUserOrDie();
		} catch (AbstractApiBean.WrappedResponse ex) {
			return error(Response.Status.FORBIDDEN,
					BundleUtil.getStringFromBundle("dashboard.list_users.api.auth.invalid_apikey"));
		}

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
	@Path("authenticatedUsers/id/{id}/convertShibToBuiltIn")
	@Deprecated
	public Response convertShibUserToBuiltin(@PathParam("id") Long id, String newEmailAddress) {
                try {
                        AuthenticatedUser user = findAuthenticatedUserOrDie();
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
	@Path("authenticatedUsers/id/{id}/convertRemoteToBuiltIn")
	public Response convertOAuthUserToBuiltin(@PathParam("id") Long id, String newEmailAddress) {
                try {
			AuthenticatedUser user = findAuthenticatedUserOrDie();
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
	@Path("authenticatedUsers/convert/builtin2shib")
	@PUT
	public Response builtin2shib(String content) {
		logger.info("entering builtin2shib...");
		try {
			AuthenticatedUser userToRunThisMethod = findAuthenticatedUserOrDie();
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
		boolean validEmail = EMailValidator.isEmailValid(overwriteEmail, null);
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
	@Path("authenticatedUsers/convert/builtin2oauth")
	@PUT
	public Response builtin2oauth(String content) {
		logger.info("entering builtin2oauth...");
		try {
			AuthenticatedUser userToRunThisMethod = findAuthenticatedUserOrDie();
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
		boolean validEmail = EMailValidator.isEmailValid(overwriteEmail, null);
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
    @Path("roles/{id}")
    public Response deleteRole(@PathParam("id") String id) {

        return response(req -> {
            DataverseRole doomed = findRoleOrDie(id);
            execCommand(new DeleteRoleCommand(req, doomed));
            return ok("role " + doomed.getName() + " deleted.");
        });
    }

	@Path("superuser/{identifier}")
	@POST
	public Response toggleSuperuser(@PathParam("identifier") String identifier) {
		ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "toggleSuperuser")
				.setInfo(identifier);
		try {
			AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);
                        if (user.isDeactivated()) {
                            return error(Status.BAD_REQUEST, "You cannot make a deactivated user a superuser.");
                        }

			user.setSuperuser(!user.isSuperuser());

			return ok("User " + user.getIdentifier() + " " + (user.isSuperuser() ? "set" : "removed")
					+ " as a superuser.");
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

                    os.write(output.build().toString().getBytes("UTF8"));
                    
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

                    os.write(output.build().toString().getBytes("UTF8"));
                    
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
	@GET
	public Response findPermissonsOn(@PathParam("dvo") String dvo) {
		try {
			DvObject dvObj = findDvo(dvo);
			if (dvObj == null) {
				return notFound("DvObject " + dvo + " not found");
			}
			try {
				User aUser = findUserOrDie();
				JsonObjectBuilder bld = Json.createObjectBuilder();
				bld.add("user", aUser.getIdentifier());
				bld.add("permissions", json(permissionSvc.permissionsFor(createDataverseRequest(aUser), dvObj)));
				return ok(bld);

			} catch (WrappedResponse wr) {
				return wr.getResponse();
			}
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
				logger.info("found file id: " + fileid);
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
    @Path("{id}/reregisterHDLToPID")
    public Response reregisterHdlToPID(@PathParam("id") String id) {
        logger.info("Starting to reregister  " + id + " Dataset Id. (from hdl to doi)" + new Date());
        try {
            if (settingsSvc.get(SettingsServiceBean.Key.Protocol.toString()).equals(GlobalId.HDL_PROTOCOL)) {
                logger.info("Bad Request protocol set to handle  " );
                return error(Status.BAD_REQUEST, BundleUtil.getStringFromBundle("admin.api.migrateHDL.failure.must.be.set.for.doi"));
            }
            
            User u = findUserOrDie();
            if (!u.isSuperuser()) {
                logger.info("Bad Request Unauthor " );
                return error(Status.UNAUTHORIZED, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
            }

            DataverseRequest r = createDataverseRequest(u);
            Dataset ds = findDatasetOrDie(id);
            if (ds.getIdentifier() != null && !ds.getIdentifier().isEmpty() && ds.getProtocol().equals(GlobalId.HDL_PROTOCOL)) {
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
	@Path("{id}/registerDataFile")
	public Response registerDataFile(@PathParam("id") String id) {
		logger.info("Starting to register  " + id + " file id. " + new Date());

		try {
			User u = findUserOrDie();
			DataverseRequest r = createDataverseRequest(u);
			DataFile df = findDataFileOrDie(id);
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
	@Path("/registerDataFileAll")
	public Response registerDataFileAll() {
		Integer count = fileService.findAll().size();
		Integer successes = 0;
		Integer alreadyRegistered = 0;
		Integer released = 0;
		Integer draft = 0;
		logger.info("Starting to register: analyzing " + count + " files. " + new Date());
		logger.info("Only unregistered, published files will be registered.");
		for (DataFile df : fileService.findAll()) {
			try {
				if ((df.getIdentifier() == null || df.getIdentifier().isEmpty())) {
					if (df.isReleased()) {
						released++;
						User u = findAuthenticatedUserOrDie();
						DataverseRequest r = createDataverseRequest(u);
						execCommand(new RegisterDvObjectCommand(r, df));
						successes++;
						if (successes % 100 == 0) {
							logger.info(successes + " of  " + count + " files registered successfully. " + new Date());
						}
					} else {
						draft++;
						logger.info(draft + " of  " + count + " files not yet published");
					}
				} else {
					alreadyRegistered++;
					logger.info(alreadyRegistered + " of  " + count + " files are already registered. " + new Date());
				}
			} catch (WrappedResponse ex) {
				released++;
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

		return ok("Datafile registration complete." + successes + " of  " + released
				+ " unregistered, published files registered successfully.");
	}

	@GET
	@Path("/updateHashValues/{alg}")
	public Response updateHashValues(@PathParam("alg") String alg, @QueryParam("num") int num) {
		Integer count = fileService.findAll().size();
		Integer successes = 0;
		Integer alreadyUpdated = 0;
		Integer rehashed = 0;
		Integer harvested=0;
		
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
			User u = findAuthenticatedUserOrDie();
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
    @Path("/computeDataFileHashValue/{fileId}/algorithm/{alg}")
    public Response computeDataFileHashValue(@PathParam("fileId") String fileId, @PathParam("alg") String alg) {

        try {
            User u = findAuthenticatedUserOrDie();
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
    @Path("/validateDataFileHashValue/{fileId}")
    public Response validateDataFileHashValue(@PathParam("fileId") String fileId) {

        try {
            User u = findAuthenticatedUserOrDie();
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

    @GET
    @Path("/submitDataVersionToArchive/{id}/{version}")
    public Response submitDatasetVersionToArchive(@PathParam("id") String dsid, @PathParam("version") String versionNumber) {

        try {
            AuthenticatedUser au = findAuthenticatedUserOrDie();
			// Note - the user is being set in the session so it becomes part of the
			// DataverseRequest and is sent to the back-end command where it is used to get
			// the API Token which is then used to retrieve files (e.g. via S3 direct
			// downloads) to create the Bag
            session.setUser(au); // TODO: Stop using session. Use createDataverseRequest instead.
            Dataset ds = findDatasetOrDie(dsid);

            DatasetVersion dv = datasetversionService.findByFriendlyVersionNumber(ds.getId(), versionNumber);
            if (dv.getArchivalCopyLocation() == null) {
                String className = settingsService.getValueForKey(SettingsServiceBean.Key.ArchiverClassName);
                AbstractSubmitToArchiveCommand cmd = ArchiverUtil.createSubmitToArchiveCommand(className, dvRequestService.getDataverseRequest(), dv);
                if (cmd != null) {
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                DatasetVersion dv = commandEngine.submit(cmd);
                                if (dv.getArchivalCopyLocation() != null) {
                                    logger.info("DatasetVersion id=" + ds.getGlobalId().toString() + " v" + versionNumber + " submitted to Archive at: "
                                            + dv.getArchivalCopyLocation());
                                } else {
                                    logger.severe("Error submitting version due to conflict/error at Archive");
                                }
                            } catch (CommandException ex) {
                                logger.log(Level.SEVERE, "Unexpected Exception calling  submit archive command", ex);
                            }
                        }
                    }).start();
                    return ok("Archive submission using " + cmd.getClass().getCanonicalName() + " started. Processing can take significant time for large datasets. View log and/or check archive for results.");
                } else {
                    logger.log(Level.SEVERE, "Could not find Archiver class: " + className);
                    return error(Status.INTERNAL_SERVER_ERROR, "Could not find Archiver class: " + className);
                }
            } else {
                return error(Status.BAD_REQUEST, "Version already archived at: " + dv.getArchivalCopyLocation());
            }
        } catch (WrappedResponse e1) {
            return error(Status.UNAUTHORIZED, "api key required");
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
		Query deleteQuery = em.createNativeQuery("DELETE FROM metric where metricname = ?");
		deleteQuery.setParameter(1, name);
		deleteQuery.executeUpdate();
		return ok("metric cache " + name + " cleared.");
	}

    @GET
    @Path("/dataverse/{alias}/addRoleAssignmentsToChildren")
    public Response addRoleAssignementsToChildren(@PathParam("alias") String alias) throws WrappedResponse {
        Dataverse owner = dataverseSvc.findByAlias(alias);
        if (owner == null) {
            return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
        }
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
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
    @Path("/dataverse/{alias}/storageDriver")
    public Response getStorageDriver(@PathParam("alias") String alias) throws WrappedResponse {
    	Dataverse dataverse = dataverseSvc.findByAlias(alias);
    	if (dataverse == null) {
    		return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
    	}
    	try {
    		AuthenticatedUser user = findAuthenticatedUserOrDie();
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
    @Path("/dataverse/{alias}/storageDriver")
    public Response setStorageDriver(@PathParam("alias") String alias, String label) throws WrappedResponse {
    	Dataverse dataverse = dataverseSvc.findByAlias(alias);
    	if (dataverse == null) {
    		return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
    	}
    	try {
    		AuthenticatedUser user = findAuthenticatedUserOrDie();
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
    @Path("/dataverse/{alias}/storageDriver")
    public Response resetStorageDriver(@PathParam("alias") String alias) throws WrappedResponse {
    	Dataverse dataverse = dataverseSvc.findByAlias(alias);
    	if (dataverse == null) {
    		return error(Response.Status.NOT_FOUND, "Could not find dataverse based on alias supplied: " + alias + ".");
    	}
    	try {
    		AuthenticatedUser user = findAuthenticatedUserOrDie();
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
    @Path("/dataverse/storageDrivers")
    public Response listStorageDrivers() throws WrappedResponse {
    	try {
    		AuthenticatedUser user = findAuthenticatedUserOrDie();
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
                return ok("Banner Message added successfully.");

        } catch (Exception e) {
            logger.warning("Unexpected Exception: " + e.getMessage());
            return error(Status.BAD_REQUEST, "Add Banner Message unexpected exception: " + e.getMessage());
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

        return ok(bannerMessageService.findAllBannerMessages().stream()
                .map(m -> jsonObjectBuilder().add("id", m.getId()).add("displayValue", m.getDisplayValue()))
                .collect(toJsonArray()));

    }
    
}
