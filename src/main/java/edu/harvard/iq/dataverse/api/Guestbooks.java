package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateGuestbookCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.json.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

@Path("guestbooks")
public class Guestbooks extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Guestbooks.class.getCanonicalName());

    @EJB
    GuestbookServiceBean guestbookService;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;

    @GET
    @AuthRequired
    @Path("{id}")
    public Response getGuestbook(@Context ContainerRequestContext crc, @PathParam("id") Long id) {
        return response( req -> {
            final Guestbook retrieved = guestbookService.find(id);
            if (retrieved != null) {
                final JsonObjectBuilder jsonbuilder = json(retrieved);
                return ok(jsonbuilder);
            } else {
                return notFound(BundleUtil.getStringFromBundle("dataset.manageGuestbooks.message.notFound"));
            }
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/list")
    public Response getGuestbooks(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier, @QueryParam("includeStats") boolean includeStats) {
        return response( req -> {
            Dataverse dataverse = findDataverseOrDie(identifier);
            final Long dataverseId = dataverse.getId();
            if (!permissionSvc.request(req).on(dataverse).has(Permission.EditDataverse)) {
                return error(Response.Status.FORBIDDEN, "Not authorized");
            }
            List<Guestbook> guestbooks = guestbookService.findGuestbooksForGivenDataverse(dataverse);
            JsonArrayBuilder guestbookArray = Json.createArrayBuilder();
            JsonPrinter jsonPrinter = new JsonPrinter();
            for (Guestbook gb : guestbooks) {
                if (includeStats) {
                    gb.setUsageCount(guestbookService.findCountUsages(gb.getId(), dataverseId));
                    gb.setResponseCount(guestbookResponseService.findCountByGuestbookId(gb.getId(), dataverseId));
                }
                guestbookArray.add(jsonPrinter.json(gb));
            }
            return ok(guestbookArray);
        }, getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}")
    public Response createGuestbook(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier, String jsonBody) {

        try {
            Dataverse dataverse = findDataverseOrDie(identifier);
            AuthenticatedUser u = getRequestAuthenticatedUserOrDie(crc);
            DataverseRequest req = createDataverseRequest(u);
            if (!permissionSvc.request(req).on(dataverse).has(Permission.EditDataverse)) {
                    return error(Response.Status.FORBIDDEN, "Not authorized");
            }

            Guestbook guestbook = new Guestbook();
            guestbook.setDataverse(dataverse);
            try {
                JsonObject jsonObj = JsonUtil.getJsonObject(jsonBody);
                jsonParser().parseGuestbook(jsonObj, guestbook);
            } catch (JsonException | JsonParseException ex) {
                logger.log(Level.WARNING, "Error parsing guestbook JSON", ex);
                return badRequest("Error parsing guestbook JSON");
            }
            guestbook.setCreateTime(Timestamp.from(Instant.now()));
            Guestbook newGuestbook = execCommand(new CreateGuestbookCommand(guestbook, req, dataverse));
            return created("/guestbooks/" + newGuestbook.getId(), json(newGuestbook));
        } catch (WrappedResponse ww) {
            return handleWrappedResponse(ww);
        } catch (EJBException ex) {
            return handleEJBException(ex, "Error creating guestbook.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error creating guestbook", ex);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error creating guestbook: " + ex.getMessage());
        }
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/{id}/enabled")
    public Response enableGuestbook(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier, @PathParam("id") String id, String body) {
        body = body.trim();
        if (!Util.isBoolean(body)) {
            return badRequest("Illegal value '" + body + "'. Use 'true' or 'false'");
        }
        Long guestbookId;
        try {
            guestbookId = Long.parseLong(id);
        } catch (NumberFormatException nfe) {
            return badRequest("Illegal id '" + id + "'");
        }
        boolean enabled = Util.isTrue(body);
        return response( req -> {
            Dataverse dataverse = findDataverseOrDie(identifier);
            if (!permissionSvc.request(req).on(dataverse).has(Permission.EditDataverse)) {
                return error(Response.Status.FORBIDDEN, "Not authorized");
            }
            List<Guestbook> guestbooks = dataverse.getGuestbooks();
            if (guestbooks != null) {
                for (Guestbook guestbook : guestbooks) {
                    if (guestbook.getId() == guestbookId) { // Ignore the fact the enable flag might not change. Just return ok
                        guestbook.setEnabled(enabled);
                        execCommand(new UpdateGuestbookCommand(guestbook, req, dataverse));
                        return ok("Guestbook " + guestbookId + " enabled=" + enabled);
                    }
                }
            }
            return notFound("Guestbook " + guestbookId + " not found.");
        }, getRequestUser(crc));
    }
    private Response handleWrappedResponse(WrappedResponse ww) {
        String error = ConstraintViolationUtil.getErrorStringForConstraintViolations(ww.getCause());
        if (!error.isEmpty()) {
            logger.log(Level.INFO, error);
            return ww.refineResponse(error);
        }
        return ww.getResponse();
    }

    private Response handleEJBException(EJBException ex, String action) {
        Throwable cause = ex;
        StringBuilder sb = new StringBuilder();
        sb.append(action);
        while (cause.getCause() != null) {
            cause = cause.getCause();
            if (cause instanceof ConstraintViolationException) {
                sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
            }
        }
        logger.log(Level.SEVERE, sb.toString());
        return error(Response.Status.INTERNAL_SERVER_ERROR, sb.toString());
    }
}
