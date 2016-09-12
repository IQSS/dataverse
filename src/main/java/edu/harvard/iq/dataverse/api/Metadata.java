/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author Leonid Andreev
 * 
 */

@Path("admin/metadata")
public class Metadata extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Metadata.class.getName());
        
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
    public Response exportAll() {
        datasetService.exportAllAsync();
        return this.accepted();
    }
    
    // reExportAll will FORCE A FULL REEXPORT on every published, local 
    // dataset, regardless of the lastexporttime value.
    @GET
    @Path("/reExportAll")
    @Produces("application/json")
    public Response reExportAll() {
        datasetService.reExportAllAsync();
        return this.accepted();
    }    
}
