package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.HarvestingDataverseConfig;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import javax.json.JsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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
                return errorResponse(Response.Status.UNAUTHORIZED, "Authentication required to use this API method");
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
    
    /* 
     *  /api/harvest/status 
     *  will, by default, return a JSON record with the information about the
     *  configured remote archives. 
     *  optionally, plain text output will [/may] be provided as well.
     */
    @GET
    @Path("status")
    public Response harvestingStatus() throws IOException {
        //return this.accepted();
        
        List<Dataverse> harvestingDataverses = dataverseService.getAllHarvestedDataverses();
        if (harvestingDataverses == null) {
            return okResponse("");
        }
        
        return okResponse(jsonObjectBuilder().add("remoteArchives", harvestingConfigsAsJsonArray(harvestingDataverses)));
    } 

    public static JsonArrayBuilder harvestingConfigsAsJsonArray(List<Dataverse> harvestingDataverses) {
        JsonArrayBuilder hdArr = Json.createArrayBuilder();
        
        for (Dataverse hd : harvestingDataverses) {
            hdArr.add(harvestingConfigAsJson(hd));
        }
        return hdArr;
    }
    
    public static JsonObjectBuilder harvestingConfigAsJson(Dataverse dataverse) {
        HarvestingDataverseConfig harvestingConfig = dataverse.getHarvestingDataverseConfig();
        if (harvestingConfig == null) {
            return null; 
        }
        
        return jsonObjectBuilder().add("nickname", harvestingConfig.getName()).
                add("dataverseAlias", dataverse.getAlias()).
                add("type", harvestingConfig.getHarvestType()).
                add("harvestURL", harvestingConfig.getHarvestingUrl()).
                add("metadataFormat", harvestingConfig.getMetadataPrefix()).
                add("set", harvestingConfig.getHarvestingSet() == null ? "N/A" : harvestingConfig.getHarvestingSet()).
                add("schedule", harvestingConfig.isScheduled() ? harvestingConfig.getScheduleDescription() : "none").
                add("inProgress", harvestingConfig.isHarvestingNow() ? "yes" : "-").
                add("lastHarvest", harvestingConfig.getLastHarvestTime() == null ? "N/A" : harvestingConfig.getLastHarvestTime().toString()).
                add("lastSuccessful", harvestingConfig.getLastSuccessfulHarvestTime() == null ? "N/A" : harvestingConfig.getLastSuccessfulHarvestTime().toString()).
                add("lastNonEmpty", harvestingConfig.getLastNonEmptyHarvestTime() == null ? "N/A" : harvestingConfig.getLastNonEmptyHarvestTime().toString()).
                add("lastResult", harvestingConfig.getHarvestResult()).
                add("datasetsHarveted", harvestingConfig.getHarvestedDatasetCount() == null ? "N/A" : harvestingConfig.getHarvestedDatasetCount().toString()).
                add("datasetsDeleted", harvestingConfig.getDeletedDatasetCount() == null ? "N/A" : harvestingConfig.getDeletedDatasetCount().toString()).
                add("datasetsFailed", harvestingConfig.getFailedDatasetCount() == null ? "N/A" : harvestingConfig.getFailedDatasetCount().toString());
    }
}
