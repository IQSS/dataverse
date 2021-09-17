package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientDao;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.json.JsonParseException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.common.NullSafeJsonBuilder.jsonObjectBuilder;

@Stateless
@Path("harvest/clients")
public class HarvestingClients extends AbstractApiBean {


    @EJB
    DataverseDao dataverseDao;
    @EJB
    HarvesterServiceBean harvesterService;
    @EJB
    HarvestingClientDao harvestingClientService;

    private static final Logger logger = Logger.getLogger(HarvestingClients.class.getName());

    /***
     *  /api/harvest/clients
     *  and
     *  /api/harvest/clients/{nickname}
     *  will, by default, return a JSON record with the information about the
     *  configured remote archives.
     *  optionally, plain text output may be provided as well.
     */
    @GET
    @Path("/")
    public Response harvestingClients(@QueryParam("key") String apiKey) throws WrappedResponse {

        findSuperuserOrDie();

        List<HarvestingClient> harvestingClients;
        try {
            harvestingClients = harvestingClientService.getAllHarvestingClients();
        } catch (Exception ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Caught an exception looking up configured harvesting clients; " + ex.getMessage());
        }

        if (harvestingClients == null) {
            // returning an empty list:
            return ok(jsonObjectBuilder().add("harvestingClients", ""));
        }

        JsonArrayBuilder hcArr = Json.createArrayBuilder();
        harvestingClients.forEach(client -> hcArr.add(harvestingConfigAsJson(client)));

        return ok(jsonObjectBuilder().add("harvestingClients", hcArr));
    }

    @GET
    @Path("{nickName}")
    public Response harvestingClient(@PathParam("nickName") String nickName, @QueryParam("key") String apiKey) throws IOException, WrappedResponse {

        findSuperuserOrDie();

        HarvestingClient harvestingClient;
        try {
            harvestingClient = harvestingClientService.findByNickname(nickName);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up harvesting client " + nickName + ": " + ex.getMessage());
            return error(Response.Status.BAD_REQUEST, "Internal error: failed to look up harvesting client " + nickName + ".");
        }

        if (harvestingClient == null) {
            return error(Response.Status.NOT_FOUND, "Harvesting client " + nickName + " not found.");
        }

        try {
            return ok(harvestingConfigAsJson(harvestingClient));
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while trying to format harvesting client config as json: " + ex.getMessage());
            return error(Response.Status.BAD_REQUEST,
                         "Internal error: failed to produce output for harvesting client " + nickName + ".");
        }
    }

    @POST
    @ApiWriteOperation
    @Path("{nickName}")
    public Response createHarvestingClient(String jsonBody, @PathParam("nickName") String nickName, @QueryParam("key") String apiKey) throws WrappedResponse {

        AuthenticatedUser superuser = findSuperuserOrDie();

        try (StringReader rdr = new StringReader(jsonBody)) {
            JsonObject json = Json.createReader(rdr).readObject();

            HarvestingClient harvestingClient = new HarvestingClient();
            // TODO: check that it doesn't exist yet...
            harvestingClient.setName(nickName);
            String dataverseAlias = jsonParser().parseHarvestingClient(json, harvestingClient);
            Dataverse ownerDataverse = dataverseDao.findByAlias(dataverseAlias);

            if (ownerDataverse == null) {
                return error(Response.Status.BAD_REQUEST, "No such dataverse: " + dataverseAlias);
            }

            harvestingClient.setDataverse(ownerDataverse);
            if (ownerDataverse.getHarvestingClientConfigs() == null) {
                ownerDataverse.setHarvestingClientConfigs(new ArrayList<>());
            }
            ownerDataverse.getHarvestingClientConfigs().add(harvestingClient);

            DataverseRequest req = createDataverseRequest(superuser);
            HarvestingClient managedHarvestingClient = execCommand(new CreateHarvestingClientCommand(req, harvestingClient));
            return created("/harvest/clients/" + nickName, harvestingConfigAsJson(managedHarvestingClient));

        } catch (JsonParseException ex) {
            return error(Response.Status.BAD_REQUEST, "Error parsing harvesting client: " + ex.getMessage());

        } catch (WrappedResponse ex) {
            return ex.getResponse();

        }

    }

    @PUT
    @ApiWriteOperation
    @Path("{nickName}")
    public Response modifyHarvestingClient(String jsonBody, @PathParam("nickName") String nickName, @QueryParam("key") String apiKey) throws WrappedResponse {

        AuthenticatedUser superuser = findSuperuserOrDie();

        HarvestingClient harvestingClient;
        try {
            harvestingClient = harvestingClientService.findByNickname(nickName);
        } catch (Exception ex) {
            // We don't care what happened; we'll just assume we couldn't find it. 
            harvestingClient = null;
        }

        if (harvestingClient == null) {
            return error(Response.Status.NOT_FOUND, "Harvesting client " + nickName + " not found.");
        }

        String ownerDataverseAlias = harvestingClient.getDataverse().getAlias();

        try (StringReader rdr = new StringReader(jsonBody)) {
            DataverseRequest req = createDataverseRequest(superuser);
            JsonObject json = Json.createReader(rdr).readObject();

            String newDataverseAlias = jsonParser().parseHarvestingClient(json, harvestingClient);

            if (newDataverseAlias != null
                    && !newDataverseAlias.equals("")
                    && !newDataverseAlias.equals(ownerDataverseAlias)) {
                return error(Response.Status.BAD_REQUEST, "Bad \"dataverseAlias\" supplied. Harvesting client " + nickName + " belongs to the dataverse " + ownerDataverseAlias);
            }
            HarvestingClient managedHarvestingClient = execCommand(new UpdateHarvestingClientCommand(req, harvestingClient));
            return created("/datasets/" + nickName, harvestingConfigAsJson(managedHarvestingClient));

        } catch (JsonParseException ex) {
            return error(Response.Status.BAD_REQUEST, "Error parsing harvesting client: " + ex.getMessage());

        } catch (WrappedResponse ex) {
            return ex.getResponse();

        }

    }


    /**
     * This POST starts a new harvesting run:
     */
    @POST
    @ApiWriteOperation
    @Path("{nickName}/run")
    public Response startHarvestingJob(@PathParam("nickName") String clientNickname, @QueryParam("key") String apiKey) throws WrappedResponse {

            AuthenticatedUser superuser = findSuperuserOrDie();

        try {
            HarvestingClient harvestingClient = harvestingClientService.findByNickname(clientNickname);

            if (harvestingClient == null) {
                return error(Response.Status.NOT_FOUND, "No such dataverse: " + clientNickname);
            }

            DataverseRequest dataverseRequest = createDataverseRequest(superuser);
            harvesterService.doAsyncHarvest(dataverseRequest, harvestingClient);

        } catch (Exception e) {
            return error(Response.Status.BAD_REQUEST, "Exception thrown when running harvesting client\"" + clientNickname + "\" via REST API; " + e.getMessage());
        }
        return this.accepted();
    }

    public static JsonObjectBuilder harvestingConfigAsJson(HarvestingClient harvestingConfig) {
        if (harvestingConfig == null) {
            return null;
        }


        return jsonObjectBuilder().add("nickName", harvestingConfig.getName()).
                add("dataverseAlias", harvestingConfig.getDataverse().getAlias()).
                add("type", harvestingConfig.getHarvestType()).
                add("harvestUrl", harvestingConfig.getHarvestingUrl()).
                add("archiveUrl", harvestingConfig.getArchiveUrl()).
                add("metadataFormat", harvestingConfig.getMetadataPrefix()).
                add("set", harvestingConfig.getHarvestingSet() == null ? "N/A" : harvestingConfig.getHarvestingSet()).
                add("schedule", harvestingConfig.isScheduled() ? harvestingConfig.getScheduleDescription() : "none").
                add("status", harvestingConfig.isHarvestingNow() ? "inProgress" : "inActive").
                add("lastHarvest", harvestingConfig.getLastHarvestTime() == null ? "N/A" : harvestingConfig.getLastHarvestTime().toString()).
                add("lastResult", harvestingConfig.getLastResult()).
                add("lastSuccessful", harvestingConfig.getLastSuccessfulHarvestTime() == null ? "N/A" : harvestingConfig.getLastSuccessfulHarvestTime().toString()).
                add("lastNonEmpty", harvestingConfig.getLastNonEmptyHarvestTime() == null ? "N/A" : harvestingConfig.getLastNonEmptyHarvestTime().toString()).
                add("lastDatasetsHarvested", harvestingConfig.getLastHarvestedDatasetCount() == null ? "N/A" : harvestingConfig.getLastHarvestedDatasetCount().toString()).
                add("lastDatasetsDeleted", harvestingConfig.getLastDeletedDatasetCount() == null ? "N/A" : harvestingConfig.getLastDeletedDatasetCount().toString()).
                add("lastDatasetsFailed", harvestingConfig.getLastFailedDatasetCount() == null ? "N/A" : harvestingConfig.getLastFailedDatasetCount().toString());
    }
}
