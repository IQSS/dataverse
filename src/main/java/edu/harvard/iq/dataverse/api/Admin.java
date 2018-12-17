package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EMailValidator;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.UserIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationProviderFactoryNotFoundException;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.groups.Group;
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
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
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

import java.util.List;
import edu.harvard.iq.dataverse.authorization.AuthTestDataServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.RegisterDvObjectCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.ws.rs.QueryParam;

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
    GroupServiceBean groupService;

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
				AuthenticationProvider provider = authSvc.loadProvider(managed);
				authSvc.deregisterProvider(provider.getId());
				authSvc.registerProvider(provider);
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
				authSvc.registerProvider(authSvc.loadProvider(row));
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
			authSvc.deregisterProvider(id);
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
		authSvc.deregisterProvider(id);
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
	public Response getAuthenticatedUser(@PathParam("identifier") String identifier) {
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
			authSvc.deleteAuthenticatedUser(user.getId());
			return ok("AuthenticatedUser " + identifier + " deleted. ");
		}
		return error(Response.Status.BAD_REQUEST, "User " + identifier + " not found.");
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
	public Response filterAuthenticatedUsers(@QueryParam("searchTerm") String searchTerm,
			@QueryParam("selectedPage") Integer selectedPage, @QueryParam("itemsPerPage") Integer itemsPerPage) {

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

		String sortKey = null;
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

	@DELETE
	@Path("authenticatedUsers/id/{id}/")
	public Response deleteAuthenticatedUserById(@PathParam("id") Long id) {
		AuthenticatedUser user = authSvc.findByID(id);
		if (user != null) {
			authSvc.deleteAuthenticatedUser(user.getId());
			return ok("AuthenticatedUser " + id + " deleted. ");
		}
		return error(Response.Status.BAD_REQUEST, "User " + id + " not found.");
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

	@Path("superuser/{identifier}")
	@POST
	public Response toggleSuperuser(@PathParam("identifier") String identifier) {
		ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "toggleSuperuser")
				.setInfo(identifier);
		try {
			AuthenticatedUser user = authSvc.getAuthenticatedUser(identifier);

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
					for (ConstraintViolation<?> constraintViolation : constraintViolationException
							.getConstraintViolations()) {
						String databaseRow = constraintViolation.getLeafBean().toString();
						String field = constraintViolation.getPropertyPath().toString();
						String invalidValue = constraintViolation.getInvalidValue().toString();
						JsonObjectBuilder violation = Json.createObjectBuilder();
						violation.add("entityClassDatabaseTableRowId", databaseRow);
						violation.add("field", field);
						violation.add("invalidValue", invalidValue);
						return ok(violation);
					}
				}
				cause = cause.getCause();
			}
		}
		return ok(msg);
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
		DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail();
		data.add("isUseGenericThumbnail", dataset.isUseGenericThumbnail());
		data.add("datasetLogoPresent", DatasetUtil.isDatasetLogoPresent(dataset));
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
        System.out.print("before the return ok...");
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
						String currentChecksum = FileUtil.CalculateChecksum(in, df.getChecksumType());
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
							String newChecksum = FileUtil.CalculateChecksum(in2, cType);

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
}
