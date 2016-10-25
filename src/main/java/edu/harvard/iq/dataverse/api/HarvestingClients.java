package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.harvest.client.ClientHarvestRun;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import javax.json.JsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Stateless
@Path("harvest/clients")
public class HarvestingClients extends AbstractApiBean {

    
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    HarvesterServiceBean harvesterService;
    @EJB
    HarvestingClientServiceBean harvestingClientService;

    private static final Logger logger = Logger.getLogger(HarvestingClients.class.getName());
    /* 
     *  /api/harvest/clients
     *  and
     *  /api/harvest/clients/{nickname}
     *  will, by default, return a JSON record with the information about the
     *  configured remote archives. 
     *  optionally, plain text output may be provided as well.
     */
    @GET
    @Path("")
    public Response harvestingClients(@QueryParam("key") String apiKey) throws IOException {
        
        List<HarvestingClient> harvestingClients = null; 
        try {
            harvestingClients = harvestingClientService.getAllHarvestingClients();
        } catch (Exception ex) {
            return error( Response.Status.INTERNAL_SERVER_ERROR, "Caught an exception looking up configured harvesting clients; " + ex.getMessage() );
        }
        
        if (harvestingClients == null) {
            // returning an empty list:
            return ok(jsonObjectBuilder().add("harvestingClients",""));
        }
        
        JsonArrayBuilder hcArr = Json.createArrayBuilder();
        
        for (HarvestingClient harvestingClient : harvestingClients) {
            // We already have this harvestingClient - wny do we need to 
            // execute this "Get HarvestingClients Client Command" in order to get it, 
            // again? - the purpose of the command is to run the request through 
            // the Authorization system, to verify that they actually have 
            // the permission to view this harvesting client config. -- L.A. 4.4
            HarvestingClient retrievedHarvestingClient = null; 
            try {
                DataverseRequest req = createDataverseRequest(findUserOrDie());
                retrievedHarvestingClient = execCommand( new GetHarvestingClientCommand(req, harvestingClient));
            } catch (Exception ex) {
                // Don't do anything. 
                // We'll just skip this one - since this means the user isn't 
                // authorized to view this client configuration. 
            }
            
            if (retrievedHarvestingClient != null) {
                hcArr.add(harvestingConfigAsJson(retrievedHarvestingClient));
            }
        }
        
        return ok(jsonObjectBuilder().add("harvestingClients", hcArr));
    } 
    
    @GET
    @Path("{nickName}")
    public Response harvestingClient(@PathParam("nickName") String nickName, @QueryParam("key") String apiKey) throws IOException {
        
        HarvestingClient harvestingClient = null; 
        try {
            harvestingClient = harvestingClientService.findByNickname(nickName);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up harvesting client " + nickName + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up harvesting client " + nickName + ".");
        }
        
        if (harvestingClient == null) {
            return error(Response.Status.NOT_FOUND, "Harvesting client " + nickName + " not found.");
        }
        
        HarvestingClient retrievedHarvestingClient = null; 
        
        try {
            // findUserOrDie() and execCommand() both throw WrappedResponse 
            // exception, that already has a proper HTTP response in it. 
            
            retrievedHarvestingClient = execCommand(new GetHarvestingClientCommand(createDataverseRequest(findUserOrDie()), harvestingClient));
            logger.info("retrieved Harvesting Client " + retrievedHarvestingClient.getName() + " with the GetHarvestingClient command.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while executing GetHarvestingClientCommand: "+ex.getMessage());
            retrievedHarvestingClient = null;
        }
        
        if (retrievedHarvestingClient == null) {
            return error( Response.Status.BAD_REQUEST, 
                    "Internal error: failed to retrieve harvesting client " + nickName + ".");
        }
        
        try {
            return ok(harvestingConfigAsJson(retrievedHarvestingClient));  
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while trying to format harvesting client config as json: "+ex.getMessage());
            return error( Response.Status.BAD_REQUEST, 
                    "Internal error: failed to produce output for harvesting client " + nickName + ".");
        }
    }
    
    @POST
    @Path("{nickName}")
    public Response createHarvestingClient(String jsonBody, @PathParam("nickName") String nickName, @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            JsonObject json = Json.createReader(rdr).readObject();
            
            HarvestingClient harvestingClient = new HarvestingClient();
            // TODO: check that it doesn't exist yet...
            harvestingClient.setName(nickName);
            String dataverseAlias = jsonParser().parseHarvestingClient(json, harvestingClient);
            Dataverse ownerDataverse = dataverseService.findByAlias(dataverseAlias);
            
            if (ownerDataverse == null) {
                return error(Response.Status.BAD_REQUEST, "No such dataverse: " + dataverseAlias);
            }
            
            harvestingClient.setDataverse(ownerDataverse);
            if (ownerDataverse.getHarvestingClientConfigs() == null) {
                ownerDataverse.setHarvestingClientConfigs(new ArrayList<>());
            }
            ownerDataverse.getHarvestingClientConfigs().add(harvestingClient);
            
            DataverseRequest req = createDataverseRequest(findUserOrDie());
            HarvestingClient managedHarvestingClient = execCommand( new CreateHarvestingClientCommand(req, harvestingClient));
            return created( "/harvest/clients/" + nickName, harvestingConfigAsJson(managedHarvestingClient));
                    
        } catch (JsonParseException ex) {
            return error( Response.Status.BAD_REQUEST, "Error parsing harvesting client: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
        
    }
    
    @PUT
    @Path("{nickName}")
    public Response modifyHarvestingClient(String jsonBody, @PathParam("nickName") String nickName, @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        HarvestingClient harvestingClient = null; 
        try {
            harvestingClient = harvestingClientService.findByNickname(nickName);
        } catch (Exception ex) {
            // We don't care what happened; we'll just assume we couldn't find it. 
            harvestingClient = null;  
        }
        
        if (harvestingClient == null) {
            return error( Response.Status.NOT_FOUND, "Harvesting client " + nickName + " not found.");
        }
        
        String ownerDataverseAlias = harvestingClient.getDataverse().getAlias();
        
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            DataverseRequest req = createDataverseRequest(findUserOrDie());
            JsonObject json = Json.createReader(rdr).readObject();
            
            String newDataverseAlias = jsonParser().parseHarvestingClient(json, harvestingClient);
            
            if (newDataverseAlias != null 
                    && !newDataverseAlias.equals("")
                    && !newDataverseAlias.equals(ownerDataverseAlias)) {
                return error(Response.Status.BAD_REQUEST, "Bad \"dataverseAlias\" supplied. Harvesting client "+nickName+" belongs to the dataverse "+ownerDataverseAlias);
            }
            HarvestingClient managedHarvestingClient = execCommand( new UpdateHarvestingClientCommand(req, harvestingClient));
            return created( "/datasets/" + nickName, harvestingConfigAsJson(managedHarvestingClient));
                    
        } catch (JsonParseException ex) {
            return error( Response.Status.BAD_REQUEST, "Error parsing harvesting client: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
        
    }
    
    // TODO: 
    // add a @DELETE method 
    // (there is already a DeleteHarvestingClient command)
    
    // Methods for managing harvesting runs (jobs):
    
    
    // This POST starts a new harvesting run:
    @POST
    @Path("{nickName}/run")
    public Response startHarvestingJob(@PathParam("nickName") String clientNickname, @QueryParam("key") String apiKey) throws IOException {
        
        try {
            AuthenticatedUser authenticatedUser = null; 
            
            try {
                authenticatedUser = findAuthenticatedUserOrDie();
            } catch (WrappedResponse wr) {
                return error(Response.Status.UNAUTHORIZED, "Authentication required to use this API method");
            }
            
            if (authenticatedUser == null || !authenticatedUser.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Only the Dataverse Admin user can run harvesting jobs");
            }
            
            HarvestingClient harvestingClient = harvestingClientService.findByNickname(clientNickname);
            
            if (harvestingClient == null) {
                return error(Response.Status.NOT_FOUND, "No such dataverse: "+clientNickname);
            }
            
            DataverseRequest dataverseRequest = createDataverseRequest(authenticatedUser);
            harvesterService.doAsyncHarvest(dataverseRequest, harvestingClient);

        } catch (Exception e) {
            return this.error(Response.Status.BAD_REQUEST, "Exception thrown when running harvesting client\""+clientNickname+"\" via REST API; " + e.getMessage());
        }
        return this.accepted();
    }
    
    // This GET shows the status of the harvesting run in progress for this 
    // client, if present: 
    // @GET
    // @Path("{nickName}/run")
    // TODO: 
    
    // This DELETE kills the harvesting run in progress for this client, 
    // if present: 
    // @DELETE
    // @Path("{nickName}/run")
    // TODO: 
    
    
    
    
    
    /* Auxiliary, helper methods: */ 
    
    /*
    @Deprecated
    public static JsonArrayBuilder harvestingConfigsAsJsonArray(List<Dataverse> harvestingDataverses) {
        JsonArrayBuilder hdArr = Json.createArrayBuilder();
        
        for (Dataverse hd : harvestingDataverses) {
            hdArr.add(harvestingConfigAsJson(hd.getHarvestingClientConfig()));
        }
        return hdArr;
    }*/
    
    public static JsonObjectBuilder harvestingConfigAsJson(HarvestingClient harvestingConfig) {
        if (harvestingConfig == null) {
            return null; 
        }
        
        
        return jsonObjectBuilder().add("nickName", harvestingConfig.getName()).
                add("dataverseAlias", harvestingConfig.getDataverse().getAlias()).
                add("type", harvestingConfig.getHarvestType()).
                add("harvestUrl", harvestingConfig.getHarvestingUrl()).
                add("archiveUrl", harvestingConfig.getArchiveUrl()).
                add("archiveDescription",harvestingConfig.getArchiveDescription()).
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
