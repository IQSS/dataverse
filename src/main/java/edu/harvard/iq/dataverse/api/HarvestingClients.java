package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.CreateHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetHarvestingClientCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateHarvestingClientCommand;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import javax.json.JsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("harvest/clients")
public class HarvestingClients extends AbstractApiBean {

    
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
    @Path("/")
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
        
        // See the comment in the harvestingClients() (plural) above for the explanation
        // of why we are looking up the client twice (tl;dr: to utilize the 
        // authorization logic in the command)
        
        HarvestingClient retrievedHarvestingClient = null; 
        
        try {
            // findUserOrDie() and execCommand() both throw WrappedResponse 
            // exception, that already has a proper HTTP response in it. 
            
            retrievedHarvestingClient = execCommand(new GetHarvestingClientCommand(createDataverseRequest(findUserOrDie()), harvestingClient));
            logger.fine("retrieved Harvesting Client " + retrievedHarvestingClient.getName() + " with the GetHarvestingClient command.");
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
        // Note that we don't check the user's authorization within the API 
        // method. Insetead, we will end up reporting a "not authorized" 
        // exception thrown by the Command, if this user has no permission 
        // to perform the action. 
 
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            JsonObject json = Json.createReader(rdr).readObject();
            
            // Check that the client with this name doesn't exist yet: 
            // (we could simply let the command fail, but that does not result 
            // in a pretty report to the end user)
            
            HarvestingClient lookedUpClient = null; 
            try {
                lookedUpClient = harvestingClientService.findByNickname(nickName);
            } catch (Exception ex) {
                logger.warning("Exception caught looking up harvesting client " + nickName + ": " + ex.getMessage());
                // let's hope that this was a fluke of some kind; we'll proceed
                // with the attempt to create a new client and report an error
                // if that fails too.
            }
            
            if (lookedUpClient != null) {
                return error(Response.Status.BAD_REQUEST, "Harvesting client " + nickName + " already exists");
            }
            
            HarvestingClient harvestingClient = new HarvestingClient();

            String dataverseAlias = jsonParser().parseHarvestingClient(json, harvestingClient);
            
            if (dataverseAlias == null) {
                return error(Response.Status.BAD_REQUEST, "dataverseAlias must be supplied"); 
            }
            
            // Check if the dataverseAlias supplied is valid, i.e. corresponds 
            // to an existing dataverse (collection): 
            Dataverse ownerDataverse = dataverseSvc.findByAlias(dataverseAlias);
            if (ownerDataverse == null) {
                return error(Response.Status.BAD_REQUEST, "No such dataverse: " + dataverseAlias);
            }
            
            // The nickname supplied as part of the Rest path takes precedence: 
            harvestingClient.setName(nickName);
            
            // Populate the description field, if none is supplied: 
            if (harvestingClient.getArchiveDescription() == null) {
                harvestingClient.setArchiveDescription(BundleUtil.getStringFromBundle("harvestclients.viewEditDialog.archiveDescription.default.generic"));
            }
            
            if (StringUtil.isEmpty(harvestingClient.getArchiveUrl())
                    || StringUtil.isEmpty(harvestingClient.getHarvestingUrl())
                    || StringUtil.isEmpty(harvestingClient.getMetadataPrefix())) {
                    return error(Response.Status.BAD_REQUEST, "Required fields harvestUrl, archiveUrl and metadataFormat must be supplied");
            }
            
            harvestingClient.setDataverse(ownerDataverse);
            if (ownerDataverse.getHarvestingClientConfigs() == null) {
                ownerDataverse.setHarvestingClientConfigs(new ArrayList<>());
            }
            ownerDataverse.getHarvestingClientConfigs().add(harvestingClient);
                        
            DataverseRequest req = createDataverseRequest(findUserOrDie());
            harvestingClient = execCommand(new CreateHarvestingClientCommand(req, harvestingClient));
            return created( "/harvest/clients/" + nickName, harvestingConfigAsJson(harvestingClient));
                    
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
            
            HarvestingClient newHarvestingClient = new HarvestingClient(); 
            String newDataverseAlias = jsonParser().parseHarvestingClient(json, newHarvestingClient);
            
            if (newDataverseAlias != null 
                    && !newDataverseAlias.equals("")
                    && !newDataverseAlias.equals(ownerDataverseAlias)) {
                return error(Response.Status.BAD_REQUEST, "Bad \"dataverseAlias\" supplied. Harvesting client "+nickName+" belongs to the dataverse "+ownerDataverseAlias);
            }
            
            // Go through the supported editable fields and update the client accordingly: 
            
            if (newHarvestingClient.getHarvestingUrl() != null) {
                harvestingClient.setHarvestingUrl(newHarvestingClient.getHarvestingUrl());
            }
            if (newHarvestingClient.getHarvestingSet() != null) {
                harvestingClient.setHarvestingSet(newHarvestingClient.getHarvestingSet());
            }
            if (newHarvestingClient.getMetadataPrefix() != null) {
                harvestingClient.setMetadataPrefix(newHarvestingClient.getMetadataPrefix());
            }
            if (newHarvestingClient.getArchiveUrl() != null) {
                harvestingClient.setArchiveUrl(newHarvestingClient.getArchiveUrl());
            }
            if (newHarvestingClient.getArchiveDescription() != null) {
                harvestingClient.setArchiveDescription(newHarvestingClient.getArchiveDescription());
            }
            if (newHarvestingClient.getHarvestStyle() != null) {
                harvestingClient.setHarvestStyle(newHarvestingClient.getHarvestStyle());
            }
            // TODO: Make schedule configurable via this API too. 
            
            harvestingClient = execCommand( new UpdateHarvestingClientCommand(req, harvestingClient));
            return ok( "/harvest/clients/" + nickName, harvestingConfigAsJson(harvestingClient));
                    
        } catch (JsonParseException ex) {
            return error( Response.Status.BAD_REQUEST, "Error parsing harvesting client: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
        
    }
    
    @DELETE
    @Path("{nickName}")
    public Response deleteHarvestingClient(@PathParam("nickName") String nickName) throws IOException {
        // Deleting a client can take a while (if there's a large amnount of 
        // harvested content associated with it). So instead of calling the command
        // directly, we will be calling an async. service bean method. 
        // Without the command engine taking care of authorization, we'll need
        // to check if the user has the right to do this explicitly:
        
        try {
            User u = findUserOrDie();
            if ((!(u instanceof AuthenticatedUser) || !u.isSuperuser())) {
                throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "Only superusers can delete harvesting clients."));
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        
        HarvestingClient harvestingClient = null; 
       
        try {
            harvestingClient = harvestingClientService.findByNickname(nickName);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up harvesting client " + nickName + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up harvesting client " + nickName);
        }
        
        if (harvestingClient == null) {
            return error(Response.Status.NOT_FOUND, "Harvesting client " + nickName + " not found.");
        }
        
        // Check if the client is in a state where it can be safely deleted: 
        
        if (harvestingClient.isDeleteInProgress()) {
            return error( Response.Status.BAD_REQUEST, "Harvesting client " + nickName + " is already being deleted (in progress)");
        }
        
        if (harvestingClient.isHarvestingNow()) {
            return error( Response.Status.BAD_REQUEST, "It is not safe to delete client " + nickName + " while a harvesting job is in progress");
        }
        
        // Finally, delete it (asynchronously): 
        
        try {
            harvestingClientService.deleteClient(harvestingClient.getId());
        } catch (Exception ex) {
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to delete harvesting client " + nickName);
        }
        
        
        return ok("Harvesting Client " + nickName + ": delete in progress");
    }
    
    
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
                add("style", harvestingConfig.getHarvestStyle()).
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
