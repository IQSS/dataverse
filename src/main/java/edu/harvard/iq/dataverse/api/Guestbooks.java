package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.GuestbookServiceBean;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateGuestbookCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJB;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;

@Path("guestbooks")
public class Guestbooks extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Guestbooks.class.getCanonicalName());

    @EJB
    GuestbookServiceBean guestbookService;

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

    @POST
    @AuthRequired
    @Path("{identifier}")
    public Response createGuestbook(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier, String jsonBody) {
            return response(req -> {
                Dataverse dataverse = findDataverseOrDie(identifier);
                logger.severe(">>> jsonBody " + jsonBody);
                Guestbook guestbook = new Guestbook();
                guestbook.setDataverse(dataverse);
                try {
                    JsonObject jsonObj = JsonUtil.getJsonObject(jsonBody);
                    jsonParser().parseGuestbook(jsonObj, guestbook);
                } catch (JsonException | JsonParseException ex) {
                    return badRequest(ex.getMessage());
                }
                guestbook.setCreateTime(Timestamp.from(Instant.now()));
                execCommand(new CreateGuestbookCommand(guestbook, req, dataverse));
                return ok("Guestbook " + guestbook.getId() + " created");
            }, getRequestUser(crc));
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
}
