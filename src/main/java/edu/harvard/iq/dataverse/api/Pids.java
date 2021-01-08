package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePidCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ReservePidCommand;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPid(@QueryParam("persistentId") String persistentId) {
        try {
            User user = findUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("api.errors.invalidApiToken"));
        }
        String baseUrl = systemConfig.getDataCiteRestApiUrlString();
        String username = System.getProperty("doi.username");
        String password = System.getProperty("doi.password");
        try {
            JsonObjectBuilder result = PidUtil.queryDoi(persistentId, baseUrl, username, password);
            return ok(result);
        } catch (NotFoundException ex) {
            return error(ex.getResponse().getStatusInfo().toEnum(), ex.getLocalizedMessage());
        } catch (InternalServerErrorException ex) {
            return error(ex.getResponse().getStatusInfo().toEnum(), ex.getLocalizedMessage());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("unreserved")
    public Response getUnreserved(@QueryParam("persistentId") String persistentId) {
        try {
            User user = findUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("admin.api.auth.mustBeSuperUser"));
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("api.errors.invalidApiToken"));
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/reserve")
    public Response reservePid(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            execCommand(new ReservePidCommand(createDataverseRequest(findUserOrDie()), dataset));
            return ok(BundleUtil.getStringFromBundle("pids.api.reservePid.success", Arrays.asList(dataset.getGlobalId().asString())));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/delete")
    public Response deletePid(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            //Restrict to never-published datasets (that should have draft/nonpublic pids). The underlying code will invalidate
            //pids that have been made public by a pid-specific method, but it's not clear that invalidating such a pid via an api that doesn't
            //destroy the dataset is a good idea.
            if(dataset.isReleased()) {
            	return badRequest("Not allowed for Datasets that have been published.");
            }
            execCommand(new DeletePidCommand(createDataverseRequest(findUserOrDie()), dataset));
            return ok(BundleUtil.getStringFromBundle("pids.api.deletePid.success", Arrays.asList(dataset.getGlobalId().asString())));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

}
