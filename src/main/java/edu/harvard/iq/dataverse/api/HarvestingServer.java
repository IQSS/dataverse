/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import javax.json.JsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
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

/**
 *
 * @author Leonid Andreev
 */
@Stateless
@Path("harvest/server/oaisets")
public class HarvestingServer extends AbstractApiBean {
    @EJB
    OAISetServiceBean oaiSetService;

    private static final Logger logger = Logger.getLogger(HarvestingServer.class.getName()); 
    
    // TODO: this should be available to admin only.

    @GET
    @Path("")
    public Response oaiSets(@QueryParam("key") String apiKey) throws IOException {
        

        List<OAISet> oaiSets = null;
        try {
            oaiSets = oaiSetService.findAll();
        } catch (Exception ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Caught an exception looking up available OAI sets; " + ex.getMessage());
        }

        if (oaiSets == null) {
            // returning an empty list:
            return ok(jsonObjectBuilder().add("oaisets", ""));
        }

        JsonArrayBuilder hcArr = Json.createArrayBuilder();

        for (OAISet set : oaiSets) {
            hcArr.add(oaiSetAsJson(set));
        }

        return ok(jsonObjectBuilder().add("oaisets", hcArr));
    }
    
    @GET
    @Path("{specname}")
    public Response oaiSet(@PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException {
        
        OAISet set = null;  
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up OAI set " + spec + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up OAI set " + spec + ".");
        }
        
        if (set == null) {
            return error(Response.Status.NOT_FOUND, "OAI set " + spec + " not found.");
        }
                
        try {
            return ok(oaiSetAsJson(set));  
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while trying to format OAI set " + spec + " as json: "+ex.getMessage());
            return error( Response.Status.BAD_REQUEST, 
                    "Internal error: failed to produce output for OAI set " + spec + ".");
        }
    }
    
    @POST
    @Path("{specname}")
    public Response createOaiSet(String jsonBody, @PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        
        //try () { 
            StringReader rdr = new StringReader(jsonBody);
            JsonObject json = Json.createReader(rdr).readObject();
            
            OAISet set = new OAISet();
            // TODO: check that it doesn't exist yet...
            set.setSpec(spec);
            // TODO: jsonParser().parseOaiSet(json, set);
            
            oaiSetService.save(set);
            
            return created( "/harvest/server/oaisets" + spec, oaiSetAsJson(set));
                    
        //} catch (JsonParseException ex) {
          //  return errorResponse( Response.Status.BAD_REQUEST, "Error parsing OAI set: " + ex.getMessage() );
            
        //} catch (WrappedResponse ex) {
        //    return ex.getResponse();  
        //}
    }

    @PUT
    @Path("{nickName}")
    public Response modifyOaiSet(String jsonBody, @PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        // TODO:
        // ...
        return created("/harvest/server/oaisets" + spec, null);
    }
    
    @DELETE
    @Path("{specname}")
    public Response deleteOaiSet(@PathParam("specname") String spec, @QueryParam("key") String apiKey) {
        OAISet set = null;  
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up OAI set " + spec + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up OAI set " + spec + ".");
        }
        
        if (set == null) {
            return error(Response.Status.NOT_FOUND, "OAI set " + spec + " not found.");
        }
        
        try {
            oaiSetService.setDeleteInProgress(set.getId());
            oaiSetService.remove(set.getId());
        } catch (Exception ex) {
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to delete OAI set " + spec + "; " + ex.getMessage());
        }
        
        return ok("OAI Set " + spec + " deleted");
    
    }
    
    @GET
    @Path("{specname}/datasets")
    public Response oaiSetListDatasets(@PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException {
        OAISet set = null;  
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up OAI set " + spec + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up OAI set " + spec + ".");
        }
        
        return ok("");
        
    }
    
    /* Auxiliary, helper methods: */
    public static JsonArrayBuilder oaiSetsAsJsonArray(List<OAISet> oaiSets) {
        JsonArrayBuilder hdArr = Json.createArrayBuilder();

        for (OAISet set : oaiSets) {
            hdArr.add(oaiSetAsJson(set));
        }
        return hdArr;
    }

    public static JsonObjectBuilder oaiSetAsJson(OAISet set) {
        if (set == null) {
            return null;
        }

        return jsonObjectBuilder().add("name", set.getName()).
                add("spec", set.getSpec()).
                add("description", set.getDescription()).
                add("definition", set.getDefinition()).
                add("version", set.getVersion());
    }

}
