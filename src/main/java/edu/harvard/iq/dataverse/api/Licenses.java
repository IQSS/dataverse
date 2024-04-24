package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.core.Response.Status;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

/**
 * Where the licenses API calls live.
 * 
 * @author qqmyers
 */
@Stateless
@Path("licenses")
public class Licenses extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Licenses.class.getName());

    @GET
    @Path("/")
    public Response getLicenses() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (License license : licenseSvc.listAll()) {
            arrayBuilder.add(JsonPrinter.json(license));
        }
        return ok(arrayBuilder);
    }

    @GET
    @Path("/{id}")
    public Response getLicenseById(@PathParam("id") long id) {
        License license = licenseSvc.getById(id);
        if (license == null)
            return error(Response.Status.NOT_FOUND, "License with ID " + id + " not found");
        return ok(json(license));
    }

    @POST
    @AuthRequired
    @Path("/")
    public Response addLicense(@Context ContainerRequestContext crc, License license) {
        User authenticatedUser;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (!authenticatedUser.isSuperuser()) {
                return error(Status.FORBIDDEN, "must be superuser");
            }
        } catch (WrappedResponse e) {
            return error(Status.UNAUTHORIZED, "api key required");
        }
        try {
            License l = licenseSvc.save(license);
            actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "licenseAdded")
                    .setInfo("License " + l.getName() + "(" + l.getUri() + ") as id: " + l.getId() + ".")
                    .setUserIdentifier(authenticatedUser.getIdentifier()));
            return created("/api/licenses/" + l.getId(), Json.createObjectBuilder().add("message", "License created"));
        } catch (WrappedResponse e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                return error(Response.Status.BAD_REQUEST, cause.getMessage());
            } else if (cause instanceof IllegalStateException) {
                return error(Response.Status.CONFLICT, cause.getMessage());
            }
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("/default")
    public Response getDefault() {
        return ok("Default license ID is " + licenseSvc.getDefault().getId());
    }

    @PUT
    @AuthRequired
    @Path("/default/{id}")
    public Response setDefault(@Context ContainerRequestContext crc, @PathParam("id") long id) {
        User authenticatedUser;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (!authenticatedUser.isSuperuser()) {
                return error(Status.FORBIDDEN, "must be superuser");
            }
        } catch (WrappedResponse e) {
            return error(Status.UNAUTHORIZED, "api key required");
        }
        try {
            if (licenseSvc.setDefault(id) == 0) {
                return error(Response.Status.NOT_FOUND, "License with ID " + id + " not found");
            }
            License license = licenseSvc.getById(id);
            actionLogSvc
                    .log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "defaultLicenseChanged")
                            .setInfo("License " + license.getName() + "(" + license.getUri() + ") as id: " + id
                                    + "is now the default license.")
                            .setUserIdentifier(authenticatedUser.getIdentifier()));
            return ok("Default license ID set to " + id);
        } catch (WrappedResponse e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                return badRequest(e.getCause().getMessage());
            }
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PUT
    @AuthRequired
    @Path("/{id}/:active/{activeState}")
    public Response setActiveState(@Context ContainerRequestContext crc, @PathParam("id") long id, @PathParam("activeState") boolean active) {
        User authenticatedUser;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (!authenticatedUser.isSuperuser()) {
                return error(Status.FORBIDDEN, "must be superuser");
            }
        } catch (WrappedResponse e) {
            return error(Status.UNAUTHORIZED, "api key required");
        }
        try {
            if (licenseSvc.setActive(id, active) == 0) {
                return error(Response.Status.NOT_FOUND, "License with ID " + id + " not found");
            }
            License license = licenseSvc.getById(id);
            actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "licenseStateChanged")
                    .setInfo("License " + license.getName() + "(" + license.getUri() + ") as id: " + id
                            + "has been made " + (active ? "active" : "inactive"))
                    .setUserIdentifier(authenticatedUser.getIdentifier()));
            return ok("License ID " + id + " set to " + (active ? "active" : "inactive"));
        } catch (WrappedResponse e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                return badRequest(e.getCause().getMessage());
            }
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PUT
    @AuthRequired
    @Path("/{id}/:sortOrder/{sortOrder}")
    public Response setSortOrder(@Context ContainerRequestContext crc, @PathParam("id") long id, @PathParam("sortOrder") long sortOrder) {
        User authenticatedUser;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (!authenticatedUser.isSuperuser()) {
                return error(Status.FORBIDDEN, "must be superuser");
            }
        } catch (WrappedResponse e) {
            return error(Status.UNAUTHORIZED, "api key required");
        }
        try {
            if (licenseSvc.setSortOrder(id, sortOrder) == 0) {
                return error(Response.Status.NOT_FOUND, "License with ID " + id + " not found");
            }
            License license = licenseSvc.getById(id);
            actionLogSvc
                    .log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "sortOrderLicenseChanged")
                            .setInfo("License " + license.getName() + "(" + license.getUri() + ") as id: " + id
                                    + "has now sort order " + sortOrder + ".")
                            .setUserIdentifier(authenticatedUser.getIdentifier()));
            return ok("License ID " + id + " sort order set to " + sortOrder);
        } catch (WrappedResponse e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                return badRequest(e.getCause().getMessage());
            }
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DELETE
    @AuthRequired
    @Path("/{id}")
    public Response deleteLicenseById(@Context ContainerRequestContext crc, @PathParam("id") long id) {
        User authenticatedUser;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            if (!authenticatedUser.isSuperuser()) {
                return error(Status.FORBIDDEN, "must be superuser");
            }
        } catch (WrappedResponse e) {
            return error(Status.UNAUTHORIZED, "api key required");
        }
        try {
            License license = licenseSvc.getById(id);
            if (license == null) {
                return error(Status.NOT_FOUND, "License with ID " + id + " not found.");
            } else if (license.isDefault()) {
                return error(Status.CONFLICT, "Please make sure the license is not the default before deleting it.");
            } else {
                if (licenseSvc.deleteById(id) == 1) {
                    actionLogSvc
                            .log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "licenseDeleted")
                                    .setInfo("License " + license.getName() + "(" + license.getUri() + ") as id: " + id
                                            + " has been deleted.")
                                    .setUserIdentifier(authenticatedUser.getIdentifier()));
                    return ok("OK. License with ID " + id + " was deleted.");
                } else {
                    return error(Status.CONFLICT, "Couldn't delete license with ID: " + id);
                }
            }
        } catch (WrappedResponse e) {
            if (e.getCause() instanceof IllegalStateException) {
                return error(Response.Status.CONFLICT, e.getMessage());
            }
            return error(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
