/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import jakarta.json.JsonObjectBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 *
 * @author Leonid Andreev
 */
@Stateless
@Path("harvest/server/oaisets")
@Tag(name = "Admin", description = "Administrative Dataverse operations.")
public class HarvestingServer extends AbstractApiBean {
    @EJB
    OAISetServiceBean oaiSetService;

    private static final Logger logger = Logger.getLogger(HarvestingServer.class.getName()); 
    
    // TODO: this should be available to admin only.
    // TODO: if we decide to update the Harvesting Sets Page to use commands for 
    // Create, Modify, and Delete we should also add them here.
    
    @GET
    @Operation(summary = "Lists OAI sets",
            description = "Returns all configured OAI sets with name, spec, description, definition, and version.")
    public Response oaiSets(
            @Parameter(description = "Legacy key value accepted by the endpoint.")
            @QueryParam("key") String apiKey) throws IOException {
    
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
    @Operation(summary = "Returns an OAI set",
            description = "Returns the OAI set with the specified set specification name.")
    public Response oaiSet(
            @Parameter(description = "OAI set specification name to return.", required = true)
            @PathParam("specname") String spec,
            @Parameter(description = "Legacy key value accepted by the endpoint.")
            @QueryParam("key") String apiKey) throws IOException {
        
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

    /**
     * create an OAI set from spec in path and other parameters from POST body
     * (as JSON). {"name":$set_name,
     * "description":$optional_set_description,"definition":$set_search_query_string}.
     */
    @POST
    @AuthRequired
    @Path("/add")
    @Operation(summary = "Creates an OAI set",
            description = "Creates an OAI set from name, definition, and optional description JSON when the authenticated user is a superuser.")
    public Response createOaiSet(@Context ContainerRequestContext crc,
            @RequestBody(description = "OAI set JSON containing name, definition, and optional description.")
            String jsonBody,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        /*
	     * authorization modeled after the UI (aka HarvestingSetsPage)
         */
        AuthenticatedUser dvUser;
        try {
            dvUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (!dvUser.isSuperuser()) {            
            return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.superUser.required"));
        }

        StringReader rdr = new StringReader(jsonBody);
        
	try( JsonReader jrdr = Json.createReader(rdr) )
	{
		JsonObject json = jrdr.readObject();

		OAISet set = new OAISet();


		String name, desc, defn;

		try {
			name = json.getString("name");
		} catch (NullPointerException npe_name) {
			return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required"));
		}
                		//Validating spec 
		if (!StringUtils.isEmpty(name)) {
			if (name.length() > 30) {
				return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.sizelimit"));
			}
			if (!Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", name)) {
				return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.invalid"));
				// If it passes the regex test, check 
			}
			if (oaiSetService.findBySpec(name) != null) {
				return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.alreadyused"));
			}

		} else {
			return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required"));
		}
                
                set.setSpec(name);
		try {
			defn = json.getString("definition");
		} catch (NullPointerException npe_defn) {
			throw new JsonParseException("definition unspecified");
		}
		try {
			desc = json.getString("description");
		} catch (NullPointerException npe_desc) {
			desc = ""; //treating description as optional
		}
		set.setName(name);
		set.setDescription(desc);
		set.setDefinition(defn);
		oaiSetService.save(set);
		return created("/harvest/server/oaisets" + name, oaiSetAsJson(set));
	}
	
    }

    @PUT
    @AuthRequired
    @Path("{specname}")
    @Operation(summary = "Updates an OAI set",
            description = "Updates the definition or description of an OAI set when the authenticated user is a superuser.")
    public Response modifyOaiSet(@Context ContainerRequestContext crc,
            @RequestBody(description = "OAI set update JSON containing definition, description, or both.")
            String jsonBody,
            @Parameter(description = "OAI set specification name to update.", required = true)
            @PathParam("specname") String spec,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) throws IOException, JsonParseException {

        AuthenticatedUser dvUser;
        try {
            dvUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (!dvUser.isSuperuser()) {            
            return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.superUser.required"));
        }

        StringReader rdr = new StringReader(jsonBody);
        
        try (JsonReader jrdr = Json.createReader(rdr)) {
            JsonObject json = jrdr.readObject();
            OAISet update;
            //Validating spec 
            if (!StringUtils.isEmpty(spec)) {
                update = oaiSetService.findBySpec(spec);
                if (update == null) {
                    return badRequest(BundleUtil.getStringFromBundle("harvestserver.editSetDialog.setspec.notFound"));
                }

            } else {
                return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required"));
            }

            String desc, defn;

            try {
                defn = json.getString("definition");
            } catch (NullPointerException npe_defn) {
                defn = ""; // if they're updating description but not definition;
            }
            try {
                desc = json.getString("description");
            } catch (NullPointerException npe_desc) {
                desc = ""; //treating description as optional
            }
            if (defn.isEmpty() && desc.isEmpty()) {
                return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required"));
            }
            update.setDescription(desc);
            update.setDefinition(defn);
            oaiSetService.save(update);
            return ok("/harvest/server/oaisets" + spec, oaiSetAsJson(update));
        }
    }
    
    @DELETE
    @AuthRequired
    @Path("{specname}")
    @Operation(summary = "Deletes an OAI set",
            description = "Marks an OAI set for deletion and removes it when the authenticated user is a superuser.")
    public Response deleteOaiSet(@Context ContainerRequestContext crc,
            @Parameter(description = "OAI set specification name to delete.", required = true)
            @PathParam("specname") String spec,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) {
        
        AuthenticatedUser dvUser;
        try {
            dvUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (!dvUser.isSuperuser()) {   
            return badRequest(BundleUtil.getStringFromBundle("harvestserver.deleteSetDialog.setspec.superUser.required"));
        }
        
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
    @Operation(summary = "Lists datasets in an OAI set",
            description = "Validates that the specified OAI set exists and returns the current placeholder response for dataset listing.")
    public Response oaiSetListDatasets(
            @Parameter(description = "OAI set specification name whose datasets are requested.", required = true)
            @PathParam("specname") String spec,
            @Parameter(description = "Legacy key value accepted by the endpoint.")
            @QueryParam("key") String apiKey) throws IOException {
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
