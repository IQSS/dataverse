/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;


import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;

import java.util.logging.Logger;
import jakarta.ejb.EJB;
import java.io.ByteArrayOutputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;

/*
    Custom API exceptions [NOT YET IMPLEMENTED]
import edu.harvard.iq.dataverse.api.exceptions.NotFoundException;
import edu.harvard.iq.dataverse.api.exceptions.ServiceUnavailableException;
import edu.harvard.iq.dataverse.api.exceptions.PermissionDeniedException;
import edu.harvard.iq.dataverse.api.exceptions.AuthorizationRequiredException;
*/

/**
 *
 * PLEASE NOTE that the "/api/meta" endpoints are deprecated! All code should
 * point to the newer "/api/access/datafile/..." endpoints instead.
 *
 * @author Leonid Andreev
 * 
 * The metadata access API is based on the DVN metadata API v.1.0 (that came 
 * with the v.3.* of the DVN app) and extended for DVN 4.0 to include more
 * granular access to subsets of the metadata that describe the dataset:
 * access to individual datafile and datavariable sections, as well as  
 * specific fragments of these sections. 
 */

@Deprecated
@Path("meta")
public class Meta {
    private static final Logger logger = Logger.getLogger(Meta.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    
    @EJB
    DDIExportServiceBean ddiExportService;
    
    @EJB
    DataFileServiceBean datafileService; 
    
    @EJB
    DatasetServiceBean datasetService;

    // Because this API is deprecated, we prefer to continue letting it operate on fileId rather adding support for persistent identifiers.
    @Deprecated
    @Path("datafile/{fileId}")
    @GET
    @Produces({"text/xml"})
    public String datafile(@PathParam("fileId") Long fileId, @QueryParam("fileMetadataId") Long fileMetadataId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) throws NotFoundException, ServiceUnavailableException /*, PermissionDeniedException, AuthorizationRequiredException*/ {
        String retValue = "";

        DataFile dataFile = null; 
        
        //httpHeaders.add("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
        //httpHeaders.add("Content-Type", "application/zip; name=\"dataverse_files.zip\"");
        response.setHeader("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
        
        dataFile = datafileService.find(fileId);
        if (dataFile == null) {
            throw new NotFoundException();
        }
        
        String fileName = dataFile.getFileMetadata().getLabel().replaceAll("\\.tab$", "-ddi.xml");
        response.setHeader("Content-disposition", "attachment; filename=\""+fileName+"\"");
        response.setHeader("Content-Type", "application/xml; name=\""+fileName+"\"");
        
        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();

        try {
            ddiExportService.exportDataFile(
                    fileId,
                    outStream,
                    exclude,
                    include,
                    fileMetadataId);

            retValue = outStream.toString();

        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial 
            // metadata record requested. 
            // We return Service Unavailable.
            throw new ServiceUnavailableException();
        }

        return retValue;
    }
    
    @Deprecated
    @Path("dataset/{datasetId}")
    @GET
    @Produces({"application/xml"})
    public String dataset(@PathParam("datasetId") Long datasetId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) throws NotFoundException /*, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
 
        Dataset dataset = datasetService.find(datasetId);
        if (dataset == null) {
            throw new NotFoundException();
        }
        
        String retValue = "";

        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();

        try {
            ddiExportService.exportDataset(
                    datasetId,
                    outStream,
                    exclude,
                    include);

            retValue = outStream.toString();

        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial 
            // metadata record requested. We simply return an empty string.
            throw new ServiceUnavailableException();
        }

        return retValue;
    }
    
}
