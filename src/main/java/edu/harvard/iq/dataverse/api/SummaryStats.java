package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.summarystats.SummaryStatsServiceBean;
import javax.ejb.EJB;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

// TODO: Where do we want this API endpoint to live?
@Path("summaryStats")
public class SummaryStats extends AbstractApiBean {

    @EJB
    DataFileServiceBean dataFileSvc;

    // TODO: If we like this new SummaryStatsServiceBean, move it to AbstractApiBean.
    @EJB
    SummaryStatsServiceBean summaryStatsSvc;

    @POST
    @Path("{id}")
    public Response getMapLayerMetadatas(@PathParam("id") Long fileIdSupplied) {
        AuthenticatedUser authenticatedUser;
        try {
            authenticatedUser = findAuthenticatedUserOrDie();
        } catch (WrappedResponse ex) {
            return error(FORBIDDEN, "Authorized users only.");
        }
        // FIXME: What should the permission be?
        if (!authenticatedUser.isSuperuser()) {
            return error(FORBIDDEN, "This API endpoint is so experimental that right now it is only available to superusers.");
        }
        DataFile dataFile = dataFileSvc.find(fileIdSupplied);
        if (dataFile == null) {
            return error(BAD_REQUEST, "File not found based on id " + fileIdSupplied + ".");
        }
        String jsonIn = "{}";
        boolean persisted = summaryStatsSvc.processPrepFile(jsonIn, dataFile);
        if (persisted) {
            return ok("Data has been persisted.");
        } else {
            return error(BAD_REQUEST, "Error persisting data.");
        }
    }

}
