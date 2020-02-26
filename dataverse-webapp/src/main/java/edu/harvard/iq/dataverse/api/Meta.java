/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;


import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.search.SearchServiceBean;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.logging.Logger;

/*
    Custom API exceptions [NOT YET IMPLEMENTED]
import edu.harvard.iq.dataverse.api.exceptions.NotFoundException;
import edu.harvard.iq.dataverse.api.exceptions.ServiceUnavailableException;
import edu.harvard.iq.dataverse.api.exceptions.PermissionDeniedException;
import edu.harvard.iq.dataverse.api.exceptions.AuthorizationRequiredException;
*/

/**
 * PLEASE NOTE that the "/api/meta" endpoints are deprecated! All code should
 * point to the newer "/api/access/datafile/..." endpoints instead.
 *
 * @author Leonid Andreev
 * <p>
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
    private VariableServiceBean dataVariableService;

    @EJB
    private PermissionServiceBean permissionService;

    @Inject
    private EmbargoAccessService embargoAccessService;

    @EJB
    DatasetDao datasetDao;


    @Deprecated
    @Path("variable/{varId}")
    @GET
    @Produces({"application/xml"})
    public String variable(@PathParam("varId") Long varId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
        String retValue = "";

        getDatasetFromDataVariable(varId)
                .filter(dt -> embargoAccessService.isRestrictedByEmbargo(dt))
                .ifPresent(dt -> { throw new ForbiddenException(); });

        ByteArrayOutputStream outStream = null;
        try {
            outStream = new ByteArrayOutputStream();

            ddiExportService.exportDataVariable(
                    varId,
                    outStream,
                    exclude,
                    include);
        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial 
            // metadata record requested. We simply return an empty string.
            return retValue;
        }

        retValue = outStream.toString();

        response.setHeader("Access-Control-Allow-Origin", "*");

        return retValue;
    }

    // Because this API is deprecated, we prefer to continue letting it operate on fileId rather adding support for persistent identifiers.
    @Deprecated
    @Path("datafile/{fileId}")
    @GET
    @Produces({"text/xml"})
    public String datafile(@PathParam("fileId") Long fileId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) throws NotFoundException, ServiceUnavailableException /*, PermissionDeniedException, AuthorizationRequiredException*/ {
        String retValue = "";

        DataFile dataFile = null;

        //httpHeaders.add("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
        //httpHeaders.add("Content-Type", "application/zip; name=\"dataverse_files.zip\"");
        response.setHeader("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");

        dataFile = datafileService.find(fileId);
        if (dataFile == null) {
            throw new NotFoundException();
        }

        if(embargoAccessService.isRestrictedByEmbargo(dataFile.getOwner())) {
            throw new ForbiddenException();
        }

        String fileName = dataFile.getFileMetadata().getLabel().replaceAll("\\.tab$", "-ddi.xml");
        response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "application/xml; name=\"" + fileName + "\"");

        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();

        try {
            ddiExportService.exportDataFile(
                    fileId,
                    outStream,
                    exclude,
                    include);

            retValue = outStream.toString();

        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial 
            // metadata record requested. 
            // We return Service Unavailable.
            throw new ServiceUnavailableException();
        }

        response.setHeader("Access-Control-Allow-Origin", "*");

        return retValue;
    }

    @Deprecated
    @Path("dataset/{datasetId}")
    @GET
    @Produces({"application/xml"})
    public String dataset(@PathParam("datasetId") Long datasetId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) throws NotFoundException /*, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {

        Dataset dataset = datasetDao.find(datasetId);
        if (dataset == null) {
            throw new NotFoundException();
        }

        if(embargoAccessService.isRestrictedByEmbargo(dataset)) {
            throw new ForbiddenException();
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

        response.setHeader("Access-Control-Allow-Origin", "*");

        return retValue;
    }

    private Optional<Dataset> getDatasetFromDataVariable(Long dataVariableId) {
        return Optional.ofNullable(dataVariableService.find(dataVariableId).getDataTable().getDataFile().getOwner());
    }
}
