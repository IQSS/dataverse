package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import java.io.IOException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Stateless
@Path("harvest")
public class Harvesting extends AbstractApiBean {

    
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    HarvesterServiceBean harvesterService;

    
    @GET
    @Path("run/{dataverseAlias}")
    public Response startHarvestingJob(@PathParam("dataverseAlias") String dataverseAlias, @QueryParam("key") String apiKey) throws IOException {
        
        try {
            AuthenticatedUser authenticatedUser = null; 
            
            try {
                authenticatedUser = findAuthenticatedUserOrDie();
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }
            
            if (authenticatedUser == null || !authenticatedUser.isSuperuser()) {
                return errorResponse(Response.Status.FORBIDDEN, "Only the Dataverse Admin user can run harvesting jobs");
            }
            
            Dataverse dataverse = dataverseService.findByAlias(dataverseAlias);
            
            if (dataverse == null) {
                return errorResponse(Response.Status.NOT_FOUND, "No such dataverse: "+dataverseAlias);
            }
            
            if (!dataverse.isHarvested()) {
                return errorResponse(Response.Status.BAD_REQUEST, "Not a HARVESTING dataverse: "+dataverseAlias);
            }
            
            //DataverseRequest dataverseRequest = createDataverseRequest(authenticatedUser);
            
            harvesterService.doAsyncHarvest(dataverse);

        } catch (Exception e) {
            return this.errorResponse(Response.Status.BAD_REQUEST, "Exception thrown when running a Harvest on dataverse \""+dataverseAlias+"\" via REST API; " + e.getMessage());
        }
        return this.accepted();
    }

}
