package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("harvest/datasets")
public class HarvestDatasets extends AbstractApiBean {

    private DatasetService datasetService;

    // -------------------- CONSTRUCTORS --------------------

    public HarvestDatasets() { }

    @Inject
    public HarvestDatasets(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    // -------------------- LOGIC --------------------

    @Path("/markForReharvest")
    @GET
    @ApiWriteOperation
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAllDatasetsLastChangeTime() {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
        } catch (WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
        datasetService.updateAllLastChangeForExporterTime();
        return Response.ok().build();
    }

    @Path("/markForReharvest/{id}")
    @GET
    @ApiWriteOperation
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDatasetLastChangeTime(@PathParam("id") String id) {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
            Dataset dataset = findDatasetOrDie(id);
            datasetService.updateLastChangeForExporterTime(dataset);
            return Response.ok().build();
        } catch (WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
    }
}
