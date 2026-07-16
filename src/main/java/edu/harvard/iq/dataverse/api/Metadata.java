/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;

import java.util.Date;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.Response;

import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 *
 * @author Leonid Andreev
 * 
 */

@Path("admin/metadata")
@Tag(name = "Admin", description = "Administrative Dataverse operations.")
public class Metadata extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Metadata.class.getName());

    @EJB
    OAISetServiceBean oaiSetService;

    @EJB
    DatasetServiceBean datasetService;

    // The following 2 commands start export all jobs in the background, 
    // asynchronously. 
    // (These API calls should probably not be here;
    // May be under "/admin" somewhere?)
    // exportAll will attempt to go through all the published, local 
    // datasets *that haven't been exported yet* - which is determined by
    // checking the lastexporttime value of the dataset; if it's null, or < the last 
    // publication date = "unexported" - and export them. 
    @GET
    @Path("/exportAll")
    @Produces("application/json")
    @Operation(summary = "Starts metadata export jobs",
            description = "Starts background exports for published local datasets that have not been exported since their last publication.")
    public Response exportAll() {
        datasetService.exportAllAsync();
        return this.accepted();
    }
    
    // reExportAll will FORCE A FULL REEXPORT on every published, local 
    // dataset, regardless of the lastexporttime value.
    @GET
    @Path("/reExportAll")
    @Produces("application/json")
    @Operation(summary = "Starts metadata re-export jobs",
            description = "Starts background re-export jobs for published local datasets, optionally limited to datasets older than a supplied date.")
    public Response reExportAll(
            @Parameter(description = "Optional cutoff date in YYYY-MM-DD format for selecting datasets to re-export.")
            @QueryParam(value = "olderThan") String olderThan) {
        Date reExportDate = null;
        if (olderThan != null && !olderThan.isEmpty()) {
            try {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                dateFormat.setLenient(false);
                reExportDate = dateFormat.parse(olderThan);
            } catch (java.text.ParseException e) {
                return error(Response.Status.BAD_REQUEST, "Invalid date format for olderThan parameter. Expected format: YYYY-MM-DD");
            }
        }
        datasetService.reExportAllAsync(reExportDate);
        return this.accepted();
    }

    @GET
    @Path("{id}/reExportDataset")
    @Operation(summary = "Starts a dataset metadata re-export",
            description = "Starts a background metadata re-export for the specified dataset.")
    public Response indexDatasetByPersistentId(
            @Parameter(description = "Dataset id or persistent identifier to re-export.", required = true)
            @PathParam("id") String id) {
        try {
            Dataset dataset = findDatasetOrDie(id);
            datasetService.reExportDatasetAsync(dataset);
            return ok("export started");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @Path("clearExportTimestamps")
    @Operation(summary = "Clears metadata export timestamps",
            description = "Clears stored metadata export timestamps for all datasets without deleting cached metadata export files.")
    public Response clearExportTimestamps() {
        // only clear the timestamp in the database, cached metadata export files are not deleted
        int numItemsCleared = datasetService.clearAllExportTimes();
        return ok("cleared: " + numItemsCleared);
    }

    /**
     * initial attempt at triggering indexing/creation/population of a OAI set without going throught
     * the UI.
     */
    @PUT
    @Path("/exportOAI/{specname}")
    @Operation(summary = "Starts an OAI set export",
            description = "Marks the specified OAI set as updating and starts its metadata export in the background.")
    public Response exportOaiSet(
            @Parameter(description = "OAI set specification name to export.", required = true)
            @PathParam("specname") String spec)
    {
	    // assuming this belongs here (because it's a metadata export), but open to moving it elsewhere
	    OAISet set = null;
	    try
	    {
		    set = oaiSetService.findBySpec(spec);
	    }
	    catch(Exception ex)
	    {
		    return error(Response.Status.BAD_REQUEST,"bad request / invalid OAI set");
	    }
	    if ( null == set )
	    {
		    return error(Response.Status.NOT_FOUND, "unable to find specified OAI set");
	    }
	    try
	    {
		    oaiSetService.setUpdateInProgress( set.getId() );
		    oaiSetService.exportOaiSetAsync(set);
		    return ok("export started");
	    }
	    catch( Exception ex )
	    {
		    return error(Response.Status.BAD_REQUEST, "problem exporting OAI set");
	    }
    }
}
