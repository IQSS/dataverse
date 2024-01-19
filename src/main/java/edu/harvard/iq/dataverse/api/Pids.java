package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePidCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ReservePidCommand;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * PIDs are Persistent IDentifiers such as DOIs or Handles.
 *
 * Currently PIDs can be minted at the dataset and file level but there is
 * demand for PIDs at the dataverse level too. That's why this dedicated "pids"
 * endpoint exists, to be somewhat future proof.
 */
@Stateless
@Path("pids")
public class Pids extends AbstractApiBean {

    @GET
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPid(@Context ContainerRequestContext crc, @QueryParam("persistentId") String persistentId) {
        User user = getRequestUser(crc);
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
        }

        // FIXME: Even before changing to MPCONFIG retrieval, this was pinned to be DataCite specific!
        //        Should this be extended to EZID and other PID systems like Handle?
        String baseUrl = JvmSettings.DATACITE_REST_API_URL.lookup();
        String username = JvmSettings.DATACITE_USERNAME.lookup();
        String password = JvmSettings.DATACITE_PASSWORD.lookup();
        
        try {
            GlobalId globalId = PidUtil.parseAsGlobalID(persistentId);
            JsonObjectBuilder result = PidUtil.queryDoi(globalId, baseUrl, username, password);
            return ok(result);
        } catch (NotFoundException ex) {
            return error(ex.getResponse().getStatusInfo().toEnum(), ex.getLocalizedMessage());
        } catch (InternalServerErrorException ex) {
            return error(ex.getResponse().getStatusInfo().toEnum(), ex.getLocalizedMessage());
        }
    }

    @GET
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Path("unreserved")
    public Response getUnreserved(@Context ContainerRequestContext crc, @QueryParam("persistentId") String persistentId) {
        User user = getRequestUser(crc);
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
        }

        JsonArrayBuilder unreserved = Json.createArrayBuilder();
        for (Dataset dataset : datasetSvc.findAll()) {
            if (dataset.isReleased()) {
                continue;
            }
            if (dataset.getGlobalIdCreateTime() == null) {
                unreserved.add(Json.createObjectBuilder()
                        .add("id", dataset.getId())
                        .add("pid", dataset.getGlobalId().asString())
                );
            }
        }
        JsonArray finalUnreserved = unreserved.build();
        int size = finalUnreserved.size();
        return ok(Json.createObjectBuilder()
                .add("numUnreserved", size)
                .add("count", finalUnreserved)
        );
    }

    @POST
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/reserve")
    public Response reservePid(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            execCommand(new ReservePidCommand(createDataverseRequest(getRequestUser(crc)), dataset));
            return ok(BundleUtil.getStringFromBundle("pids.api.reservePid.success", Arrays.asList(dataset.getGlobalId().asString())));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/delete")
    public Response deletePid(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            //Restrict to never-published datasets (that should have draft/nonpublic pids). The underlying code will invalidate
            //pids that have been made public by a pid-specific method, but it's not clear that invalidating such a pid via an api that doesn't
            //destroy the dataset is a good idea.
            if(dataset.isReleased()) {
            	return badRequest("Not allowed for Datasets that have been published.");
            }
            execCommand(new DeletePidCommand(createDataverseRequest(getRequestUser(crc)), dataset));
            return ok(BundleUtil.getStringFromBundle("pids.api.deletePid.success", Arrays.asList(dataset.getGlobalId().asString())));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

}
