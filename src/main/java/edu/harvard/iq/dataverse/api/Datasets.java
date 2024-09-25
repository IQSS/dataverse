package edu.harvard.iq.dataverse.api;

import com.amazonaws.services.s3.model.PartETag;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.dataaccess.*;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UnforcedCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.globus.GlobusUtil;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.makedatacount.*;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean.MakeDataCountEntry;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.storageuse.UploadSessionQuotaLimit;
import edu.harvard.iq.dataverse.util.*;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.*;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.api.ApiConstants.*;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());
    private static final Pattern dataFilePattern = Pattern.compile("^[0-9a-f]{11}-[0-9a-f]{12}\\.?.*");
    
    @Inject DataverseSession session;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseServiceBean dataverseService;
    
    @EJB
    GlobusServiceBean globusService;

    @EJB
    UserNotificationServiceBean userNotificationService;
    
    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    AuthenticationServiceBean authenticationServiceBean;
    
    @EJB
    DDIExportServiceBean ddiExportService;

    @EJB
    MetadataBlockServiceBean metadataBlockService;
    
    @EJB
    DataFileServiceBean fileService;

    @EJB
    IngestServiceBean ingestService;

    @EJB
    EjbDataverseEngine commandEngine;
    
    @EJB
    IndexServiceBean indexService;

    @EJB
    S3PackageImporter s3PackageImporter;
     
    @EJB
    SettingsServiceBean settingsService;

    // TODO: Move to AbstractApiBean
    @EJB
    DatasetMetricsServiceBean datasetMetricsSvc;
    
    @EJB
    DatasetExternalCitationsServiceBean datasetExternalCitationsService;

    @EJB
    EmbargoServiceBean embargoService;

    @EJB
    RetentionServiceBean retentionService;

    @Inject
    MakeDataCountLoggingServiceBean mdcLogService;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;

    @Inject
    WorkflowServiceBean wfService;
    
    @Inject
    DataverseRoleServiceBean dataverseRoleService;

    @EJB
    DatasetVersionServiceBean datasetversionService;

    @Inject
    PrivateUrlServiceBean privateUrlService;

    @Inject
    DatasetVersionFilesServiceBean datasetVersionFilesServiceBean;

    @Inject
    DatasetTypeServiceBean datasetTypeSvc;

    /**
     * Used to consolidate the way we parse and handle dataset versions.
     * @param <T> 
     */
    public interface DsVersionHandler<T> {
        T handleLatest();
        T handleDraft();
        T handleSpecific( long major, long minor );
        T handleLatestPublished();
    }
    
    @GET
    @AuthRequired
    @Path("{id}")
    public Response getDataset(@Context ContainerRequestContext crc, @PathParam("id") String id, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response,  @QueryParam("returnOwners") boolean returnOwners) {
        return response( req -> {
            final Dataset retrieved = execCommand(new GetDatasetCommand(req, findDatasetOrDie(id, true)));
            final DatasetVersion latest = execCommand(new GetLatestAccessibleDatasetVersionCommand(req, retrieved));
            final JsonObjectBuilder jsonbuilder = json(retrieved, returnOwners);
            //Report MDC if this is a released version (could be draft if user has access, or user may not have access at all and is not getting metadata beyond the minimum)
            if((latest != null) && latest.isReleased()) {
                MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountEntry(uriInfo, headers, dvRequestService, retrieved);
                mdcLogService.logEntry(entry);
            }
            return ok(jsonbuilder.add("latestVersion", (latest != null) ? json(latest, true) : null));
        }, getRequestUser(crc));
    }
    
    // This API call should, ideally, call findUserOrDie() and the GetDatasetCommand 
    // to obtain the dataset that we are trying to export - which would handle
    // Auth in the process... For now, Auth isn't necessary - since export ONLY 
    // WORKS on published datasets, which are open to the world. -- L.A. 4.5
    @GET
    @Path("/export")
    @Produces({"application/xml", "application/json", "application/html", "application/ld+json", "*/*" })
    public Response exportDataset(@QueryParam("persistentId") String persistentId, @QueryParam("exporter") String exporter, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) {

        try {
            Dataset dataset = datasetService.findByGlobalId(persistentId);
            if (dataset == null) {
                return error(Response.Status.NOT_FOUND, "A dataset with the persistentId " + persistentId + " could not be found.");
            }
            
            ExportService instance = ExportService.getInstance();
            
            InputStream is = instance.getExport(dataset, exporter);
           
            String mediaType = instance.getMediaType(exporter);
            //Export is only possible for released (non-draft) dataset versions so we can log without checking to see if this is a request for a draft 
            MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountEntry(uriInfo, headers, dvRequestService, dataset);
            mdcLogService.logEntry(entry);
            
            return Response.ok()
                    .entity(is)
                    .type(mediaType).
                    build();
        } catch (Exception wr) {
            logger.warning(wr.getMessage());
            return error(Response.Status.FORBIDDEN, "Export Failed");
        }
    }

    @DELETE
    @AuthRequired
    @Path("{id}")
    public Response deleteDataset(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        // Internally, "DeleteDatasetCommand" simply redirects to "DeleteDatasetVersionCommand"
        // (and there's a comment that says "TODO: remove this command")
        // do we need an exposed API call for it? 
        // And DeleteDatasetVersionCommand further redirects to DestroyDatasetCommand, 
        // if the dataset only has 1 version... In other words, the functionality 
        // currently provided by this API is covered between the "deleteDraftVersion" and
        // "destroyDataset" API calls.  
        // (The logic below follows the current implementation of the underlying 
        // commands!)

        User u = getRequestUser(crc);
        return response( req -> {
            Dataset doomed = findDatasetOrDie(id);
            DatasetVersion doomedVersion = doomed.getLatestVersion();
            boolean destroy = false;
            
            if (doomed.getVersions().size() == 1) {
                if (doomed.isReleased() && (!(u instanceof AuthenticatedUser) || !u.isSuperuser())) {
                    throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "Only superusers can delete published datasets"));
                }
                destroy = true;
            } else {
                if (!doomedVersion.isDraft()) {
                    throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "This is a published dataset with multiple versions. This API can only delete the latest version if it is a DRAFT"));
                }
            }
            
            // Gather the locations of the physical files that will need to be 
            // deleted once the destroy command execution has been finalized:
            Map<Long, String> deleteStorageLocations = fileService.getPhysicalFilesToDelete(doomedVersion, destroy);
            
            execCommand( new DeleteDatasetCommand(req, findDatasetOrDie(id)));
            
            // If we have gotten this far, the destroy command has succeeded, 
            // so we can finalize it by permanently deleting the physical files:
            // (DataFileService will double-check that the datafiles no 
            // longer exist in the database, before attempting to delete 
            // the physical files)
            if (!deleteStorageLocations.isEmpty()) {
                fileService.finalizeFileDeletes(deleteStorageLocations);
            }
            
            return ok("Dataset " + id + " deleted");
        }, u);
    }
        
    @DELETE
    @AuthRequired
    @Path("{id}/destroy")
    public Response destroyDataset(@Context ContainerRequestContext crc, @PathParam("id") String id) {

        User u = getRequestUser(crc);
        return response(req -> {
            // first check if dataset is released, and if so, if user is a superuser
            Dataset doomed = findDatasetOrDie(id);

            if (doomed.isReleased() && (!(u instanceof AuthenticatedUser) || !u.isSuperuser())) {
                throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "Destroy can only be called by superusers."));
            }

            // Gather the locations of the physical files that will need to be 
            // deleted once the destroy command execution has been finalized:
            Map<Long, String> deleteStorageLocations = fileService.getPhysicalFilesToDelete(doomed);

            execCommand(new DestroyDatasetCommand(doomed, req));

            // If we have gotten this far, the destroy command has succeeded, 
            // so we can finalize permanently deleting the physical files:
            // (DataFileService will double-check that the datafiles no 
            // longer exist in the database, before attempting to delete 
            // the physical files)
            if (!deleteStorageLocations.isEmpty()) {
                fileService.finalizeFileDeletes(deleteStorageLocations);
            }

            return ok("Dataset " + id + " destroyed");
        }, u);
    }
    
    @DELETE
    @AuthRequired
    @Path("{id}/versions/{versionId}")
    public Response deleteDraftVersion(@Context ContainerRequestContext crc, @PathParam("id") String id,  @PathParam("versionId") String versionId ){
        if (!DS_VERSION_DRAFT.equals(versionId)) {
            return badRequest("Only the " + DS_VERSION_DRAFT + " version can be deleted");
        }

        return response( req -> {
            Dataset dataset = findDatasetOrDie(id);
            DatasetVersion doomed = dataset.getLatestVersion();
            
            if (!doomed.isDraft()) {
                throw new WrappedResponse(error(Response.Status.UNAUTHORIZED, "This is NOT a DRAFT version"));
            }
            
            // Gather the locations of the physical files that will need to be 
            // deleted once the destroy command execution has been finalized:
            
            Map<Long, String> deleteStorageLocations = fileService.getPhysicalFilesToDelete(doomed);
            
            execCommand( new DeleteDatasetVersionCommand(req, dataset));
            
            // If we have gotten this far, the delete command has succeeded - 
            // by either deleting the Draft version of a published dataset, 
            // or destroying an unpublished one. 
            // This means we can finalize permanently deleting the physical files:
            // (DataFileService will double-check that the datafiles no 
            // longer exist in the database, before attempting to delete 
            // the physical files)
            if (!deleteStorageLocations.isEmpty()) {
                fileService.finalizeFileDeletes(deleteStorageLocations);
            }
            
            return ok("Draft version of dataset " + id + " deleted");
        }, getRequestUser(crc));
    }
        
    @DELETE
    @AuthRequired
    @Path("{datasetId}/deleteLink/{linkedDataverseId}")
    public Response deleteDatasetLinkingDataverse(@Context ContainerRequestContext crc, @PathParam("datasetId") String datasetId, @PathParam("linkedDataverseId") String linkedDataverseId) {
                boolean index = true;
        return response(req -> {
            execCommand(new DeleteDatasetLinkingDataverseCommand(req, findDatasetOrDie(datasetId), findDatasetLinkingDataverseOrDie(datasetId, linkedDataverseId), index));
            return ok("Link from Dataset " + datasetId + " to linked Dataverse " + linkedDataverseId + " deleted");
        }, getRequestUser(crc));
    }
        
    @PUT
    @AuthRequired
    @Path("{id}/citationdate")
    public Response setCitationDate(@Context ContainerRequestContext crc, @PathParam("id") String id, String dsfTypeName) {
        return response( req -> {
            if ( dsfTypeName.trim().isEmpty() ){
                return badRequest("Please provide a dataset field type in the requst body.");
            }
            DatasetFieldType dsfType = null;
            if (!":publicationDate".equals(dsfTypeName)) {
                dsfType = datasetFieldSvc.findByName(dsfTypeName);
                if (dsfType == null) {
                    return badRequest("Dataset Field Type Name " + dsfTypeName + " not found.");
                }
            }

            execCommand(new SetDatasetCitationDateCommand(req, findDatasetOrDie(id), dsfType));
            return ok("Citation Date for dataset " + id + " set to: " + (dsfType != null ? dsfType.getDisplayName() : "default"));
        }, getRequestUser(crc));
    }
    
    @DELETE
    @AuthRequired
    @Path("{id}/citationdate")
    public Response useDefaultCitationDate(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        return response( req -> {
            execCommand(new SetDatasetCitationDateCommand(req, findDatasetOrDie(id), null));
            return ok("Citation Date for dataset " + id + " set to default");
        }, getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{id}/versions")
    public Response listVersions(@Context ContainerRequestContext crc, @PathParam("id") String id, @QueryParam("excludeFiles") Boolean excludeFiles, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset) {

        return response( req -> {
            Dataset dataset = findDatasetOrDie(id);
            Boolean deepLookup = excludeFiles == null ? true : !excludeFiles;

            return ok( execCommand( new ListVersionsCommand(req, dataset, offset, limit, deepLookup) )
                                .stream()
                                .map( d -> json(d, deepLookup) )
                                .collect(toJsonArray()));
        }, getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}")
    public Response getVersion(@Context ContainerRequestContext crc,
                               @PathParam("id") String datasetId,
                               @PathParam("versionId") String versionId,
                               @QueryParam("excludeFiles") Boolean excludeFiles,
                               @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                               @QueryParam("returnOwners") boolean returnOwners,
                               @Context UriInfo uriInfo,
                               @Context HttpHeaders headers) {
        return response( req -> {
            
            //If excludeFiles is null the default is to provide the files and because of this we need to check permissions. 
            boolean checkPerms = excludeFiles == null ? true : !excludeFiles;
            
            Dataset dataset = findDatasetOrDie(datasetId);
            DatasetVersion requestedDatasetVersion = getDatasetVersionOrDie(req, 
                                                                            versionId, 
                                                                            dataset, 
                                                                            uriInfo, 
                                                                            headers, 
                                                                            includeDeaccessioned,
                                                                            checkPerms);

            if (requestedDatasetVersion == null || requestedDatasetVersion.getId() == null) {
                return notFound("Dataset version not found");
            }

            if (excludeFiles == null ? true : !excludeFiles) {
                requestedDatasetVersion = datasetversionService.findDeep(requestedDatasetVersion.getId());
            }

            JsonObjectBuilder jsonBuilder = json(requestedDatasetVersion,
                                                 null, 
                                                 excludeFiles == null ? true : !excludeFiles, 
                                                 returnOwners);
            return ok(jsonBuilder);

        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/files")
    public Response getVersionFiles(@Context ContainerRequestContext crc,
                                    @PathParam("id") String datasetId,
                                    @PathParam("versionId") String versionId,
                                    @QueryParam("limit") Integer limit,
                                    @QueryParam("offset") Integer offset,
                                    @QueryParam("contentType") String contentType,
                                    @QueryParam("accessStatus") String accessStatus,
                                    @QueryParam("categoryName") String categoryName,
                                    @QueryParam("tabularTagName") String tabularTagName,
                                    @QueryParam("searchText") String searchText,
                                    @QueryParam("orderCriteria") String orderCriteria,
                                    @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers) {
        return response(req -> {
            DatasetVersion datasetVersion = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId, false), uriInfo, headers, includeDeaccessioned);
            DatasetVersionFilesServiceBean.FileOrderCriteria fileOrderCriteria;
            try {
                fileOrderCriteria = orderCriteria != null ? DatasetVersionFilesServiceBean.FileOrderCriteria.valueOf(orderCriteria) : DatasetVersionFilesServiceBean.FileOrderCriteria.NameAZ;
            } catch (IllegalArgumentException e) {
                return badRequest(BundleUtil.getStringFromBundle("datasets.api.version.files.invalid.order.criteria", List.of(orderCriteria)));
            }
            FileSearchCriteria fileSearchCriteria;
            try {
                fileSearchCriteria = new FileSearchCriteria(
                        contentType,
                        accessStatus != null ? FileSearchCriteria.FileAccessStatus.valueOf(accessStatus) : null,
                        categoryName,
                        tabularTagName,
                        searchText
                );
            } catch (IllegalArgumentException e) {
                return badRequest(BundleUtil.getStringFromBundle("datasets.api.version.files.invalid.access.status", List.of(accessStatus)));
            }
            return ok(jsonFileMetadatas(datasetVersionFilesServiceBean.getFileMetadatas(datasetVersion, limit, offset, fileSearchCriteria, fileOrderCriteria)),
                    datasetVersionFilesServiceBean.getFileMetadataCount(datasetVersion, fileSearchCriteria));
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/files/counts")
    public Response getVersionFileCounts(@Context ContainerRequestContext crc,
                                         @PathParam("id") String datasetId,
                                         @PathParam("versionId") String versionId,
                                         @QueryParam("contentType") String contentType,
                                         @QueryParam("accessStatus") String accessStatus,
                                         @QueryParam("categoryName") String categoryName,
                                         @QueryParam("tabularTagName") String tabularTagName,
                                         @QueryParam("searchText") String searchText,
                                         @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                         @Context UriInfo uriInfo,
                                         @Context HttpHeaders headers) {
        return response(req -> {
            FileSearchCriteria fileSearchCriteria;
            try {
                fileSearchCriteria = new FileSearchCriteria(
                        contentType,
                        accessStatus != null ? FileSearchCriteria.FileAccessStatus.valueOf(accessStatus) : null,
                        categoryName,
                        tabularTagName,
                        searchText
                );
            } catch (IllegalArgumentException e) {
                return badRequest(BundleUtil.getStringFromBundle("datasets.api.version.files.invalid.access.status", List.of(accessStatus)));
            }
            DatasetVersion datasetVersion = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers, includeDeaccessioned);
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("total", datasetVersionFilesServiceBean.getFileMetadataCount(datasetVersion, fileSearchCriteria));
            jsonObjectBuilder.add("perContentType", json(datasetVersionFilesServiceBean.getFileMetadataCountPerContentType(datasetVersion, fileSearchCriteria)));
            jsonObjectBuilder.add("perCategoryName", json(datasetVersionFilesServiceBean.getFileMetadataCountPerCategoryName(datasetVersion, fileSearchCriteria)));
            jsonObjectBuilder.add("perTabularTagName", jsonFileCountPerTabularTagNameMap(datasetVersionFilesServiceBean.getFileMetadataCountPerTabularTagName(datasetVersion, fileSearchCriteria)));
            jsonObjectBuilder.add("perAccessStatus", jsonFileCountPerAccessStatusMap(datasetVersionFilesServiceBean.getFileMetadataCountPerAccessStatus(datasetVersion, fileSearchCriteria)));
            return ok(jsonObjectBuilder);
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/dirindex")
    @Produces("text/html")
    public Response getFileAccessFolderView(@Context ContainerRequestContext crc, @PathParam("id") String datasetId, @QueryParam("version") String versionId, @QueryParam("folder") String folderName, @QueryParam("original") Boolean originals, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) {

        folderName = folderName == null ? "" : folderName;
        versionId = versionId == null ? DS_VERSION_LATEST_PUBLISHED : versionId;
        
        DatasetVersion version;
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            version = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        
        String output = FileUtil.formatFolderListingHtml(folderName, version, "", originals != null && originals);
        
        // return "NOT FOUND" if there is no such folder in the dataset version:
        
        if ("".equals(output)) {
            return notFound("Folder " + folderName + " does not exist");
        }
        
        
        String indexFileName = folderName.equals("") ? ".index.html"
                : ".index-" + folderName.replace('/', '_') + ".html";
        response.setHeader("Content-disposition", "filename=\"" + indexFileName + "\"");

        
        return Response.ok()
                .entity(output)
                //.type("application/html").
                .build();
    }
    
    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/metadata")
    public Response getVersionMetadata(@Context ContainerRequestContext crc, @PathParam("id") String datasetId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return response( req -> ok(
                    jsonByBlocks(
                        getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers )
                                .getDatasetFields())), getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{id}/versions/{versionNumber}/metadata/{block}")
    public Response getVersionMetadataBlock(@Context ContainerRequestContext crc,
                                            @PathParam("id") String datasetId,
                                            @PathParam("versionNumber") String versionNumber,
                                            @PathParam("block") String blockName,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        
        return response( req -> {
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionNumber, findDatasetOrDie(datasetId), uriInfo, headers );
            
            Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(dsv.getDatasetFields());
            for ( Map.Entry<MetadataBlock, List<DatasetField>> p : fieldsByBlock.entrySet() ) {
                if ( p.getKey().getName().equals(blockName) ) {
                    return ok(json(p.getKey(), p.getValue()));
                }
            }
            return notFound("metadata block named " + blockName + " not found");
        }, getRequestUser(crc));
    }

    /**
     * Add Signposting
     * @param datasetId
     * @param versionId
     * @param uriInfo
     * @param headers
     * @return
     */
    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/linkset")
    public Response getLinkset(@Context ContainerRequestContext crc, @PathParam("id") String datasetId, @PathParam("versionId") String versionId, 
           @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        if (DS_VERSION_DRAFT.equals(versionId)) {
            return badRequest("Signposting is not supported on the " + DS_VERSION_DRAFT + " version");
        }
        DataverseRequest req = createDataverseRequest(getRequestUser(crc));
        try {
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers);
            return Response
                    .ok(Json.createObjectBuilder()
                            .add("linkset",
                                    new SignpostingResources(systemConfig, dsv,
                                            JvmSettings.SIGNPOSTING_LEVEL1_AUTHOR_LIMIT.lookupOptional().orElse(""),
                                            JvmSettings.SIGNPOSTING_LEVEL1_ITEM_LIMIT.lookupOptional().orElse(""))
                                                    .getJsonLinkset())
                            .build())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/modifyRegistration")
    public Response updateDatasetTargetURL(@Context ContainerRequestContext crc, @PathParam("id") String id ) {
        return response( req -> {
            execCommand(new UpdateDatasetTargetURLCommand(findDatasetOrDie(id), req));
            return ok("Dataset " + id + " target url updated");
        }, getRequestUser(crc));
    }
    
    @POST
    @AuthRequired
    @Path("/modifyRegistrationAll")
    public Response updateDatasetTargetURLAll(@Context ContainerRequestContext crc) {
        return response( req -> {
            datasetService.findAll().forEach( ds -> {
                try {
                    execCommand(new UpdateDatasetTargetURLCommand(findDatasetOrDie(ds.getId().toString()), req));
                } catch (WrappedResponse ex) {
                    Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            return ok("Update All Dataset target url completed");
        }, getRequestUser(crc));
    }
    
    @POST
    @AuthRequired
    @Path("{id}/modifyRegistrationMetadata")
    public Response updateDatasetPIDMetadata(@Context ContainerRequestContext crc, @PathParam("id") String id) {

        try {
            Dataset dataset = findDatasetOrDie(id);
            if (!dataset.isReleased()) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.failure.dataset.must.be.released"));
            }
        } catch (WrappedResponse ex) {
            Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
        }

        return response(req -> {
            Dataset dataset = findDatasetOrDie(id);
            execCommand(new UpdateDvObjectPIDMetadataCommand(dataset, req));
            List<String> args = Arrays.asList(dataset.getIdentifier());
            return ok(BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.success.for.single.dataset", args));
        }, getRequestUser(crc));
    }
    
    @POST
    @AuthRequired
    @Path("/modifyRegistrationPIDMetadataAll")
    public Response updateDatasetPIDMetadataAll(@Context ContainerRequestContext crc) {
        return response( req -> {
            datasetService.findAll().forEach( ds -> {
                try {
                    logger.fine("ReRegistering: " + ds.getId() + " : " + ds.getIdentifier());
                    if (!ds.isReleased() || (!ds.isIdentifierRegistered() || (ds.getIdentifier() == null))) {
                        if (ds.isReleased()) {
                            logger.warning("Dataset id=" + ds.getId() + " is in an inconsistent state (publicationdate but no identifier/identifier not registered");
                        }
                    } else {
                    execCommand(new UpdateDvObjectPIDMetadataCommand(findDatasetOrDie(ds.getId().toString()), req));
                    }
                } catch (WrappedResponse ex) {
                    Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            return ok(BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.success.for.update.all"));
        }, getRequestUser(crc));
    }
  
    @PUT
    @AuthRequired
    @Path("{id}/versions/{versionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDraftVersion(@Context ContainerRequestContext crc, String jsonBody, @PathParam("id") String id, @PathParam("versionId") String versionId) {
        if (!DS_VERSION_DRAFT.equals(versionId)) {
            return error( Response.Status.BAD_REQUEST, "Only the " + DS_VERSION_DRAFT + " version can be updated");
        }
        
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            Dataset ds = findDatasetOrDie(id);
            JsonObject json = JsonUtil.getJsonObject(jsonBody);
            DatasetVersion incomingVersion = jsonParser().parseDatasetVersion(json);
            
            // clear possibly stale fields from the incoming dataset version.
            // creation and modification dates are updated by the commands.
            incomingVersion.setId(null);
            incomingVersion.setVersionNumber(null);
            incomingVersion.setMinorVersionNumber(null);
            incomingVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
            incomingVersion.setDataset(ds);
            incomingVersion.setCreateTime(null);
            incomingVersion.setLastUpdateTime(null);
            
            if (!incomingVersion.getFileMetadatas().isEmpty()){
                return error( Response.Status.BAD_REQUEST, "You may not add files via this api.");
            }
            
            boolean updateDraft = ds.getLatestVersion().isDraft();
            
            DatasetVersion managedVersion;
            if (updateDraft) {
                final DatasetVersion editVersion = ds.getOrCreateEditVersion();
                editVersion.setDatasetFields(incomingVersion.getDatasetFields());
                editVersion.setTermsOfUseAndAccess(incomingVersion.getTermsOfUseAndAccess());
                editVersion.getTermsOfUseAndAccess().setDatasetVersion(editVersion);
                boolean hasValidTerms = TermsOfUseAndAccessValidator.isTOUAValid(editVersion.getTermsOfUseAndAccess(), null);
                if (!hasValidTerms) {
                    return error(Status.CONFLICT, BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
                }
                Dataset managedDataset = execCommand(new UpdateDatasetVersionCommand(ds, req));
                managedVersion = managedDataset.getOrCreateEditVersion();
            } else {
                boolean hasValidTerms = TermsOfUseAndAccessValidator.isTOUAValid(incomingVersion.getTermsOfUseAndAccess(), null);
                if (!hasValidTerms) {
                    return error(Status.CONFLICT, BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
                }
                managedVersion = execCommand(new CreateDatasetVersionCommand(req, ds, incomingVersion));
            }
            return ok( json(managedVersion, true) );
                    
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset version Json: " + ex.getMessage(), ex);
            return error( Response.Status.BAD_REQUEST, "Error parsing dataset version: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
    }

    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/metadata")
    @Produces("application/ld+json, application/json-ld")
    public Response getVersionJsonLDMetadata(@Context ContainerRequestContext crc, @PathParam("id") String id, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(id), uriInfo, headers);
            OREMap ore = new OREMap(dsv,
                    settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport, false));
            return ok(ore.getOREMapBuilder(true));

        } catch (WrappedResponse ex) {
            ex.printStackTrace();
            return ex.getResponse();
        } catch (Exception jpe) {
            logger.log(Level.SEVERE, "Error getting jsonld metadata for dsv: ", jpe.getLocalizedMessage());
            jpe.printStackTrace();
            return error(Response.Status.INTERNAL_SERVER_ERROR, jpe.getLocalizedMessage());
        }
    }

    @GET
    @AuthRequired
    @Path("{id}/metadata")
    @Produces("application/ld+json, application/json-ld")
    public Response getJsonLDMetadata(@Context ContainerRequestContext crc, @PathParam("id") String id, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return getVersionJsonLDMetadata(crc, id, DS_VERSION_LATEST, uriInfo, headers);
    }

    @PUT
    @AuthRequired
    @Path("{id}/metadata")
    @Consumes("application/ld+json, application/json-ld")
    public Response updateVersionMetadata(@Context ContainerRequestContext crc, String jsonLDBody, @PathParam("id") String id, @DefaultValue("false") @QueryParam("replace") boolean replaceTerms) {

        try {
            Dataset ds = findDatasetOrDie(id);
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            //Get draft state as of now

            boolean updateDraft = ds.getLatestVersion().isDraft();
            //Get the current draft or create a new version to update
            DatasetVersion dsv = ds.getOrCreateEditVersion();
            dsv = JSONLDUtil.updateDatasetVersionMDFromJsonLD(dsv, jsonLDBody, metadataBlockService, datasetFieldSvc, !replaceTerms, false, licenseSvc);
            dsv.getTermsOfUseAndAccess().setDatasetVersion(dsv);
            boolean hasValidTerms = TermsOfUseAndAccessValidator.isTOUAValid(dsv.getTermsOfUseAndAccess(), null);
            if (!hasValidTerms) {
                return error(Status.CONFLICT, BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
            }
            DatasetVersion managedVersion;
            Dataset managedDataset = execCommand(new UpdateDatasetVersionCommand(ds, req));
            managedVersion = managedDataset.getLatestVersion();
            String info = updateDraft ? "Version Updated" : "Version Created";
            return ok(Json.createObjectBuilder().add(info, managedVersion.getVersionDate()));

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}", jsonLDBody);
            return error(Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage());
        }
    }

    @PUT
    @AuthRequired
    @Path("{id}/metadata/delete")
    @Consumes("application/ld+json, application/json-ld")
    public Response deleteMetadata(@Context ContainerRequestContext crc, String jsonLDBody, @PathParam("id") String id) {
        try {
            Dataset ds = findDatasetOrDie(id);
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            //Get draft state as of now

            boolean updateDraft = ds.getLatestVersion().isDraft();
            //Get the current draft or create a new version to update
            DatasetVersion dsv = ds.getOrCreateEditVersion();
            dsv = JSONLDUtil.deleteDatasetVersionMDFromJsonLD(dsv, jsonLDBody, metadataBlockService, licenseSvc);
            dsv.getTermsOfUseAndAccess().setDatasetVersion(dsv);
            DatasetVersion managedVersion;
            Dataset managedDataset = execCommand(new UpdateDatasetVersionCommand(ds, req));
            managedVersion = managedDataset.getLatestVersion();
            String info = updateDraft ? "Version Updated" : "Version Created";
            return ok(Json.createObjectBuilder().add(info, managedVersion.getVersionDate()));

        } catch (WrappedResponse ex) {
            ex.printStackTrace();
            return ex.getResponse();
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}", jsonLDBody);
            jpe.printStackTrace();
            return error(Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage());
        }
    }

    @PUT
    @AuthRequired
    @Path("{id}/deleteMetadata")
    public Response deleteVersionMetadata(@Context ContainerRequestContext crc, String jsonBody, @PathParam("id") String id) throws WrappedResponse {

        DataverseRequest req = createDataverseRequest(getRequestUser(crc));

        return processDatasetFieldDataDelete(jsonBody, id, req);
    }

    private Response processDatasetFieldDataDelete(String jsonBody, String id, DataverseRequest req) {
        try {

            Dataset ds = findDatasetOrDie(id);
            JsonObject json = JsonUtil.getJsonObject(jsonBody);
            //Get the current draft or create a new version to update
            DatasetVersion dsv = ds.getOrCreateEditVersion();
            dsv.getTermsOfUseAndAccess().setDatasetVersion(dsv);
            List<DatasetField> fields = new LinkedList<>();
            DatasetField singleField = null;

            JsonArray fieldsJson = json.getJsonArray("fields");
            if (fieldsJson == null) {
                singleField = jsonParser().parseField(json, Boolean.FALSE);
                fields.add(singleField);
            } else {
                fields = jsonParser().parseMultipleFields(json);
            }

            dsv.setVersionState(DatasetVersion.VersionState.DRAFT);

            List<ControlledVocabularyValue> controlledVocabularyItemsToRemove = new ArrayList<ControlledVocabularyValue>();
            List<DatasetFieldValue> datasetFieldValueItemsToRemove = new ArrayList<DatasetFieldValue>();
            List<DatasetFieldCompoundValue> datasetFieldCompoundValueItemsToRemove = new ArrayList<DatasetFieldCompoundValue>();

            for (DatasetField updateField : fields) {
                boolean found = false;
                for (DatasetField dsf : dsv.getDatasetFields()) {
                    if (dsf.getDatasetFieldType().equals(updateField.getDatasetFieldType())) {
                        if (dsf.getDatasetFieldType().isAllowMultiples()) {
                            if (updateField.getDatasetFieldType().isControlledVocabulary()) {
                                if (dsf.getDatasetFieldType().isAllowMultiples()) {
                                    for (ControlledVocabularyValue cvv : updateField.getControlledVocabularyValues()) {
                                        for (ControlledVocabularyValue existing : dsf.getControlledVocabularyValues()) {
                                            if (existing.getStrValue().equals(cvv.getStrValue())) {
                                                found = true;
                                                controlledVocabularyItemsToRemove.add(existing);
                                            }
                                        }
                                        if (!found) {
                                            logger.log(Level.SEVERE, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + cvv.getStrValue() + " not found.");
                                            return error(Response.Status.BAD_REQUEST, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + cvv.getStrValue() + " not found.");
                                        }
                                    }
                                    for (ControlledVocabularyValue remove : controlledVocabularyItemsToRemove) {
                                        dsf.getControlledVocabularyValues().remove(remove);
                                    }

                                } else {
                                    if (dsf.getSingleControlledVocabularyValue().getStrValue().equals(updateField.getSingleControlledVocabularyValue().getStrValue())) {
                                        found = true;
                                        dsf.setSingleControlledVocabularyValue(null);
                                    }

                                }
                            } else {
                                if (!updateField.getDatasetFieldType().isCompound()) {
                                    if (dsf.getDatasetFieldType().isAllowMultiples()) {
                                        for (DatasetFieldValue dfv : updateField.getDatasetFieldValues()) {
                                            for (DatasetFieldValue edsfv : dsf.getDatasetFieldValues()) {
                                                if (edsfv.getDisplayValue().equals(dfv.getDisplayValue())) {
                                                    found = true;
                                                    datasetFieldValueItemsToRemove.add(dfv);
                                                }
                                            }
                                            if (!found) {
                                                logger.log(Level.SEVERE, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + dfv.getDisplayValue() + " not found.");
                                                return error(Response.Status.BAD_REQUEST, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + dfv.getDisplayValue() + " not found.");
                                            }
                                        }
                                        datasetFieldValueItemsToRemove.forEach((remove) -> {
                                            dsf.getDatasetFieldValues().remove(remove);
                                        });

                                    } else {
                                        if (dsf.getSingleValue().getDisplayValue().equals(updateField.getSingleValue().getDisplayValue())) {
                                            found = true;
                                            dsf.setSingleValue(null);
                                        }

                                    }
                                } else {
                                    for (DatasetFieldCompoundValue dfcv : updateField.getDatasetFieldCompoundValues()) {
                                        String deleteVal = getCompoundDisplayValue(dfcv);
                                        for (DatasetFieldCompoundValue existing : dsf.getDatasetFieldCompoundValues()) {
                                            String existingString = getCompoundDisplayValue(existing);
                                            if (existingString.equals(deleteVal)) {
                                                found = true;
                                                datasetFieldCompoundValueItemsToRemove.add(existing);
                                            }
                                        }
                                        datasetFieldCompoundValueItemsToRemove.forEach((remove) -> {
                                            dsf.getDatasetFieldCompoundValues().remove(remove);
                                        });
                                        if (!found) {
                                            logger.log(Level.SEVERE, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + deleteVal + " not found.");
                                            return error(Response.Status.BAD_REQUEST, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + deleteVal + " not found.");
                                        }
                                    }
                                }
                            }
                        } else {
                            found = true;
                            dsf.setSingleValue(null);
                            dsf.setSingleControlledVocabularyValue(null);
                        }
                        break;
                    }
                }
                if (!found){
                    String displayValue = !updateField.getDisplayValue().isEmpty() ? updateField.getDisplayValue() : updateField.getCompoundDisplayValue();
                    logger.log(Level.SEVERE, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + displayValue + " not found." );
                    return error(Response.Status.BAD_REQUEST, "Delete metadata failed: " + updateField.getDatasetFieldType().getDisplayName() + ": " + displayValue + " not found." );
                }
            }


            DatasetVersion managedVersion = execCommand(new UpdateDatasetVersionCommand(ds, req)).getLatestVersion();
            return ok(json(managedVersion, true));

        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset update Json: " + ex.getMessage(), ex);
            return error(Response.Status.BAD_REQUEST, "Error processing metadata delete: " + ex.getMessage());

        } catch (WrappedResponse ex) {
            logger.log(Level.SEVERE, "Delete metadata error: " + ex.getMessage(), ex);
            return ex.getResponse();

        }
    
    }
    
    private String getCompoundDisplayValue (DatasetFieldCompoundValue dscv){
        String returnString = "";
        for (DatasetField dsf : dscv.getChildDatasetFields()) {
            for (String value : dsf.getValues()) {
                if (!(value == null)) {
                    returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
                }
            }
        }
        return returnString;
    }
    
    @PUT
    @AuthRequired
    @Path("{id}/editMetadata")
    public Response editVersionMetadata(@Context ContainerRequestContext crc, String jsonBody, @PathParam("id") String id, @QueryParam("replace") Boolean replace) {

        Boolean replaceData = replace != null;
        DataverseRequest req = null;
        req = createDataverseRequest(getRequestUser(crc));

        return processDatasetUpdate(jsonBody, id, req, replaceData);
    }
    
    
    private Response processDatasetUpdate(String jsonBody, String id, DataverseRequest req, Boolean replaceData){
        try {
           
            Dataset ds = findDatasetOrDie(id);
            JsonObject json = JsonUtil.getJsonObject(jsonBody);
            //Get the current draft or create a new version to update
            DatasetVersion dsv = ds.getOrCreateEditVersion();
            dsv.getTermsOfUseAndAccess().setDatasetVersion(dsv);
            List<DatasetField> fields = new LinkedList<>();
            DatasetField singleField = null;
            
            JsonArray fieldsJson = json.getJsonArray("fields");
            if (fieldsJson == null) {
                singleField = jsonParser().parseField(json, Boolean.FALSE);
                fields.add(singleField);
            } else {
                fields = jsonParser().parseMultipleFields(json);
            }
            

            String valdationErrors = validateDatasetFieldValues(fields);

            if (!valdationErrors.isEmpty()) {
                logger.log(Level.SEVERE, "Semantic error parsing dataset update Json: " + valdationErrors, valdationErrors);
                return error(Response.Status.BAD_REQUEST, "Error parsing dataset update: " + valdationErrors);
            }

            dsv.setVersionState(DatasetVersion.VersionState.DRAFT);

            //loop through the update fields     
            // and compare to the version fields  
            //if exist add/replace values
            //if not add entire dsf
            for (DatasetField updateField : fields) {
                boolean found = false;
                for (DatasetField dsf : dsv.getDatasetFields()) {
                    if (dsf.getDatasetFieldType().equals(updateField.getDatasetFieldType())) {
                        found = true;
                        if (dsf.isEmpty() || dsf.getDatasetFieldType().isAllowMultiples() || replaceData) {
                            List priorCVV = new ArrayList<>();
                            String cvvDisplay = "";

                            if (updateField.getDatasetFieldType().isControlledVocabulary()) {
                                cvvDisplay = dsf.getDisplayValue();
                                for (ControlledVocabularyValue cvvOld : dsf.getControlledVocabularyValues()) {
                                    priorCVV.add(cvvOld);
                                }
                            }

                            if (replaceData) {
                                if (dsf.getDatasetFieldType().isAllowMultiples()) {
                                    dsf.setDatasetFieldCompoundValues(new ArrayList<>());
                                    dsf.setDatasetFieldValues(new ArrayList<>());
                                    dsf.setControlledVocabularyValues(new ArrayList<>());
                                    priorCVV.clear();
                                    dsf.getControlledVocabularyValues().clear();
                                } else {
                                    dsf.setSingleValue("");
                                    dsf.setSingleControlledVocabularyValue(null);
                                }
                              cvvDisplay="";
                            }
                            if (updateField.getDatasetFieldType().isControlledVocabulary()) {
                                if (dsf.getDatasetFieldType().isAllowMultiples()) {
                                    for (ControlledVocabularyValue cvv : updateField.getControlledVocabularyValues()) {
                                        if (!cvvDisplay.contains(cvv.getStrValue())) {
                                            priorCVV.add(cvv);
                                        }
                                    }
                                    dsf.setControlledVocabularyValues(priorCVV);
                                } else {
                                    dsf.setSingleControlledVocabularyValue(updateField.getSingleControlledVocabularyValue());
                                }
                            } else {
                                if (!updateField.getDatasetFieldType().isCompound()) {
                                    if (dsf.getDatasetFieldType().isAllowMultiples()) {
                                        for (DatasetFieldValue dfv : updateField.getDatasetFieldValues()) {
                                            if (!dsf.getDisplayValue().contains(dfv.getDisplayValue())) {
                                                dfv.setDatasetField(dsf);
                                                dsf.getDatasetFieldValues().add(dfv);
                                            }
                                        }
                                    } else {
                                        dsf.setSingleValue(updateField.getValue());
                                    }
                                } else {
                                    for (DatasetFieldCompoundValue dfcv : updateField.getDatasetFieldCompoundValues()) {
                                        if (!dsf.getCompoundDisplayValue().contains(updateField.getCompoundDisplayValue())) {
                                            dfcv.setParentDatasetField(dsf);
                                            dsf.setDatasetVersion(dsv);
                                            dsf.getDatasetFieldCompoundValues().add(dfcv);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (!dsf.isEmpty() && !dsf.getDatasetFieldType().isAllowMultiples() || !replaceData) {
                                return error(Response.Status.BAD_REQUEST, "You may not add data to a field that already has data and does not allow multiples. Use replace=true to replace existing data (" + dsf.getDatasetFieldType().getDisplayName() + ")");
                            }
                        }
                        break;
                    }
                }
                if (!found) {
                    updateField.setDatasetVersion(dsv);
                    dsv.getDatasetFields().add(updateField);
                }
            }
            DatasetVersion managedVersion = execCommand(new UpdateDatasetVersionCommand(ds, req)).getLatestVersion();

            return ok(json(managedVersion, true));

        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset update Json: " + ex.getMessage(), ex);
            return error(Response.Status.BAD_REQUEST, "Error parsing dataset update: " + ex.getMessage());

        } catch (WrappedResponse ex) {
            logger.log(Level.SEVERE, "Update metdata error: " + ex.getMessage(), ex);
            return ex.getResponse();

        }
    }
    
    private String validateDatasetFieldValues(List<DatasetField> fields) {
        StringBuilder error = new StringBuilder();

        for (DatasetField dsf : fields) {
            if (dsf.getDatasetFieldType().isAllowMultiples() && dsf.getControlledVocabularyValues().isEmpty()
                    && dsf.getDatasetFieldCompoundValues().isEmpty() && dsf.getDatasetFieldValues().isEmpty()) {
                error.append("Empty multiple value for field: ").append(dsf.getDatasetFieldType().getDisplayName()).append(" ");
            } else if (!dsf.getDatasetFieldType().isAllowMultiples() && dsf.getSingleValue().getValue().isEmpty()) {
                error.append("Empty value for field: ").append(dsf.getDatasetFieldType().getDisplayName()).append(" ");
            }
        }

        if (!error.toString().isEmpty()) {
            return (error.toString());
        }
        return "";
    }
    
    /**
     * @deprecated This was shipped as a GET but should have been a POST, see https://github.com/IQSS/dataverse/issues/2431
     */
    @GET
    @AuthRequired
    @Path("{id}/actions/:publish")
    @Deprecated
    public Response publishDataseUsingGetDeprecated(@Context ContainerRequestContext crc, @PathParam("id") String id, @QueryParam("type") String type ) {
        logger.info("publishDataseUsingGetDeprecated called on id " + id + ". Encourage use of POST rather than GET, which is deprecated.");
        return publishDataset(crc, id, type, false);
    }

    @POST
    @AuthRequired
    @Path("{id}/actions/:publish")
    public Response publishDataset(@Context ContainerRequestContext crc, @PathParam("id") String id, @QueryParam("type") String type, @QueryParam("assureIsIndexed") boolean mustBeIndexed) {
        try {
            if (type == null) {
                return error(Response.Status.BAD_REQUEST, "Missing 'type' parameter (either 'major','minor', or 'updatecurrent').");
            }
            boolean updateCurrent=false;
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            type = type.toLowerCase();
            boolean isMinor=false;
            switch (type) {
                case "minor":
                    isMinor = true;
                    break;
                case "major":
                    isMinor = false;
                    break;
                case "updatecurrent":
                    if (user.isSuperuser()) {
                        updateCurrent = true;
                    } else {
                        return error(Response.Status.FORBIDDEN, "Only superusers can update the current version");
                    }
                    break;
                default:
                    return error(Response.Status.BAD_REQUEST, "Illegal 'type' parameter value '" + type + "'. It needs to be either 'major', 'minor', or 'updatecurrent'.");
            }

            Dataset ds = findDatasetOrDie(id);
            
            boolean hasValidTerms = TermsOfUseAndAccessValidator.isTOUAValid(ds.getLatestVersion().getTermsOfUseAndAccess(), null);
            if (!hasValidTerms) {
                return error(Status.CONFLICT, BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
            }
            
            if (mustBeIndexed) {
                logger.fine("IT: " + ds.getIndexTime());
                logger.fine("MT: " + ds.getModificationTime());
                logger.fine("PIT: " + ds.getPermissionIndexTime());
                logger.fine("PMT: " + ds.getPermissionModificationTime());
                if (ds.getIndexTime() != null && ds.getModificationTime() != null) {
                    logger.fine("ITMT: " + (ds.getIndexTime().compareTo(ds.getModificationTime()) <= 0));
                }
                /*
                 * Some calls, such as the /datasets/actions/:import* commands do not set the
                 * modification or permission modification times. The checks here are trying to
                 * see if indexing or permissionindexing could be pending, so they check to see
                 * if the relevant modification time is set and if so, whether the index is also
                 * set and if so, if it after the modification time. If the modification time is
                 * set and the index time is null or is before the mod time, the 409/conflict
                 * error is returned.
                 *
                 */
                if ((ds.getModificationTime()!=null && (ds.getIndexTime() == null || (ds.getIndexTime().compareTo(ds.getModificationTime()) <= 0))) ||
                        (ds.getPermissionModificationTime()!=null && (ds.getPermissionIndexTime() == null || (ds.getPermissionIndexTime().compareTo(ds.getPermissionModificationTime()) <= 0)))) {
                    return error(Response.Status.CONFLICT, "Dataset is awaiting indexing");
                }
            }
            if (updateCurrent) {
                /*
                 * Note: The code here mirrors that in the
                 * edu.harvard.iq.dataverse.DatasetPage:updateCurrentVersion method. Any changes
                 * to the core logic (i.e. beyond updating the messaging about results) should
                 * be applied to the code there as well.
                 */
                String errorMsg = null;
                String successMsg = null;
                try {
                    CuratePublishedDatasetVersionCommand cmd = new CuratePublishedDatasetVersionCommand(ds, createDataverseRequest(user));
                    ds = commandEngine.submit(cmd);
                    successMsg = BundleUtil.getStringFromBundle("datasetversion.update.success");

                    // If configured, update archive copy as well
                    String className = settingsService.get(SettingsServiceBean.Key.ArchiverClassName.toString());
                    DatasetVersion updateVersion = ds.getLatestVersion();
                    AbstractSubmitToArchiveCommand archiveCommand = ArchiverUtil.createSubmitToArchiveCommand(className, createDataverseRequest(user), updateVersion);
                    if (archiveCommand != null) {
                        // Delete the record of any existing copy since it is now out of date/incorrect
                        updateVersion.setArchivalCopyLocation(null);
                        /*
                         * Then try to generate and submit an archival copy. Note that running this
                         * command within the CuratePublishedDatasetVersionCommand was causing an error:
                         * "The attribute [id] of class
                         * [edu.harvard.iq.dataverse.DatasetFieldCompoundValue] is mapped to a primary
                         * key column in the database. Updates are not allowed." To avoid that, and to
                         * simplify reporting back to the GUI whether this optional step succeeded, I've
                         * pulled this out as a separate submit().
                         */
                        try {
                            updateVersion = commandEngine.submit(archiveCommand);
                            if (!updateVersion.getArchivalCopyLocationStatus().equals(DatasetVersion.ARCHIVAL_STATUS_FAILURE)) {
                                successMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.success");
                            } else {
                                successMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.failure");
                            }
                        } catch (CommandException ex) {
                            successMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.failure") + " - " + ex.toString();
                            logger.severe(ex.getMessage());
                        }
                    }
                } catch (CommandException ex) {
                    errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.failure") + " - " + ex.toString();
                    logger.severe(ex.getMessage());
                }
                if (errorMsg != null) {
                    return error(Response.Status.INTERNAL_SERVER_ERROR, errorMsg);
                } else {
                    return Response.ok(Json.createObjectBuilder()
                            .add("status", ApiConstants.STATUS_OK)
                            .add("status_details", successMsg)
                            .add("data", json(ds)).build())
                            .type(MediaType.APPLICATION_JSON)
                            .build();
                }
            } else {
                PublishDatasetResult res = execCommand(new PublishDatasetCommand(ds,
                        createDataverseRequest(user),
                        isMinor));
                return res.isWorkflow() ? accepted(json(res.getDataset())) : ok(json(res.getDataset()));
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/actions/:releasemigrated")
    @Consumes("application/ld+json, application/json-ld")
    public Response publishMigratedDataset(@Context ContainerRequestContext crc, String jsonldBody, @PathParam("id") String id, @DefaultValue("false") @QueryParam ("updatepidatprovider") boolean contactPIDProvider) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Only superusers can release migrated datasets");
            }

            Dataset ds = findDatasetOrDie(id);
            try {
                JsonObject metadata = JSONLDUtil.decontextualizeJsonLD(jsonldBody);
                String pubDate = metadata.getString(JsonLDTerm.schemaOrg("datePublished").getUrl());
                logger.fine("Submitted date: " + pubDate);
                LocalDateTime dateTime = null;
                if(!StringUtils.isEmpty(pubDate)) {
                    dateTime = JSONLDUtil.getDateTimeFrom(pubDate);
                    final Timestamp time = Timestamp.valueOf(dateTime);
                    //Set version release date
                    ds.getLatestVersion().setReleaseTime(new Date(time.getTime()));
                }
                // dataset.getPublicationDateFormattedYYYYMMDD())
                // Assign a version number if not set
                if (ds.getLatestVersion().getVersionNumber() == null) {

                    if (ds.getVersions().size() == 1) {
                        // First Release
                        ds.getLatestVersion().setVersionNumber(Long.valueOf(1));
                        ds.getLatestVersion().setMinorVersionNumber(Long.valueOf(0));
                    } else if (ds.getLatestVersion().isMinorUpdate()) {
                        ds.getLatestVersion().setVersionNumber(Long.valueOf(ds.getVersionNumber()));
                        ds.getLatestVersion().setMinorVersionNumber(Long.valueOf(ds.getMinorVersionNumber() + 1));
                    } else {
                        // major, non-first release
                        ds.getLatestVersion().setVersionNumber(Long.valueOf(ds.getVersionNumber() + 1));
                        ds.getLatestVersion().setMinorVersionNumber(Long.valueOf(0));
                    }
                }
                if(ds.getLatestVersion().getVersionNumber()==1 && ds.getLatestVersion().getMinorVersionNumber()==0) {
                    //Also set publication date if this is the first
                    if(dateTime != null) {
                      ds.setPublicationDate(Timestamp.valueOf(dateTime));
                    }
                    // Release User is only set in FinalizeDatasetPublicationCommand if the pub date
                    // is null, so set it here.
                    ds.setReleaseUser((AuthenticatedUser) user);
                }
            } catch (Exception e) {
                logger.fine(e.getMessage());
                throw new BadRequestException("Unable to set publication date ("
                        + JsonLDTerm.schemaOrg("datePublished").getUrl() + "): " + e.getMessage());
            }
            /*
             * Note: The code here mirrors that in the
             * edu.harvard.iq.dataverse.DatasetPage:updateCurrentVersion method. Any changes
             * to the core logic (i.e. beyond updating the messaging about results) should
             * be applied to the code there as well.
             */
            String errorMsg = null;
            Optional<Workflow> prePubWf = wfService.getDefaultWorkflow(TriggerType.PrePublishDataset);

            try {
                // ToDo - should this be in onSuccess()? May relate to todo above
                if (prePubWf.isPresent()) {
                    // Start the workflow, the workflow will call FinalizeDatasetPublication later
                    wfService.start(prePubWf.get(),
                            new WorkflowContext(createDataverseRequest(user), ds, TriggerType.PrePublishDataset, !contactPIDProvider),
                            false);
                } else {
                    FinalizeDatasetPublicationCommand cmd = new FinalizeDatasetPublicationCommand(ds,
                            createDataverseRequest(user), !contactPIDProvider);
                    ds = commandEngine.submit(cmd);
                }
            } catch (CommandException ex) {
                errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.failure") + " - " + ex.toString();
                logger.severe(ex.getMessage());
            }

            if (errorMsg != null) {
                return error(Response.Status.INTERNAL_SERVER_ERROR, errorMsg);
            } else {
                return prePubWf.isPresent() ? accepted(json(ds)) : ok(json(ds));
            }

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/move/{targetDataverseAlias}")
    public Response moveDataset(@Context ContainerRequestContext crc, @PathParam("id") String id, @PathParam("targetDataverseAlias") String targetDataverseAlias, @QueryParam("forceMove") Boolean force) {
        try {
            User u = getRequestUser(crc);
            Dataset ds = findDatasetOrDie(id);
            Dataverse target = dataverseService.findByAlias(targetDataverseAlias);
            if (target == null) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.moveDataset.error.targetDataverseNotFound"));
            }
            //Command requires Super user - it will be tested by the command
            execCommand(new MoveDatasetCommand(
                    createDataverseRequest(u), ds, target, force
            ));
            return ok(BundleUtil.getStringFromBundle("datasets.api.moveDataset.success"));
        } catch (WrappedResponse ex) {
            if (ex.getCause() instanceof UnforcedCommandException) {
                return ex.refineResponse(BundleUtil.getStringFromBundle("datasets.api.moveDataset.error.suggestForce"));
            } else {
                return ex.getResponse();
            }
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/files/actions/:set-embargo")
    public Response createFileEmbargo(@Context ContainerRequestContext crc, @PathParam("id") String id, String jsonBody){

        // user is authenticated
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Status.UNAUTHORIZED, "Authentication is required.");
        }

        Dataset dataset;
        try {
            dataset = findDatasetOrDie(id);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        
        boolean hasValidTerms = TermsOfUseAndAccessValidator.isTOUAValid(dataset.getLatestVersion().getTermsOfUseAndAccess(), null);
        
        if (!hasValidTerms){
            return error(Status.CONFLICT, BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
        }

        // client is superadmin or (client has EditDataset permission on these files and files are unreleased)
        /*
         * This is only a pre-test - if there's no draft version, there are clearly no
         * files that a normal user can change. The converse is not true. A draft
         * version could contain only files that have already been released. Further, we
         * haven't checked the file list yet so the user could still be trying to change
         * released files even if there are some unreleased/draft-only files. Doing this
         * check here does avoid having to do further parsing for some error cases. It
         * also checks the user can edit this dataset, so we don't have to make that
         * check later.
         */

        if ((!authenticatedUser.isSuperuser() && (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) ) || !permissionService.userOn(authenticatedUser, dataset).has(Permission.EditDataset)) {
            return error(Status.FORBIDDEN, "Either the files are released and user is not a superuser or user does not have EditDataset permissions");
        }

        // check if embargoes are allowed(:MaxEmbargoDurationInMonths), gets the :MaxEmbargoDurationInMonths setting variable, if 0 or not set(null) return 400
        long maxEmbargoDurationInMonths = 0;
        try {
            maxEmbargoDurationInMonths  = Long.parseLong(settingsService.get(SettingsServiceBean.Key.MaxEmbargoDurationInMonths.toString()));
        } catch (NumberFormatException nfe){
            if (nfe.getMessage().contains("null")) {
                return error(Status.BAD_REQUEST, "No Embargoes allowed");
            }
        }
        if (maxEmbargoDurationInMonths == 0){
            return error(Status.BAD_REQUEST, "No Embargoes allowed");
        }

        JsonObject json = JsonUtil.getJsonObject(jsonBody);

        Embargo embargo = new Embargo();


        LocalDate currentDateTime = LocalDate.now();
        LocalDate dateAvailable = LocalDate.parse(json.getString("dateAvailable"));

        // check :MaxEmbargoDurationInMonths if -1
        LocalDate maxEmbargoDateTime = maxEmbargoDurationInMonths != -1 ? LocalDate.now().plusMonths(maxEmbargoDurationInMonths) : null;
        // dateAvailable is not in the past
        if (dateAvailable.isAfter(currentDateTime)){
            embargo.setDateAvailable(dateAvailable);
        } else {
            return error(Status.BAD_REQUEST, "Date available can not be in the past");
        }

        // dateAvailable is within limits
        if (maxEmbargoDateTime != null){
            if (dateAvailable.isAfter(maxEmbargoDateTime)){
                return error(Status.BAD_REQUEST, "Date available can not exceed MaxEmbargoDurationInMonths: "+maxEmbargoDurationInMonths);
            }
        }

        embargo.setReason(json.getString("reason"));

        List<DataFile> datasetFiles = dataset.getFiles();
        List<DataFile> filesToEmbargo = new LinkedList<>();

        // extract fileIds from json, find datafiles and add to list
        if (json.containsKey("fileIds")){
            JsonArray fileIds = json.getJsonArray("fileIds");
            for (JsonValue jsv : fileIds) {
                try {
                    DataFile dataFile = findDataFileOrDie(jsv.toString());
                    filesToEmbargo.add(dataFile);
                } catch (WrappedResponse ex) {
                    return ex.getResponse();
                }
            }
        }

        List<Embargo> orphanedEmbargoes = new ArrayList<Embargo>();
        // check if files belong to dataset
        if (datasetFiles.containsAll(filesToEmbargo)) {
            JsonArrayBuilder restrictedFiles = Json.createArrayBuilder();
            boolean badFiles = false;
            for (DataFile datafile : filesToEmbargo) {
                // superuser can overrule an existing embargo, even on released files
                if (datafile.isReleased() && !authenticatedUser.isSuperuser()) {
                    restrictedFiles.add(datafile.getId());
                    badFiles = true;
                }
            }
            if (badFiles) {
                return Response.status(Status.FORBIDDEN)
                        .entity(NullSafeJsonBuilder.jsonObjectBuilder().add("status", ApiConstants.STATUS_ERROR)
                                .add("message", "You do not have permission to embargo the following files")
                                .add("files", restrictedFiles).build())
                        .type(MediaType.APPLICATION_JSON_TYPE).build();
            }
            embargo=embargoService.merge(embargo);
            // Good request, so add the embargo. Track any existing embargoes so we can
            // delete them if there are no files left that reference them.
            for (DataFile datafile : filesToEmbargo) {
                Embargo emb = datafile.getEmbargo();
                if (emb != null) {
                    emb.getDataFiles().remove(datafile);
                    if (emb.getDataFiles().isEmpty()) {
                        orphanedEmbargoes.add(emb);
                    }
                }
                // Save merges the datafile with an embargo into the context
                datafile.setEmbargo(embargo);
                fileService.save(datafile);
            }
            //Call service to get action logged
            long embargoId = embargoService.save(embargo, authenticatedUser.getIdentifier());
            if (orphanedEmbargoes.size() > 0) {
                for (Embargo emb : orphanedEmbargoes) {
                    embargoService.deleteById(emb.getId(), authenticatedUser.getIdentifier());
                }
            }
            //If superuser, report changes to any released files
            if (authenticatedUser.isSuperuser()) {
                String releasedFiles = filesToEmbargo.stream().filter(d -> d.isReleased())
                        .map(d -> d.getId().toString()).collect(Collectors.joining(","));
                if (!releasedFiles.isBlank()) {
                    actionLogSvc
                            .log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "embargoAddedTo")
                                    .setInfo("Embargo id: " + embargo.getId() + " added for released file(s), id(s) "
                                            + releasedFiles + ".")
                                    .setUserIdentifier(authenticatedUser.getIdentifier()));
                }
            }
            return ok(Json.createObjectBuilder().add("message", "Files were embargoed"));
        } else {
            return error(BAD_REQUEST, "Not all files belong to dataset");
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/files/actions/:unset-embargo")
    public Response removeFileEmbargo(@Context ContainerRequestContext crc, @PathParam("id") String id, String jsonBody){

        // user is authenticated
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Status.UNAUTHORIZED, "Authentication is required.");
        }

        Dataset dataset;
        try {
            dataset = findDatasetOrDie(id);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        // client is superadmin or (client has EditDataset permission on these files and files are unreleased)
        // check if files are unreleased(DRAFT?)
        //ToDo - here and below - check the release status of files and not the dataset state (draft dataset version still can have released files)
        if ((!authenticatedUser.isSuperuser() && (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) ) || !permissionService.userOn(authenticatedUser, dataset).has(Permission.EditDataset)) {
            return error(Status.FORBIDDEN, "Either the files are released and user is not a superuser or user does not have EditDataset permissions");
        }

        // check if embargoes are allowed(:MaxEmbargoDurationInMonths), gets the :MaxEmbargoDurationInMonths setting variable, if 0 or not set(null) return 400
        //Todo - is 400 right for embargoes not enabled
        //Todo - handle getting Long for duration in one place (settings getLong method? or is that only in wrapper (view scoped)?
        int maxEmbargoDurationInMonths = 0;
        try {
            maxEmbargoDurationInMonths  = Integer.parseInt(settingsService.get(SettingsServiceBean.Key.MaxEmbargoDurationInMonths.toString()));
        } catch (NumberFormatException nfe){
            if (nfe.getMessage().contains("null")) {
                return error(Status.BAD_REQUEST, "No Embargoes allowed");
            }
        }
        if (maxEmbargoDurationInMonths == 0){
            return error(Status.BAD_REQUEST, "No Embargoes allowed");
        }

        JsonObject json = JsonUtil.getJsonObject(jsonBody);

        List<DataFile> datasetFiles = dataset.getFiles();
        List<DataFile> embargoFilesToUnset = new LinkedList<>();

        // extract fileIds from json, find datafiles and add to list
        if (json.containsKey("fileIds")){
            JsonArray fileIds = json.getJsonArray("fileIds");
            for (JsonValue jsv : fileIds) {
                try {
                    DataFile dataFile = findDataFileOrDie(jsv.toString());
                    embargoFilesToUnset.add(dataFile);
                } catch (WrappedResponse ex) {
                    return ex.getResponse();
                }
            }
        }

        List<Embargo> orphanedEmbargoes = new ArrayList<Embargo>();
        // check if files belong to dataset
        if (datasetFiles.containsAll(embargoFilesToUnset)) {
            JsonArrayBuilder restrictedFiles = Json.createArrayBuilder();
            boolean badFiles = false;
            for (DataFile datafile : embargoFilesToUnset) {
                // superuser can overrule an existing embargo, even on released files
                if (datafile.getEmbargo()==null || ((datafile.isReleased() && datafile.getEmbargo() != null) && !authenticatedUser.isSuperuser())) {
                    restrictedFiles.add(datafile.getId());
                    badFiles = true;
                }
            }
            if (badFiles) {
                return Response.status(Status.FORBIDDEN)
                        .entity(NullSafeJsonBuilder.jsonObjectBuilder().add("status", ApiConstants.STATUS_ERROR)
                                .add("message", "The following files do not have embargoes or you do not have permission to remove their embargoes")
                                .add("files", restrictedFiles).build())
                        .type(MediaType.APPLICATION_JSON_TYPE).build();
            }
            // Good request, so remove the embargo from the files. Track any existing embargoes so we can
            // delete them if there are no files left that reference them.
            for (DataFile datafile : embargoFilesToUnset) {
                Embargo emb = datafile.getEmbargo();
                if (emb != null) {
                    emb.getDataFiles().remove(datafile);
                    if (emb.getDataFiles().isEmpty()) {
                        orphanedEmbargoes.add(emb);
                    }
                }
                // Save merges the datafile with an embargo into the context
                datafile.setEmbargo(null);
                fileService.save(datafile);
            }
            if (orphanedEmbargoes.size() > 0) {
                for (Embargo emb : orphanedEmbargoes) {
                    embargoService.deleteById(emb.getId(), authenticatedUser.getIdentifier());
                }
            }
            String releasedFiles = embargoFilesToUnset.stream().filter(d -> d.isReleased()).map(d->d.getId().toString()).collect(Collectors.joining(","));
            if(!releasedFiles.isBlank()) {
                ActionLogRecord removeRecord = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "embargoRemovedFrom").setInfo("Embargo removed from released file(s), id(s) " + releasedFiles + ".");
                removeRecord.setUserIdentifier(authenticatedUser.getIdentifier());
                actionLogSvc.log(removeRecord);
            }
            return ok(Json.createObjectBuilder().add("message", "Embargo(es) were removed from files"));
        } else {
            return error(BAD_REQUEST, "Not all files belong to dataset");
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/files/actions/:set-retention")
    public Response createFileRetention(@Context ContainerRequestContext crc, @PathParam("id") String id, String jsonBody){

        // user is authenticated
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Status.UNAUTHORIZED, "Authentication is required.");
        }

        Dataset dataset;
        try {
            dataset = findDatasetOrDie(id);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        boolean hasValidTerms = TermsOfUseAndAccessValidator.isTOUAValid(dataset.getLatestVersion().getTermsOfUseAndAccess(), null);

        if (!hasValidTerms){
            return error(Status.CONFLICT, BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
        }

        // client is superadmin or (client has EditDataset permission on these files and files are unreleased)
        // check if files are unreleased(DRAFT?)
        if ((!authenticatedUser.isSuperuser() && (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) ) || !permissionService.userOn(authenticatedUser, dataset).has(Permission.EditDataset)) {
            return error(Status.FORBIDDEN, "Either the files are released and user is not a superuser or user does not have EditDataset permissions");
        }

        // check if retentions are allowed(:MinRetentionDurationInMonths), gets the :MinRetentionDurationInMonths setting variable, if 0 or not set(null) return 400
        long minRetentionDurationInMonths = 0;
        try {
            minRetentionDurationInMonths  = Long.parseLong(settingsService.get(SettingsServiceBean.Key.MinRetentionDurationInMonths.toString()));
        } catch (NumberFormatException nfe){
            if (nfe.getMessage().contains("null")) {
                return error(Status.BAD_REQUEST, "No Retention periods allowed");
            }
        }
        if (minRetentionDurationInMonths == 0){
            return error(Status.BAD_REQUEST, "No Retention periods allowed");
        }

        JsonObject json;
        try {
            json = JsonUtil.getJsonObject(jsonBody);
        } catch (JsonException ex) {
            return error(Status.BAD_REQUEST, "Invalid JSON; error message: " + ex.getMessage());
        }

        Retention retention = new Retention();


        LocalDate currentDateTime = LocalDate.now();

        // Extract the dateUnavailable - check if specified and valid
        String dateUnavailableStr = "";
        LocalDate dateUnavailable;
        try {
            dateUnavailableStr = json.getString("dateUnavailable");
            dateUnavailable = LocalDate.parse(dateUnavailableStr);
        } catch (NullPointerException npex) {
            return error(Status.BAD_REQUEST, "Invalid retention period; no dateUnavailable specified");
        } catch (ClassCastException ccex) {
            return error(Status.BAD_REQUEST, "Invalid retention period; dateUnavailable must be a string");
        } catch (DateTimeParseException dtpex) {
            return error(Status.BAD_REQUEST, "Invalid date format for dateUnavailable: " + dateUnavailableStr);
        }

        // check :MinRetentionDurationInMonths if -1
        LocalDate minRetentionDateTime = minRetentionDurationInMonths != -1 ? LocalDate.now().plusMonths(minRetentionDurationInMonths) : null;
        // dateUnavailable is not in the past
        if (dateUnavailable.isAfter(currentDateTime)){
            retention.setDateUnavailable(dateUnavailable);
        } else {
            return error(Status.BAD_REQUEST, "Date unavailable can not be in the past");
        }

        // dateAvailable is within limits
        if (minRetentionDateTime != null){
            if (dateUnavailable.isBefore(minRetentionDateTime)){
                return error(Status.BAD_REQUEST, "Date unavailable can not be earlier than MinRetentionDurationInMonths: "+minRetentionDurationInMonths + " from now");
            }
        }
        
        try {
            String reason = json.getString("reason");
            retention.setReason(reason);
        } catch (NullPointerException npex) {
            // ignoring; no reason specified is OK, it is optional
        } catch (ClassCastException ccex) {
            return error(Status.BAD_REQUEST, "Invalid retention period; reason must be a string");
        }


        List<DataFile> datasetFiles = dataset.getFiles();
        List<DataFile> filesToRetention = new LinkedList<>();

        // extract fileIds from json, find datafiles and add to list
        if (json.containsKey("fileIds")){
            try {
                JsonArray fileIds = json.getJsonArray("fileIds");
                for (JsonValue jsv : fileIds) {
                    try {
                        DataFile dataFile = findDataFileOrDie(jsv.toString());
                        filesToRetention.add(dataFile);
                    } catch (WrappedResponse ex) {
                        return ex.getResponse();
                    }
                }
            } catch (ClassCastException ccex) {
                return error(Status.BAD_REQUEST, "Invalid retention period; fileIds must be an array of id strings");
            } catch (NullPointerException npex) {
                return error(Status.BAD_REQUEST, "Invalid retention period; no fileIds specified");
            }
        } else {
            return error(Status.BAD_REQUEST, "No fileIds specified");
        }

        List<Retention> orphanedRetentions = new ArrayList<Retention>();
        // check if files belong to dataset
        if (datasetFiles.containsAll(filesToRetention)) {
            JsonArrayBuilder restrictedFiles = Json.createArrayBuilder();
            boolean badFiles = false;
            for (DataFile datafile : filesToRetention) {
                // superuser can overrule an existing retention, even on released files
                if (datafile.isReleased() && !authenticatedUser.isSuperuser()) {
                    restrictedFiles.add(datafile.getId());
                    badFiles = true;
                }
            }
            if (badFiles) {
                return Response.status(Status.FORBIDDEN)
                        .entity(NullSafeJsonBuilder.jsonObjectBuilder().add("status", ApiConstants.STATUS_ERROR)
                                .add("message", "You do not have permission to set a retention period for the following files")
                                .add("files", restrictedFiles).build())
                        .type(MediaType.APPLICATION_JSON_TYPE).build();
            }
            retention=retentionService.merge(retention);
            // Good request, so add the retention. Track any existing retentions so we can
            // delete them if there are no files left that reference them.
            for (DataFile datafile : filesToRetention) {
                Retention ret = datafile.getRetention();
                if (ret != null) {
                    ret.getDataFiles().remove(datafile);
                    if (ret.getDataFiles().isEmpty()) {
                        orphanedRetentions.add(ret);
                    }
                }
                // Save merges the datafile with an retention into the context
                datafile.setRetention(retention);
                fileService.save(datafile);
            }
            //Call service to get action logged
            long retentionId = retentionService.save(retention, authenticatedUser.getIdentifier());
            if (orphanedRetentions.size() > 0) {
                for (Retention ret : orphanedRetentions) {
                    retentionService.delete(ret, authenticatedUser.getIdentifier());
                }
            }
            //If superuser, report changes to any released files
            if (authenticatedUser.isSuperuser()) {
                String releasedFiles = filesToRetention.stream().filter(d -> d.isReleased())
                        .map(d -> d.getId().toString()).collect(Collectors.joining(","));
                if (!releasedFiles.isBlank()) {
                    actionLogSvc
                            .log(new ActionLogRecord(ActionLogRecord.ActionType.Admin, "retentionAddedTo")
                                    .setInfo("Retention id: " + retention.getId() + " added for released file(s), id(s) "
                                            + releasedFiles + ".")
                                    .setUserIdentifier(authenticatedUser.getIdentifier()));
                }
            }
            return ok(Json.createObjectBuilder().add("message", "File(s) retention period has been set or updated"));
        } else {
            return error(BAD_REQUEST, "Not all files belong to dataset");
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/files/actions/:unset-retention")
    public Response removeFileRetention(@Context ContainerRequestContext crc, @PathParam("id") String id, String jsonBody){

        // user is authenticated
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Status.UNAUTHORIZED, "Authentication is required.");
        }

        Dataset dataset;
        try {
            dataset = findDatasetOrDie(id);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        // client is superadmin or (client has EditDataset permission on these files and files are unreleased)
        // check if files are unreleased(DRAFT?)
        //ToDo - here and below - check the release status of files and not the dataset state (draft dataset version still can have released files)
        if ((!authenticatedUser.isSuperuser() && (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) ) || !permissionService.userOn(authenticatedUser, dataset).has(Permission.EditDataset)) {
            return error(Status.FORBIDDEN, "Either the files are released and user is not a superuser or user does not have EditDataset permissions");
        }

        // check if retentions are allowed(:MinRetentionDurationInMonths), gets the :MinRetentionDurationInMonths setting variable, if 0 or not set(null) return 400
        int minRetentionDurationInMonths = 0;
        try {
            minRetentionDurationInMonths  = Integer.parseInt(settingsService.get(SettingsServiceBean.Key.MinRetentionDurationInMonths.toString()));
        } catch (NumberFormatException nfe){
            if (nfe.getMessage().contains("null")) {
                return error(Status.BAD_REQUEST, "No Retention periods allowed");
            }
        }
        if (minRetentionDurationInMonths == 0){
            return error(Status.BAD_REQUEST, "No Retention periods allowed");
        }

        JsonObject json;
        try {
            json = JsonUtil.getJsonObject(jsonBody);
        } catch (JsonException ex) {
            return error(Status.BAD_REQUEST, "Invalid JSON; error message: " + ex.getMessage());
        }

        List<DataFile> datasetFiles = dataset.getFiles();
        List<DataFile> retentionFilesToUnset = new LinkedList<>();

        // extract fileIds from json, find datafiles and add to list
        if (json.containsKey("fileIds")){
            try {
                JsonArray fileIds = json.getJsonArray("fileIds");
                for (JsonValue jsv : fileIds) {
                    try {
                        DataFile dataFile = findDataFileOrDie(jsv.toString());
                        retentionFilesToUnset.add(dataFile);
                    } catch (WrappedResponse ex) {
                        return ex.getResponse();
                    }
                }
            } catch (ClassCastException ccex) {
                return error(Status.BAD_REQUEST, "fileIds must be an array of id strings");
            } catch (NullPointerException npex) {
                return error(Status.BAD_REQUEST, "No fileIds specified");
            }
        } else {
            return error(Status.BAD_REQUEST, "No fileIds specified");
        }

        List<Retention> orphanedRetentions = new ArrayList<Retention>();
        // check if files belong to dataset
        if (datasetFiles.containsAll(retentionFilesToUnset)) {
            JsonArrayBuilder restrictedFiles = Json.createArrayBuilder();
            boolean badFiles = false;
            for (DataFile datafile : retentionFilesToUnset) {
                // superuser can overrule an existing retention, even on released files
                if (datafile.getRetention()==null || ((datafile.isReleased() && datafile.getRetention() != null) && !authenticatedUser.isSuperuser())) {
                    restrictedFiles.add(datafile.getId());
                    badFiles = true;
                }
            }
            if (badFiles) {
                return Response.status(Status.FORBIDDEN)
                        .entity(NullSafeJsonBuilder.jsonObjectBuilder().add("status", ApiConstants.STATUS_ERROR)
                                .add("message", "The following files do not have retention periods or you do not have permission to remove their retention periods")
                                .add("files", restrictedFiles).build())
                        .type(MediaType.APPLICATION_JSON_TYPE).build();
            }
            // Good request, so remove the retention from the files. Track any existing retentions so we can
            // delete them if there are no files left that reference them.
            for (DataFile datafile : retentionFilesToUnset) {
                Retention ret = datafile.getRetention();
                if (ret != null) {
                    ret.getDataFiles().remove(datafile);
                    if (ret.getDataFiles().isEmpty()) {
                        orphanedRetentions.add(ret);
                    }
                }
                // Save merges the datafile with an retention into the context
                datafile.setRetention(null);
                fileService.save(datafile);
            }
            if (orphanedRetentions.size() > 0) {
                for (Retention ret : orphanedRetentions) {
                    retentionService.delete(ret, authenticatedUser.getIdentifier());
                }
            }
            String releasedFiles = retentionFilesToUnset.stream().filter(d -> d.isReleased()).map(d->d.getId().toString()).collect(Collectors.joining(","));
            if(!releasedFiles.isBlank()) {
                ActionLogRecord removeRecord = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "retentionRemovedFrom").setInfo("Retention removed from released file(s), id(s) " + releasedFiles + ".");
                removeRecord.setUserIdentifier(authenticatedUser.getIdentifier());
                actionLogSvc.log(removeRecord);
            }
            return ok(Json.createObjectBuilder().add("message", "Retention periods were removed from file(s)"));
        } else {
            return error(BAD_REQUEST, "Not all files belong to dataset");
        }
    }

    @PUT
    @AuthRequired
    @Path("{linkedDatasetId}/link/{linkingDataverseAlias}")
    public Response linkDataset(@Context ContainerRequestContext crc, @PathParam("linkedDatasetId") String linkedDatasetId, @PathParam("linkingDataverseAlias") String linkingDataverseAlias) {
        try {
            User u = getRequestUser(crc);
            Dataset linked = findDatasetOrDie(linkedDatasetId);
            Dataverse linking = findDataverseOrDie(linkingDataverseAlias);
            if (linked == null){
                return error(Response.Status.BAD_REQUEST, "Linked Dataset not found.");
            }
            if (linking == null) {
                return error(Response.Status.BAD_REQUEST, "Linking Dataverse not found.");
            }
            execCommand(new LinkDatasetCommand(
                    createDataverseRequest(u), linking, linked
            ));
            return ok("Dataset " + linked.getId() + " linked successfully to " + linking.getAlias());
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @Path("{id}/versions/{versionId}/customlicense")
    public Response getCustomTermsTab(@PathParam("id") String id, @PathParam("versionId") String versionId,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        User user = session.getUser();
        String persistentId;
        try {
            if (DatasetUtil.getLicense(getDatasetVersionOrDie(createDataverseRequest(user), versionId, findDatasetOrDie(id), uriInfo, headers)) != null) {
                return error(Status.NOT_FOUND, "This Dataset has no custom license");
            }
            persistentId = getRequestParameter(":persistentId".substring(1));
            if (versionId.equals(DS_VERSION_DRAFT)) {
                versionId = "DRAFT";
            }
        } catch (WrappedResponse wrappedResponse) {
            return wrappedResponse.getResponse();
        }
        return Response.seeOther(URI.create(systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId="
                + persistentId + "&version=" + versionId + "&selectTab=termsTab")).build();
    }


    @GET
    @AuthRequired
    @Path("{id}/links")
    public Response getLinks(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied ) {
        try {
            User u = getRequestUser(crc);
            if (!u.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Not a superuser");
            }
            Dataset dataset = findDatasetOrDie(idSupplied);

            long datasetId = dataset.getId();
            List<Dataverse> dvsThatLinkToThisDatasetId = dataverseSvc.findDataversesThatLinkToThisDatasetId(datasetId);
            JsonArrayBuilder dataversesThatLinkToThisDatasetIdBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThatLinkToThisDatasetId) {
                dataversesThatLinkToThisDatasetIdBuilder.add(dataverse.getAlias() + " (id " + dataverse.getId() + ")");
            }
            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("dataverses that link to dataset id " + datasetId, dataversesThatLinkToThisDatasetIdBuilder);
            return ok(response);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    /**
     * Add a given assignment to a given user or group
     * @param ra     role assignment DTO
     * @param id     dataset id
     * @param apiKey
     */
    @POST
    @AuthRequired
    @Path("{identifier}/assignments")
    public Response createAssignment(@Context ContainerRequestContext crc, RoleAssignmentDTO ra, @PathParam("identifier") String id, @QueryParam("key") String apiKey) {
        try {
            Dataset dataset = findDatasetOrDie(id);
            
            RoleAssignee assignee = findAssignee(ra.getAssignee());
            if (assignee == null) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.grant.role.assignee.not.found.error"));
            }
            
            DataverseRole theRole;
            Dataverse dv = dataset.getOwner();
            theRole = null;
            while ((theRole == null) && (dv != null)) {
                for (DataverseRole aRole : rolesSvc.availableRoles(dv.getId())) {
                    if (aRole.getAlias().equals(ra.getRole())) {
                        theRole = aRole;
                        break;
                    }
                }
                dv = dv.getOwner();
            }
            if (theRole == null) {
                List<String> args = Arrays.asList(ra.getRole(), dataset.getOwner().getDisplayName());
                return error(Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.grant.role.not.found.error", args));
            }

            String privateUrlToken = null;
            return ok(
                    json(execCommand(new AssignRoleCommand(assignee, theRole, dataset, createDataverseRequest(getRequestUser(crc)), privateUrlToken))));
        } catch (WrappedResponse ex) {
            List<String> args = Arrays.asList(ex.getMessage());
            logger.log(Level.WARNING, BundleUtil.getStringFromBundle("datasets.api.grant.role.cant.create.assignment.error", args));
            return ex.getResponse();
        }

    }
    
    @DELETE
    @AuthRequired
    @Path("{identifier}/assignments/{id}")
    public Response deleteAssignment(@Context ContainerRequestContext crc, @PathParam("id") long assignmentId, @PathParam("identifier") String dsId) {
        RoleAssignment ra = em.find(RoleAssignment.class, assignmentId);
        if (ra != null) {
            try {
                findDatasetOrDie(dsId);
                execCommand(new RevokeRoleCommand(ra, createDataverseRequest(getRequestUser(crc))));
                List<String> args = Arrays.asList(ra.getRole().getName(), ra.getAssigneeIdentifier(), ra.getDefinitionPoint().accept(DvObject.NamePrinter));
                return ok(BundleUtil.getStringFromBundle("datasets.api.revoke.role.success", args));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        } else {
            List<String> args = Arrays.asList(Long.toString(assignmentId));
            return error(Status.NOT_FOUND, BundleUtil.getStringFromBundle("datasets.api.revoke.role.not.found.error", args));
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/assignments")
    public Response getAssignments(@Context ContainerRequestContext crc, @PathParam("identifier") String id) {
        return response(req ->
                ok(execCommand(
                        new ListRoleAssignments(req, findDatasetOrDie(id)))
                        .stream().map(ra -> json(ra)).collect(toJsonArray())), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/privateUrl")
    public Response getPrivateUrlData(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        return response( req -> {
            PrivateUrl privateUrl = execCommand(new GetPrivateUrlCommand(req, findDatasetOrDie(idSupplied)));
            return (privateUrl != null) ? ok(json(privateUrl))
                    : error(Response.Status.NOT_FOUND, "Private URL not found.");
        }, getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{id}/privateUrl")
    public Response createPrivateUrl(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied,@DefaultValue("false") @QueryParam ("anonymizedAccess") boolean anonymizedAccess) {
        if(anonymizedAccess && settingsSvc.getValueForKey(SettingsServiceBean.Key.AnonymizedFieldTypeNames)==null) {
            throw new NotAcceptableException("Anonymized Access not enabled");
        }
        return response(req ->
                ok(json(execCommand(
                new CreatePrivateUrlCommand(req, findDatasetOrDie(idSupplied), anonymizedAccess)))), getRequestUser(crc));
    }

    @DELETE
    @AuthRequired
    @Path("{id}/privateUrl")
    public Response deletePrivateUrl(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        return response( req -> {
            Dataset dataset = findDatasetOrDie(idSupplied);
            PrivateUrl privateUrl = execCommand(new GetPrivateUrlCommand(req, dataset));
            if (privateUrl != null) {
                execCommand(new DeletePrivateUrlCommand(req, dataset));
                return ok("Private URL deleted.");
            } else {
                return notFound("No Private URL to delete.");
            }
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/thumbnail/candidates")
    public Response getDatasetThumbnailCandidates(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            boolean canUpdateThumbnail = false;
            canUpdateThumbnail = permissionSvc.requestOn(createDataverseRequest(getRequestUser(crc)), dataset).canIssue(UpdateDatasetThumbnailCommand.class);
            if (!canUpdateThumbnail) {
                return error(Response.Status.FORBIDDEN, "You are not permitted to list dataset thumbnail candidates.");
            }
            JsonArrayBuilder data = Json.createArrayBuilder();
            boolean considerDatasetLogoAsCandidate = true;
            for (DatasetThumbnail datasetThumbnail : DatasetUtil.getThumbnailCandidates(dataset, considerDatasetLogoAsCandidate, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE)) {
                JsonObjectBuilder candidate = Json.createObjectBuilder();
                String base64image = datasetThumbnail.getBase64image();
                if (base64image != null) {
                    logger.fine("found a candidate!");
                    candidate.add("base64image", base64image);
                }
                DataFile dataFile = datasetThumbnail.getDataFile();
                if (dataFile != null) {
                    candidate.add("dataFileId", dataFile.getId());
                }
                data.add(candidate);
            }
            return ok(data);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "Could not find dataset based on id supplied: " + idSupplied + ".");
        }
    }

    @GET
    @Produces({"image/png"})
    @Path("{id}/thumbnail")
    public Response getDatasetThumbnail(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            InputStream is = DatasetUtil.getThumbnailAsInputStream(dataset, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            if(is == null) {
                return notFound("Thumbnail not available");
            }
            return Response.ok(is).build();
        } catch (WrappedResponse wr) {
            return notFound("Thumbnail not available");
        }
    }

    @GET
    @Produces({ "image/png" })
    @Path("{id}/logo")
    public Response getDatasetLogo(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            InputStream is = DatasetUtil.getLogoAsInputStream(dataset);
            if (is == null) {
                return notFound("Logo not available");
            }
            return Response.ok(is).build();
        } catch (WrappedResponse wr) {
            return notFound("Logo not available");
        }
    }

    // TODO: Rather than only supporting looking up files by their database IDs (dataFileIdSupplied), consider supporting persistent identifiers.
    @POST
    @AuthRequired
    @Path("{id}/thumbnail/{dataFileId}")
    public Response setDataFileAsThumbnail(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied, @PathParam("dataFileId") long dataFileIdSupplied) {
        try {
            DatasetThumbnail datasetThumbnail = execCommand(new UpdateDatasetThumbnailCommand(createDataverseRequest(getRequestUser(crc)), findDatasetOrDie(idSupplied), UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, dataFileIdSupplied, null));
            return ok("Thumbnail set to " + datasetThumbnail.getBase64image());
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/thumbnail")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @Operation(summary = "Uploads a logo for a dataset", 
               description = "Uploads a logo for a dataset")
    @APIResponse(responseCode = "200",
               description = "Dataset logo uploaded successfully")
    @Tag(name = "uploadDatasetLogo", 
         description = "Uploads a logo for a dataset")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA))          
    public Response uploadDatasetLogo(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied, @FormDataParam("file") InputStream inputStream) {
        try {
            DatasetThumbnail datasetThumbnail = execCommand(new UpdateDatasetThumbnailCommand(createDataverseRequest(getRequestUser(crc)), findDatasetOrDie(idSupplied), UpdateDatasetThumbnailCommand.UserIntent.setNonDatasetFileAsThumbnail, null, inputStream));
            return ok("Thumbnail is now " + datasetThumbnail.getBase64image());
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("{id}/thumbnail")
    public Response removeDatasetLogo(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        try {
            execCommand(new UpdateDatasetThumbnailCommand(createDataverseRequest(getRequestUser(crc)), findDatasetOrDie(idSupplied), UpdateDatasetThumbnailCommand.UserIntent.removeThumbnail, null, null));
            return ok("Dataset thumbnail removed.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @Deprecated(forRemoval = true, since = "2024-07-07")
    @GET
    @AuthRequired
    @Path("{identifier}/dataCaptureModule/rsync")
    public Response getRsync(@Context ContainerRequestContext crc, @PathParam("identifier") String id) {
        //TODO - does it make sense to switch this to dataset identifier for consistency with the rest of the DCM APIs?
        if (!DataCaptureModuleUtil.rsyncSupportEnabled(settingsSvc.getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            return error(Response.Status.METHOD_NOT_ALLOWED, SettingsServiceBean.Key.UploadMethods + " does not contain " + SystemConfig.FileUploadMethods.RSYNC + ".");
        }
        Dataset dataset = null;
        try {
            dataset = findDatasetOrDie(id);
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            ScriptRequestResponse scriptRequestResponse = execCommand(new RequestRsyncScriptCommand(createDataverseRequest(user), dataset));
            
            DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, user.getId(), "script downloaded");
            if (lock == null) {
                logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
                return error(Response.Status.FORBIDDEN, "Failed to lock the dataset (dataset id="+dataset.getId()+")");
            }
            return ok(scriptRequestResponse.getScript(), MediaType.valueOf(MediaType.TEXT_PLAIN), null);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        } catch (EJBException ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Something went wrong attempting to download rsync script: " + EjbUtil.ejbExceptionToString(ex));
        }
    }
    
    /**
     * This api endpoint triggers the creation of a "package" file in a dataset
     * after that package has been moved onto the same filesystem via the Data Capture Module.
     * The package is really just a way that Dataverse interprets a folder created by DCM, seeing it as just one file.
     * The "package" can be downloaded over RSAL.
     *
     * This endpoint currently supports both posix file storage and AWS s3 storage in Dataverse, and depending on which one is active acts accordingly.
     *
     * The initial design of the DCM/Dataverse interaction was not to use packages, but to allow import of all individual files natively into Dataverse.
     * But due to the possibly immense number of files (millions) the package approach was taken.
     * This is relevant because the posix ("file") code contains many remnants of that development work.
     * The s3 code was written later and is set to only support import as packages. It takes a lot from FileRecordWriter.
     * -MAD 4.9.1
     */
    @POST
    @AuthRequired
    @Path("{identifier}/dataCaptureModule/checksumValidation")
    public Response receiveChecksumValidationResults(@Context ContainerRequestContext crc, @PathParam("identifier") String id, JsonObject jsonFromDcm) {
        logger.log(Level.FINE, "jsonFromDcm: {0}", jsonFromDcm);
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!authenticatedUser.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        String statusMessageFromDcm = jsonFromDcm.getString("status");
        try {
            Dataset dataset = findDatasetOrDie(id);
            if ("validation passed".equals(statusMessageFromDcm)) {
                logger.log(Level.INFO, "Checksum Validation passed for DCM.");

                String storageDriver = dataset.getDataverseContext().getEffectiveStorageDriverId();
                String uploadFolder = jsonFromDcm.getString("uploadFolder");
                int totalSize = jsonFromDcm.getInt("totalSize");
                String storageDriverType = System.getProperty("dataverse.file." + storageDriver + ".type");
                
                if (storageDriverType.equals("file")) {
                    logger.log(Level.INFO, "File storage driver used for (dataset id={0})", dataset.getId());

                    ImportMode importMode = ImportMode.MERGE;
                    try {
                        JsonObject jsonFromImportJobKickoff = execCommand(new ImportFromFileSystemCommand(createDataverseRequest(getRequestUser(crc)), dataset, uploadFolder, new Long(totalSize), importMode));
                        long jobId = jsonFromImportJobKickoff.getInt("executionId");
                        String message = jsonFromImportJobKickoff.getString("message");
                        JsonObjectBuilder job = Json.createObjectBuilder();
                        job.add("jobId", jobId);
                        job.add("message", message);
                        return ok(job);
                    } catch (WrappedResponse wr) {
                        String message = wr.getMessage();
                        return error(Response.Status.INTERNAL_SERVER_ERROR, "Uploaded files have passed checksum validation but something went wrong while attempting to put the files into Dataverse. Message was '" + message + "'.");
                    }
                } else if(storageDriverType.equals(DataAccess.S3)) {
                    
                    logger.log(Level.INFO, "S3 storage driver used for DCM (dataset id={0})", dataset.getId());
                    try {
                        
                        //Where the lifting is actually done, moving the s3 files over and having dataverse know of the existance of the package
                        s3PackageImporter.copyFromS3(dataset, uploadFolder);
                        DataFile packageFile = s3PackageImporter.createPackageDataFile(dataset, uploadFolder, new Long(totalSize));
                        
                        if (packageFile == null) {
                            logger.log(Level.SEVERE, "S3 File package import failed.");
                            return error(Response.Status.INTERNAL_SERVER_ERROR, "S3 File package import failed.");
                        }
                        DatasetLock dcmLock = dataset.getLockFor(DatasetLock.Reason.DcmUpload);
                        if (dcmLock == null) {
                            logger.log(Level.WARNING, "Dataset not locked for DCM upload");
                        } else {
                            datasetService.removeDatasetLocks(dataset, DatasetLock.Reason.DcmUpload);
                            dataset.removeLock(dcmLock);
                        }
                        
                        // update version using the command engine to enforce user permissions and constraints
                        if (dataset.getVersions().size() == 1 && dataset.getLatestVersion().getVersionState() == DatasetVersion.VersionState.DRAFT) {
                            try {
                                Command<Dataset> cmd;
                                cmd = new UpdateDatasetVersionCommand(dataset, new DataverseRequest(authenticatedUser, (HttpServletRequest) null));
                                commandEngine.submit(cmd);
                            } catch (CommandException ex) {
                                return error(Response.Status.INTERNAL_SERVER_ERROR, "CommandException updating DatasetVersion from batch job: " + ex.getMessage());
                            }
                        } else {
                            String constraintError = "ConstraintException updating DatasetVersion form batch job: dataset must be a "
                                    + "single version in draft mode.";
                            logger.log(Level.SEVERE, constraintError);
                        }

                        JsonObjectBuilder job = Json.createObjectBuilder();
                        return ok(job);
                        
                    } catch (IOException e) {
                        String message = e.getMessage();
                        return error(Response.Status.INTERNAL_SERVER_ERROR, "Uploaded files have passed checksum validation but something went wrong while attempting to move the files into Dataverse. Message was '" + message + "'.");
                    }
                } else {
                    return error(Response.Status.INTERNAL_SERVER_ERROR, "Invalid storage driver in Dataverse, not compatible with dcm");
                }
            } else if ("validation failed".equals(statusMessageFromDcm)) {
                Map<String, AuthenticatedUser> distinctAuthors = permissionService.getDistinctUsersWithPermissionOn(Permission.EditDataset, dataset);
                distinctAuthors.values().forEach((value) -> {
                    userNotificationService.sendNotification((AuthenticatedUser) value, new Timestamp(new Date().getTime()), UserNotification.Type.CHECKSUMFAIL, dataset.getId());
                });
                List<AuthenticatedUser> superUsers = authenticationServiceBean.findSuperUsers();
                if (superUsers != null && !superUsers.isEmpty()) {
                    superUsers.forEach((au) -> {
                        userNotificationService.sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.CHECKSUMFAIL, dataset.getId());
                    });
                }
                return ok("User notified about checksum validation failure.");
            } else {
                return error(Response.Status.BAD_REQUEST, "Unexpected status cannot be processed: " + statusMessageFromDcm);
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    

    @POST
    @AuthRequired
    @Path("{id}/submitForReview")
    public Response submitForReview(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        try {
            Dataset updatedDataset = execCommand(new SubmitDatasetForReviewCommand(createDataverseRequest(getRequestUser(crc)), findDatasetOrDie(idSupplied)));
            JsonObjectBuilder result = Json.createObjectBuilder();
            
            boolean inReview = updatedDataset.isLockedFor(DatasetLock.Reason.InReview);
            
            result.add("inReview", inReview);
            result.add("message", "Dataset id " + updatedDataset.getId() + " has been submitted for review.");
            return ok(result);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/returnToAuthor")
    public Response returnToAuthor(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied, String jsonBody) {

        if (jsonBody == null || jsonBody.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "You must supply JSON to this API endpoint and it must contain a reason for returning the dataset (field: reasonForReturn).");
        }
        JsonObject json = JsonUtil.getJsonObject(jsonBody);
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            String reasonForReturn = null;
            reasonForReturn = json.getString("reasonForReturn");
            if ((reasonForReturn == null || reasonForReturn.isEmpty())
                    && !FeatureFlags.DISABLE_RETURN_TO_AUTHOR_REASON.enabled()) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("dataset.reject.datasetNotInReview"));
            }
            AuthenticatedUser authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            Dataset updatedDataset = execCommand(new ReturnDatasetToAuthorCommand(createDataverseRequest(authenticatedUser), dataset, reasonForReturn ));

            JsonObjectBuilder result = Json.createObjectBuilder();
            result.add("inReview", false);
            result.add("message", "Dataset id " + updatedDataset.getId() + " has been sent back to the author(s).");
            return ok(result);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{id}/curationStatus")
    public Response getCurationStatus(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        try {
            Dataset ds = findDatasetOrDie(idSupplied);
            DatasetVersion dsv = ds.getLatestVersion();
            User user = getRequestUser(crc);
            if (dsv.isDraft() && permissionSvc.requestOn(createDataverseRequest(user), ds).has(Permission.PublishDataset)) {
                return response(req -> ok(dsv.getExternalStatusLabel()==null ? "":dsv.getExternalStatusLabel()), user);
            } else {
                return error(Response.Status.FORBIDDEN, "You are not permitted to view the curation status of this dataset.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{id}/curationStatus")
    public Response setCurationStatus(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied, @QueryParam("label") String label) {
        Dataset ds = null;
        User u = null;
        try {
            ds = findDatasetOrDie(idSupplied);
            u = getRequestUser(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        try {
            execCommand(new SetCurationStatusCommand(createDataverseRequest(u), ds, label));
            return ok("Curation Status updated");
        } catch (WrappedResponse wr) {
            // Just change to Bad Request and send
            return Response.fromResponse(wr.getResponse()).status(Response.Status.BAD_REQUEST).build();
        }
    }

    @DELETE
    @AuthRequired
    @Path("{id}/curationStatus")
    public Response deleteCurationStatus(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied) {
        Dataset ds = null;
        User u = null;
        try {
            ds = findDatasetOrDie(idSupplied);
            u = getRequestUser(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        try {
            execCommand(new SetCurationStatusCommand(createDataverseRequest(u), ds, null));
            return ok("Curation Status deleted");
        } catch (WrappedResponse wr) {
            //Just change to Bad Request and send
            return Response.fromResponse(wr.getResponse()).status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @AuthRequired
    @Path("{id}/uploadurls")
    public Response getMPUploadUrls(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied, @QueryParam("size") long fileSize) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);

            boolean canUpdateDataset = false;
            canUpdateDataset = permissionSvc.requestOn(createDataverseRequest(getRequestUser(crc)), dataset)
                    .canIssue(UpdateDatasetVersionCommand.class);
            if (!canUpdateDataset) {
                return error(Response.Status.FORBIDDEN, "You are not permitted to upload files to this dataset.");
            }
            S3AccessIO<DataFile> s3io = FileUtil.getS3AccessForDirectUpload(dataset);
            if (s3io == null) {
                return error(Response.Status.NOT_FOUND,
                        "Direct upload not supported for files in this dataset: " + dataset.getId());
            }
            Long maxSize = systemConfig.getMaxFileUploadSizeForStore(dataset.getEffectiveStorageDriverId());
            if (maxSize != null) {
                if(fileSize > maxSize) {
                    return error(Response.Status.BAD_REQUEST,
                            "The file you are trying to upload is too large to be uploaded to this dataset. " +
                                    "The maximum allowed file size is " + maxSize + " bytes.");
                }
            }
            UploadSessionQuotaLimit limit = fileService.getUploadSessionQuotaLimit(dataset);
            if (limit != null) {
                if(fileSize > limit.getRemainingQuotaInBytes()) {
                    return error(Response.Status.BAD_REQUEST,
                            "The file you are trying to upload is too large to be uploaded to this dataset. " +
                                    "The remaing file size quota is " + limit.getRemainingQuotaInBytes() + " bytes.");
                }
            }
            JsonObjectBuilder response = null;
            String storageIdentifier = null;
            try {
                storageIdentifier = FileUtil.getStorageIdentifierFromLocation(s3io.getStorageLocation());
                response = s3io.generateTemporaryS3UploadUrls(dataset.getGlobalId().asString(), storageIdentifier, fileSize);

            } catch (IOException io) {
                logger.warning(io.getMessage());
                throw new WrappedResponse(io,
                        error(Response.Status.INTERNAL_SERVER_ERROR, "Could not create process direct upload request"));
            }

            response.add("storageIdentifier", storageIdentifier);
            return ok(response);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("mpupload")
    public Response abortMPUpload(@Context ContainerRequestContext crc, @QueryParam("globalid") String idSupplied, @QueryParam("storageidentifier") String storageidentifier, @QueryParam("uploadid") String uploadId) {
        try {
            Dataset dataset = datasetSvc.findByGlobalId(idSupplied);
            //Allow the API to be used within a session (e.g. for direct upload in the UI)
            User user = session.getUser();
            if (!user.isAuthenticated()) {
                try {
                    user = getRequestAuthenticatedUserOrDie(crc);
                } catch (WrappedResponse ex) {
                    logger.info(
                            "Exception thrown while trying to figure out permissions while getting aborting upload for dataset id "
                                    + dataset.getId() + ": " + ex.getLocalizedMessage());
                    throw ex;
                }
            }
            boolean allowed = false;
            if (dataset != null) {
                allowed = permissionSvc.requestOn(createDataverseRequest(user), dataset)
                        .canIssue(UpdateDatasetVersionCommand.class);
            } else {
                /*
                 * The only legitimate case where a global id won't correspond to a dataset is
                 * for uploads during creation. Given that this call will still fail unless all
                 * three parameters correspond to an active multipart upload, it should be safe
                 * to allow the attempt for an authenticated user. If there are concerns about
                 * permissions, one could check with the current design that the user is allowed
                 * to create datasets in some dataverse that is configured to use the storage
                 * provider specified in the storageidentifier, but testing for the ability to
                 * create a dataset in a specific dataverse would requiring changing the design
                 * somehow (e.g. adding the ownerId to this call).
                 */
                allowed = true;
            }
            if (!allowed) {
                return error(Response.Status.FORBIDDEN,
                        "You are not permitted to abort file uploads with the supplied parameters.");
            }
            try {
                S3AccessIO.abortMultipartUpload(idSupplied, storageidentifier, uploadId);
            } catch (IOException io) {
                logger.warning("Multipart upload abort failed for uploadId: " + uploadId + " storageidentifier="
                        + storageidentifier + " dataset Id: " + dataset.getId());
                logger.warning(io.getMessage());
                throw new WrappedResponse(io,
                        error(Response.Status.INTERNAL_SERVER_ERROR, "Could not abort multipart upload"));
            }
            return Response.noContent().build();
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("mpupload")
    public Response completeMPUpload(@Context ContainerRequestContext crc, String partETagBody, @QueryParam("globalid") String idSupplied, @QueryParam("storageidentifier") String storageidentifier, @QueryParam("uploadid") String uploadId) {
        try {
            Dataset dataset = datasetSvc.findByGlobalId(idSupplied);
            //Allow the API to be used within a session (e.g. for direct upload in the UI)
            User user = session.getUser();
            if (!user.isAuthenticated()) {
                try {
                    user = getRequestAuthenticatedUserOrDie(crc);
                } catch (WrappedResponse ex) {
                    logger.info(
                            "Exception thrown while trying to figure out permissions to complete mpupload for dataset id "
                                    + dataset.getId() + ": " + ex.getLocalizedMessage());
                    throw ex;
                }
            }
            boolean allowed = false;
            if (dataset != null) {
                allowed = permissionSvc.requestOn(createDataverseRequest(user), dataset)
                        .canIssue(UpdateDatasetVersionCommand.class);
            } else {
                /*
                 * The only legitimate case where a global id won't correspond to a dataset is
                 * for uploads during creation. Given that this call will still fail unless all
                 * three parameters correspond to an active multipart upload, it should be safe
                 * to allow the attempt for an authenticated user. If there are concerns about
                 * permissions, one could check with the current design that the user is allowed
                 * to create datasets in some dataverse that is configured to use the storage
                 * provider specified in the storageidentifier, but testing for the ability to
                 * create a dataset in a specific dataverse would requiring changing the design
                 * somehow (e.g. adding the ownerId to this call).
                 */
                allowed = true;
            }
            if (!allowed) {
                return error(Response.Status.FORBIDDEN,
                        "You are not permitted to complete file uploads with the supplied parameters.");
            }
            List<PartETag> eTagList = new ArrayList<PartETag>();
            logger.info("Etags: " + partETagBody);
            try {
                JsonObject object = JsonUtil.getJsonObject(partETagBody);
                for (String partNo : object.keySet()) {
                    eTagList.add(new PartETag(Integer.parseInt(partNo), object.getString(partNo)));
                }
                for (PartETag et : eTagList) {
                    logger.info("Part: " + et.getPartNumber() + " : " + et.getETag());
                }
            } catch (JsonException je) {
                logger.info("Unable to parse eTags from: " + partETagBody);
                throw new WrappedResponse(je, error(Response.Status.INTERNAL_SERVER_ERROR, "Could not complete multipart upload"));
            }
            try {
                S3AccessIO.completeMultipartUpload(idSupplied, storageidentifier, uploadId, eTagList);
            } catch (IOException io) {
                logger.warning("Multipart upload completion failed for uploadId: " + uploadId + " storageidentifier=" + storageidentifier + " globalId: " + idSupplied);
                logger.warning(io.getMessage());
                try {
                    S3AccessIO.abortMultipartUpload(idSupplied, storageidentifier, uploadId);
                } catch (IOException e) {
                    logger.severe("Also unable to abort the upload (and release the space on S3 for uploadId: " + uploadId + " storageidentifier=" + storageidentifier + " globalId: " + idSupplied);
                    logger.severe(io.getMessage());
                }

                throw new WrappedResponse(io, error(Response.Status.INTERNAL_SERVER_ERROR, "Could not complete multipart upload"));
            }
            return ok("Multipart Upload completed");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    /**
     * Add a File to an existing Dataset
     *
     * @param idSupplied
     * @param jsonData
     * @param fileInputStream
     * @param contentDispositionHeader
     * @param formDataBodyPart
     * @return
     */
    @POST
    @AuthRequired
    @Path("{id}/add")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @Operation(summary = "Uploads a file for a dataset", 
               description = "Uploads a file for a dataset")
    @APIResponse(responseCode = "200",
               description = "File uploaded successfully to dataset")
    @Tag(name = "addFileToDataset", 
         description = "Uploads a file for a dataset")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA))  
    public Response addFileToDataset(@Context ContainerRequestContext crc,
                    @PathParam("id") String idSupplied,
                    @FormDataParam("jsonData") String jsonData,
                    @FormDataParam("file") InputStream fileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                    @FormDataParam("file") final FormDataBodyPart formDataBodyPart
                    ){

        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        User authUser;
        authUser = getRequestUser(crc);

        // -------------------------------------
        // (2) Get the Dataset Id
        //  
        // -------------------------------------
        Dataset dataset;
        
        try {
            dataset = findDatasetOrDie(idSupplied);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        
        //------------------------------------
        // (2a) Make sure dataset does not have package file
        //
        // --------------------------------------
        
        for (DatasetVersion dv : dataset.getVersions()) {
            if (dv.isHasPackageFile()) {
                return error(Response.Status.FORBIDDEN,
                        BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                );
            }
        }

        // (2a) Load up optional params via JSON
        //---------------------------------------
        OptionalFileParams optionalFileParams = null;
        msgt("(api) jsonData: " + jsonData);

        try {
            optionalFileParams = new OptionalFileParams(jsonData);
        } catch (DataFileTagException ex) {
            return error(Response.Status.BAD_REQUEST, ex.getMessage());
        }
        catch (ClassCastException | com.google.gson.JsonParseException ex) {
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("file.addreplace.error.parsing"));
        }
        
        // -------------------------------------
        // (3) Get the file name and content type
        // -------------------------------------
        String newFilename = null;
        String newFileContentType = null;
        String newStorageIdentifier = null;
        if (null == contentDispositionHeader) {
            if (optionalFileParams.hasStorageIdentifier()) {
                newStorageIdentifier = optionalFileParams.getStorageIdentifier();
                newStorageIdentifier = DataAccess.expandStorageIdentifierIfNeeded(newStorageIdentifier);
                
                if(!DataAccess.uploadToDatasetAllowed(dataset,  newStorageIdentifier)) {
                    return error(BAD_REQUEST,
                            "Dataset store configuration does not allow provided storageIdentifier.");
                }
                if (optionalFileParams.hasFileName()) {
                    newFilename = optionalFileParams.getFileName();
                    if (optionalFileParams.hasMimetype()) {
                        newFileContentType = optionalFileParams.getMimeType();
                    }
                }
            } else {
                return error(BAD_REQUEST,
                        "You must upload a file or provide a valid storageidentifier, filename, and mimetype.");
            }
        } else {
            newFilename = contentDispositionHeader.getFileName();
            // Let's see if the form data part has the mime (content) type specified.
            // Note that we don't want to rely on formDataBodyPart.getMediaType() -
            // because that defaults to "text/plain" when no "Content-Type:" header is
            // present. Instead we'll go through the headers, and see if "Content-Type:"
            // is there. If not, we'll default to "application/octet-stream" - the generic
            // unknown type. This will prompt the application to run type detection and
            // potentially find something more accurate.
            // newFileContentType = formDataBodyPart.getMediaType().toString();

            for (String header : formDataBodyPart.getHeaders().keySet()) {
                if (header.equalsIgnoreCase("Content-Type")) {
                    newFileContentType = formDataBodyPart.getHeaders().get(header).get(0);
                }
            }
            if (newFileContentType == null) {
                newFileContentType = FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT;
            }
        }


        //-------------------
        // (3) Create the AddReplaceFileHelper object
        //-------------------
        msg("ADD!");

        DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                ingestService,
                datasetService,
                fileService,
                permissionSvc,
                commandEngine,
                systemConfig);


        //-------------------
        // (4) Run "runAddFileByDatasetId"
        //-------------------
        addFileHelper.runAddFileByDataset(dataset,
                newFilename,
                newFileContentType,
                newStorageIdentifier,
                fileInputStream,
                optionalFileParams);


        if (addFileHelper.hasError()){
            //conflict response status added for 8859
            if (Response.Status.CONFLICT.equals(addFileHelper.getHttpErrorCode())){
                return conflict(addFileHelper.getErrorMessagesAsString("\n"));
            }
            return error(addFileHelper.getHttpErrorCode(), addFileHelper.getErrorMessagesAsString("\n"));
        } else {
            String successMsg = BundleUtil.getStringFromBundle("file.addreplace.success.add");
            try {
                //msgt("as String: " + addFileHelper.getSuccessResult());
                /**
                 * @todo We need a consistent, sane way to communicate a human
                 * readable message to an API client suitable for human
                 * consumption. Imagine if the UI were built in Angular or React
                 * and we want to return a message from the API as-is to the
                 * user. Human readable.
                 */
                logger.fine("successMsg: " + successMsg);
                String duplicateWarning = addFileHelper.getDuplicateFileWarning();
                if (duplicateWarning != null && !duplicateWarning.isEmpty()) {
                    return ok(addFileHelper.getDuplicateFileWarning(), addFileHelper.getSuccessResultAsJsonObjectBuilder());
                } else {
                    return ok(addFileHelper.getSuccessResultAsJsonObjectBuilder());
                }

                //"Look at that!  You added a file! (hey hey, it may have worked)");
            } catch (NoFilesException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
                return error(Response.Status.BAD_REQUEST, "NoFileException!  Serious Error! See administrator!");

            }
        }
        
    } // end: addFileToDataset


    /**
     * Clean storage of a Dataset
     *
     * @param idSupplied
     * @return
     */
    @GET
    @AuthRequired
    @Path("{id}/cleanStorage")
    public Response cleanStorage(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied, @QueryParam("dryrun") Boolean dryrun) {
        // get user and dataset
        User authUser = getRequestUser(crc);

        Dataset dataset;
        try {
            dataset = findDatasetOrDie(idSupplied);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        
        // check permissions
        if (!permissionSvc.permissionsFor(createDataverseRequest(authUser), dataset).contains(Permission.EditDataset)) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Access denied!");
        }

        boolean doDryRun = dryrun != null && dryrun.booleanValue();

        // check if no legacy files are present
        Set<String> datasetFilenames = getDatasetFilenames(dataset);
        if (datasetFilenames.stream().anyMatch(x -> !dataFilePattern.matcher(x).matches())) {
            logger.log(Level.WARNING, "Dataset contains legacy files not matching the naming pattern!");
        }

        Predicate<String> filter = getToDeleteFilesFilter(datasetFilenames);
        List<String> deleted;
        try {
            StorageIO<DvObject> datasetIO = DataAccess.getStorageIO(dataset);
            deleted = datasetIO.cleanUp(filter, doDryRun);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "IOException! Serious Error! See administrator!");
        }

        return ok("Found: " + datasetFilenames.stream().collect(Collectors.joining(", ")) + "\n" + "Deleted: " + deleted.stream().collect(Collectors.joining(", ")));
        
    }

    private static Set<String> getDatasetFilenames(Dataset dataset) {
        Set<String> files = new HashSet<>();
        for (DataFile dataFile: dataset.getFiles()) {
            String storageIdentifier = dataFile.getStorageIdentifier();
            String location = storageIdentifier.substring(storageIdentifier.indexOf("://") + 3);
            String[] locationParts = location.split(":");//separate bucket, swift container, etc. from fileName
            files.add(locationParts[locationParts.length-1]);
        }
        return files;
    }

    public static Predicate<String> getToDeleteFilesFilter(Set<String> datasetFilenames) {
        return f -> {
            return dataFilePattern.matcher(f).matches() && datasetFilenames.stream().noneMatch(x -> f.startsWith(x));
        };
    }

    private void msg(String m) {
        //System.out.println(m);
        logger.fine(m);
    }

    private void dashes() {
        msg("----------------");
    }

    private void msgt(String m) {
        dashes();
        msg(m);
        dashes();
    }


    public static <T> T handleVersion(String versionId, DsVersionHandler<T> hdl)
            throws WrappedResponse {
        switch (versionId) {
            case DS_VERSION_LATEST:
                return hdl.handleLatest();
            case DS_VERSION_DRAFT:
                return hdl.handleDraft();
            case DS_VERSION_LATEST_PUBLISHED:
                return hdl.handleLatestPublished();
            default:
                try {
                    String[] versions = versionId.split("\\.");
                    switch (versions.length) {
                        case 1:
                            return hdl.handleSpecific(Long.parseLong(versions[0]), (long) 0.0);
                        case 2:
                            return hdl.handleSpecific(Long.parseLong(versions[0]), Long.parseLong(versions[1]));
                        default:
                            throw new WrappedResponse(error(Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'"));
                    }
                } catch (NumberFormatException nfe) {
                    throw new WrappedResponse(error(Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'"));
                }
        }
    }

    /*
     * includeDeaccessioned default to false and checkPermsWhenDeaccessioned to false. Use it only when you are sure that the you don't need to work with
     * a deaccessioned dataset.
     */
    private DatasetVersion getDatasetVersionOrDie(final DataverseRequest req, 
                                                  String versionNumber, 
                                                  final Dataset ds,
                                                  UriInfo uriInfo, 
                                                  HttpHeaders headers) throws WrappedResponse {
        //The checkPerms was added to check the permissions ONLY when the dataset is deaccessioned.
        boolean checkFilePerms = false;
        boolean includeDeaccessioned = false;
        return getDatasetVersionOrDie(req, versionNumber, ds, uriInfo, headers, includeDeaccessioned, checkFilePerms);
    }
    
    /*
     * checkPermsWhenDeaccessioned default to true. Be aware that the version will be only be obtainable if the user has edit permissions.
     */
    private DatasetVersion getDatasetVersionOrDie(final DataverseRequest req, String versionNumber, final Dataset ds,
            UriInfo uriInfo, HttpHeaders headers, boolean includeDeaccessioned) throws WrappedResponse {
        boolean checkPermsWhenDeaccessioned = true;
        boolean bypassAccessCheck = false;
        return getDatasetVersionOrDie(req, versionNumber, ds, uriInfo, headers, includeDeaccessioned, checkPermsWhenDeaccessioned, bypassAccessCheck);
    }

    /*
     * checkPermsWhenDeaccessioned default to true. Be aware that the version will be only be obtainable if the user has edit permissions.
     */
    private DatasetVersion getDatasetVersionOrDie(final DataverseRequest req, String versionNumber, final Dataset ds,
                                                  UriInfo uriInfo, HttpHeaders headers, boolean includeDeaccessioned, boolean checkPermsWhenDeaccessioned) throws WrappedResponse {
        boolean bypassAccessCheck = false;
        return getDatasetVersionOrDie(req, versionNumber, ds, uriInfo, headers, includeDeaccessioned, checkPermsWhenDeaccessioned, bypassAccessCheck);
    }

    /*
     * Will allow to define when the permissions should be checked when a deaccesioned dataset is requested. If the user doesn't have edit permissions will result in an error.
     */
    private DatasetVersion getDatasetVersionOrDie(final DataverseRequest req, String versionNumber, final Dataset ds,
            UriInfo uriInfo, HttpHeaders headers, boolean includeDeaccessioned, boolean checkPermsWhenDeaccessioned,
            boolean bypassAccessCheck)
            throws WrappedResponse {

        DatasetVersion dsv = findDatasetVersionOrDie(req, versionNumber, ds, includeDeaccessioned, checkPermsWhenDeaccessioned);

        if (dsv == null || dsv.getId() == null) {
            throw new WrappedResponse(
                    notFound("Dataset version " + versionNumber + " of dataset " + ds.getId() + " not found"));
        }
        if (dsv.isReleased()&& uriInfo!=null) {
            MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountEntry(uriInfo, headers, dvRequestService, ds);
            mdcLogService.logEntry(entry);
        }
        return dsv;
    }
 
    @GET
    @Path("{identifier}/locks")
    public Response getLocksForDataset(@PathParam("identifier") String id, @QueryParam("type") DatasetLock.Reason lockType) {

        Dataset dataset = null;
        try {
            dataset = findDatasetOrDie(id);
            Set<DatasetLock> locks;
            if (lockType == null) {
                locks = dataset.getLocks();
            } else {
                // request for a specific type lock:
                DatasetLock lock = dataset.getLockFor(lockType);

                locks = new HashSet<>();
                if (lock != null) {
                    locks.add(lock);
                }
            }
            
            return ok(locks.stream().map(lock -> json(lock)).collect(toJsonArray()));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/locks")
    public Response deleteLocks(@Context ContainerRequestContext crc, @PathParam("identifier") String id, @QueryParam("type") DatasetLock.Reason lockType) {

        return response(req -> {
            try {
                AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
                if (!user.isSuperuser()) {
                    return error(Response.Status.FORBIDDEN, "This API end point can be used by superusers only.");
                }
                Dataset dataset = findDatasetOrDie(id);
                
                if (lockType == null) {
                    Set<DatasetLock.Reason> locks = new HashSet<>();
                    for (DatasetLock lock : dataset.getLocks()) {
                        locks.add(lock.getReason());
                    }
                    if (!locks.isEmpty()) {
                        for (DatasetLock.Reason locktype : locks) {
                            execCommand(new RemoveLockCommand(req, dataset, locktype));
                            // refresh the dataset:
                            dataset = findDatasetOrDie(id);
                        }
                        // kick of dataset reindexing, in case the locks removed 
                        // affected the search card:
                        indexService.asyncIndexDataset(dataset, true);
                        return ok("locks removed");
                    }
                    return ok("dataset not locked");
                }
                // request for a specific type lock:
                DatasetLock lock = dataset.getLockFor(lockType);
                if (lock != null) {
                    execCommand(new RemoveLockCommand(req, dataset, lock.getReason()));
                    // refresh the dataset:
                    dataset = findDatasetOrDie(id);
                    // ... and kick of dataset reindexing, in case the lock removed 
                    // affected the search card:
                    indexService.asyncIndexDataset(dataset, true);
                    return ok("lock type " + lock.getReason() + " removed");
                }
                return ok("no lock type " + lockType + " on the dataset");
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }

        }, getRequestUser(crc));

    }
    
    @POST
    @AuthRequired
    @Path("{identifier}/lock/{type}")
    public Response lockDataset(@Context ContainerRequestContext crc, @PathParam("identifier") String id, @PathParam("type") DatasetLock.Reason lockType) {
        return response(req -> {
            try {
                AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
                if (!user.isSuperuser()) {
                    return error(Response.Status.FORBIDDEN, "This API end point can be used by superusers only.");
                }
                Dataset dataset = findDatasetOrDie(id);
                DatasetLock lock = dataset.getLockFor(lockType);
                if (lock != null) {
                    return error(Response.Status.FORBIDDEN, "dataset already locked with lock type " + lockType);
                }
                lock = new DatasetLock(lockType, user);
                execCommand(new AddLockCommand(req, dataset, lock));
                // refresh the dataset:
                dataset = findDatasetOrDie(id);
                // ... and kick of dataset reindexing:
                indexService.asyncIndexDataset(dataset, true);

                return ok("dataset locked with lock type " + lockType);
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }

        }, getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("locks")
    public Response listLocks(@Context ContainerRequestContext crc, @QueryParam("type") String lockType, @QueryParam("userIdentifier") String userIdentifier) { //DatasetLock.Reason lockType) {
        // This API is here, under /datasets, and not under /admin, because we
        // likely want it to be accessible to admin users who may not necessarily 
        // have localhost access, that would be required to get to /api/admin in 
        // most installations. It is still reasonable however to limit access to
        // this api to admin users only.
        AuthenticatedUser apiUser;
        try {
            apiUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.UNAUTHORIZED, "Authentication is required.");
        }
        if (!apiUser.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        
        // Locks can be optinally filtered by type, user or both.
        DatasetLock.Reason lockTypeValue = null;
        AuthenticatedUser user = null; 
        
        // For the lock type, we use a QueryParam of type String, instead of 
        // DatasetLock.Reason; that would be less code to write, but this way 
        // we can check if the value passed matches a valid lock type ("reason") 
        // and provide a helpful error message if it doesn't. If you use a 
        // QueryParam of an Enum type, trying to pass an invalid value to it 
        // results in a potentially confusing "404/NOT FOUND - requested 
        // resource is not available".
        if (lockType != null && !lockType.isEmpty()) {
            try {
                lockTypeValue = DatasetLock.Reason.valueOf(lockType);
            } catch (IllegalArgumentException iax) {
                StringJoiner reasonJoiner = new StringJoiner(", ");
                for (Reason r: Reason.values()) {
                    reasonJoiner.add(r.name());
                };
                String errorMessage = "Invalid lock type value: " + lockType + 
                        "; valid lock types: " + reasonJoiner.toString();
                return error(Response.Status.BAD_REQUEST, errorMessage);
            }
        }
        
        if (userIdentifier != null && !userIdentifier.isEmpty()) {
            user = authSvc.getAuthenticatedUser(userIdentifier);
            if (user == null) {
                return error(Response.Status.BAD_REQUEST, "Unknown user identifier: "+userIdentifier);
            }
        }
        
        //List<DatasetLock> locks = datasetService.getDatasetLocksByType(lockType);
        List<DatasetLock> locks = datasetService.listLocks(lockTypeValue, user);
                            
        return ok(locks.stream().map(lock -> json(lock)).collect(toJsonArray()));
    }   
    
    
    @GET
    @Path("{id}/makeDataCount/citations")
    public Response getMakeDataCountCitations(@PathParam("id") String idSupplied) {
        
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            JsonArrayBuilder datasetsCitations = Json.createArrayBuilder();
            List<DatasetExternalCitations> externalCitations = datasetExternalCitationsService.getDatasetExternalCitationsByDataset(dataset);
            for (DatasetExternalCitations citation : externalCitations) {
                JsonObjectBuilder candidateObj = Json.createObjectBuilder();
                /**
                 * In the future we can imagine storing and presenting more
                 * information about the citation such as the title of the paper
                 * and the names of the authors. For now, we'll at least give
                 * the URL of the citation so people can click and find out more
                 * about the citation.
                 */
                candidateObj.add("citationUrl", citation.getCitedByUrl());
                datasetsCitations.add(candidateObj);
            }
            return ok(datasetsCitations);

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }

    @GET
    @Path("{id}/makeDataCount/{metric}")
    public Response getMakeDataCountMetricCurrentMonth(@PathParam("id") String idSupplied, @PathParam("metric") String metricSupplied, @QueryParam("country") String country) {
        String nullCurrentMonth = null;
        return getMakeDataCountMetric(idSupplied, metricSupplied, nullCurrentMonth, country);
    }

    @GET
    @AuthRequired
    @Path("{identifier}/storagesize")
    public Response getStorageSize(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, @QueryParam("includeCached") boolean includeCached) {
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasize.storage"),
                execCommand(new GetDatasetStorageSizeCommand(req, findDatasetOrDie(dvIdtf), includeCached, GetDatasetStorageSizeCommand.Mode.STORAGE, null)))), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/versions/{versionId}/downloadsize")
    public Response getDownloadSize(@Context ContainerRequestContext crc,
                                    @PathParam("identifier") String dvIdtf,
                                    @PathParam("versionId") String version,
                                    @QueryParam("contentType") String contentType,
                                    @QueryParam("accessStatus") String accessStatus,
                                    @QueryParam("categoryName") String categoryName,
                                    @QueryParam("tabularTagName") String tabularTagName,
                                    @QueryParam("searchText") String searchText,
                                    @QueryParam("mode") String mode,
                                    @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers) {

        return response(req -> {
            FileSearchCriteria fileSearchCriteria;
            try {
                fileSearchCriteria = new FileSearchCriteria(
                        contentType,
                        accessStatus != null ? FileSearchCriteria.FileAccessStatus.valueOf(accessStatus) : null,
                        categoryName,
                        tabularTagName,
                        searchText
                );
            } catch (IllegalArgumentException e) {
                return badRequest(BundleUtil.getStringFromBundle("datasets.api.version.files.invalid.access.status", List.of(accessStatus)));
            }
            DatasetVersionFilesServiceBean.FileDownloadSizeMode fileDownloadSizeMode;
            try {
                fileDownloadSizeMode = mode != null ? DatasetVersionFilesServiceBean.FileDownloadSizeMode.valueOf(mode) : DatasetVersionFilesServiceBean.FileDownloadSizeMode.All;
            } catch (IllegalArgumentException e) {
                return error(Response.Status.BAD_REQUEST, "Invalid mode: " + mode);
            }
            DatasetVersion datasetVersion = getDatasetVersionOrDie(req, version, findDatasetOrDie(dvIdtf), uriInfo, headers, includeDeaccessioned);
            long datasetStorageSize = datasetVersionFilesServiceBean.getFilesDownloadSize(datasetVersion, fileSearchCriteria, fileDownloadSizeMode);
            String message = MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasize.download"), datasetStorageSize);
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("message", message);
            jsonObjectBuilder.add("storageSize", datasetStorageSize);
            return ok(jsonObjectBuilder);
        }, getRequestUser(crc));
    }

    @GET
    @Path("{id}/makeDataCount/{metric}/{yyyymm}")
    public Response getMakeDataCountMetric(@PathParam("id") String idSupplied, @PathParam("metric") String metricSupplied, @PathParam("yyyymm") String yyyymm, @QueryParam("country") String country) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            NullSafeJsonBuilder jsonObjectBuilder = jsonObjectBuilder();
            MakeDataCountUtil.MetricType metricType = null;
            try {
                metricType = MakeDataCountUtil.MetricType.fromString(metricSupplied);
            } catch (IllegalArgumentException ex) {
                return error(Response.Status.BAD_REQUEST, ex.getMessage());
            }
            String monthYear = null;
            if (yyyymm != null) {
                // We add "-01" because we store "2018-05-01" rather than "2018-05" in the "monthyear" column.
                // Dates come to us as "2018-05-01" in the SUSHI JSON ("begin-date") and we decided to store them as-is.
                monthYear = MetricsUtil.sanitizeYearMonthUserInput(yyyymm) + "-01";
            }
            if (country != null) {
                country = country.toLowerCase();
                if (!MakeDataCountUtil.isValidCountryCode(country)) {
                    return error(Response.Status.BAD_REQUEST, "Country must be one of the ISO 1366 Country Codes");
                }
            }
            DatasetMetrics datasetMetrics = datasetMetricsSvc.getDatasetMetricsByDatasetForDisplay(dataset, monthYear, country);
            if (datasetMetrics == null) {
                return ok("No metrics available for dataset " + dataset.getId() + " for " + yyyymm + " for country code " + country + ".");
            } else if (datasetMetrics.getDownloadsTotal() + datasetMetrics.getViewsTotal() == 0) {
                return ok("No metrics available for dataset " + dataset.getId() + " for " + yyyymm + " for country code " + country + ".");
            }
            Long viewsTotalRegular = null;
            Long viewsUniqueRegular = null;
            Long downloadsTotalRegular = null;
            Long downloadsUniqueRegular = null;
            Long viewsTotalMachine = null;
            Long viewsUniqueMachine = null;
            Long downloadsTotalMachine = null;
            Long downloadsUniqueMachine = null;
            Long viewsTotal = null;
            Long viewsUnique = null;
            Long downloadsTotal = null;
            Long downloadsUnique = null;
            switch (metricSupplied) {
                case "viewsTotal":
                    viewsTotal = datasetMetrics.getViewsTotal();
                    break;
                case "viewsTotalRegular":
                    viewsTotalRegular = datasetMetrics.getViewsTotalRegular();
                    break;
                case "viewsTotalMachine":
                    viewsTotalMachine = datasetMetrics.getViewsTotalMachine();
                    break;
                case "viewsUnique":
                    viewsUnique = datasetMetrics.getViewsUnique();
                    break;
                case "viewsUniqueRegular":
                    viewsUniqueRegular = datasetMetrics.getViewsUniqueRegular();
                    break;
                case "viewsUniqueMachine":
                    viewsUniqueMachine = datasetMetrics.getViewsUniqueMachine();
                    break;
                case "downloadsTotal":
                    downloadsTotal = datasetMetrics.getDownloadsTotal();
                    break;
                case "downloadsTotalRegular":
                    downloadsTotalRegular = datasetMetrics.getDownloadsTotalRegular();
                    break;
                case "downloadsTotalMachine":
                    downloadsTotalMachine = datasetMetrics.getDownloadsTotalMachine();
                    break;
                case "downloadsUnique":
                    downloadsUnique = datasetMetrics.getDownloadsUnique();
                    break;
                case "downloadsUniqueRegular":
                    downloadsUniqueRegular = datasetMetrics.getDownloadsUniqueRegular();
                    break;
                case "downloadsUniqueMachine":
                    downloadsUniqueMachine = datasetMetrics.getDownloadsUniqueMachine();
                    break;
                default:
                    break;
            }
            /**
             * TODO: Think more about the JSON output and the API design.
             * getDatasetMetricsByDatasetMonthCountry returns a single row right
             * now, by country. We could return multiple metrics (viewsTotal,
             * viewsUnique, downloadsTotal, and downloadsUnique) by country.
             */
            jsonObjectBuilder.add("viewsTotalRegular", viewsTotalRegular);
            jsonObjectBuilder.add("viewsUniqueRegular", viewsUniqueRegular);
            jsonObjectBuilder.add("downloadsTotalRegular", downloadsTotalRegular);
            jsonObjectBuilder.add("downloadsUniqueRegular", downloadsUniqueRegular);
            jsonObjectBuilder.add("viewsTotalMachine", viewsTotalMachine);
            jsonObjectBuilder.add("viewsUniqueMachine", viewsUniqueMachine);
            jsonObjectBuilder.add("downloadsTotalMachine", downloadsTotalMachine);
            jsonObjectBuilder.add("downloadsUniqueMachine", downloadsUniqueMachine);
            jsonObjectBuilder.add("viewsTotal", viewsTotal);
            jsonObjectBuilder.add("viewsUnique", viewsUnique);
            jsonObjectBuilder.add("downloadsTotal", downloadsTotal);
            jsonObjectBuilder.add("downloadsUnique", downloadsUnique);
            return ok(jsonObjectBuilder);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        } catch (Exception e) {
            //bad date - caught in sanitize call
            return error(BAD_REQUEST, e.getMessage());
        }
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/storageDriver")
    public Response getFileStore(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse { 
        
        Dataset dataset; 
        
        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        
        return response(req -> ok(dataset.getEffectiveStorageDriverId()), getRequestUser(crc));
    }
    
    @PUT
    @AuthRequired
    @Path("{identifier}/storageDriver")
    public Response setFileStore(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            String storageDriverLabel,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {
        
        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        
        // We don't want to allow setting this to a store id that does not exist: 
        for (Entry<String, String> store : DataAccess.getStorageDriverLabels().entrySet()) {
            if (store.getKey().equals(storageDriverLabel)) {
                dataset.setStorageDriverId(store.getValue());
                datasetService.merge(dataset);
                return ok("Storage driver set to: " + store.getKey() + "/" + store.getValue());
            }
        }
        return error(Response.Status.BAD_REQUEST,
                "No Storage Driver found for : " + storageDriverLabel);
    }
    
    @DELETE
    @AuthRequired
    @Path("{identifier}/storageDriver")
    public Response resetFileStore(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {
        
        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        
        dataset.setStorageDriverId(null);
        datasetService.merge(dataset);
        return ok("Storage reset to default: " + DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
    }

    @GET
    @AuthRequired
    @Path("{identifier}/curationLabelSet")
    public Response getCurationLabelSet(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {

        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        return response(req -> ok(dataset.getEffectiveCurationLabelSetName()), getRequestUser(crc));
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/curationLabelSet")
    public Response setCurationLabelSet(@Context ContainerRequestContext crc,
                                        @PathParam("identifier") String dvIdtf,
                                        @QueryParam("name") String curationLabelSet,
                                        @Context UriInfo uriInfo,
                                        @Context HttpHeaders headers) throws WrappedResponse {

        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.UNAUTHORIZED, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        if (SystemConfig.CURATIONLABELSDISABLED.equals(curationLabelSet) || SystemConfig.DEFAULTCURATIONLABELSET.equals(curationLabelSet)) {
            dataset.setCurationLabelSetName(curationLabelSet);
            datasetService.merge(dataset);
            return ok("Curation Label Set Name set to: " + curationLabelSet);
        } else {
            for (String setName : systemConfig.getCurationLabels().keySet()) {
                if (setName.equals(curationLabelSet)) {
                    dataset.setCurationLabelSetName(curationLabelSet);
                    datasetService.merge(dataset);
                    return ok("Curation Label Set Name set to: " + setName);
                }
            }
        }
        return error(Response.Status.BAD_REQUEST,
            "No Such Curation Label Set");
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/curationLabelSet")
    public Response resetCurationLabelSet(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {

        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

        dataset.setCurationLabelSetName(SystemConfig.DEFAULTCURATIONLABELSET);
        datasetService.merge(dataset);
        return ok("Curation Label Set reset to default: " + SystemConfig.DEFAULTCURATIONLABELSET);
    }

    @GET
    @AuthRequired
    @Path("{identifier}/allowedCurationLabels")
    public Response getAllowedCurationLabels(@Context ContainerRequestContext crc,
                                             @PathParam("identifier") String dvIdtf,
                                             @Context UriInfo uriInfo,
                                             @Context HttpHeaders headers) throws WrappedResponse {
        AuthenticatedUser user = null;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        if (permissionSvc.requestOn(createDataverseRequest(user), dataset).has(Permission.PublishDataset)) {
            String[] labelArray = systemConfig.getCurationLabels().get(dataset.getEffectiveCurationLabelSetName());
            return response(req -> ok(String.join(",", labelArray)), getRequestUser(crc));
        } else {
            return error(Response.Status.FORBIDDEN, "You are not permitted to view the allowed curation labels for this dataset.");
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/timestamps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimestamps(@Context ContainerRequestContext crc, @PathParam("identifier") String id) {

        Dataset dataset = null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            dataset = findDatasetOrDie(id);
            User u = getRequestUser(crc);
            Set<Permission> perms = new HashSet<Permission>();
            perms.add(Permission.ViewUnpublishedDataset);
            boolean canSeeDraft = permissionSvc.hasPermissionsFor(u, dataset, perms);
            JsonObjectBuilder timestamps = Json.createObjectBuilder();
            logger.fine("CSD: " + canSeeDraft);
            logger.fine("IT: " + dataset.getIndexTime());
            logger.fine("MT: " + dataset.getModificationTime());
            logger.fine("PIT: " + dataset.getPermissionIndexTime());
            logger.fine("PMT: " + dataset.getPermissionModificationTime());
            // Basic info if it's released
            if (dataset.isReleased() || canSeeDraft) {
                timestamps.add("createTime", formatter.format(dataset.getCreateDate().toLocalDateTime()));
                if (dataset.getPublicationDate() != null) {
                    timestamps.add("publicationTime", formatter.format(dataset.getPublicationDate().toLocalDateTime()));
                }

                if (dataset.getLastExportTime() != null) {
                    timestamps.add("lastMetadataExportTime",
                            formatter.format(dataset.getLastExportTime().toInstant().atZone(ZoneId.systemDefault())));

                }

                if (dataset.getMostRecentMajorVersionReleaseDate() != null) {
                    timestamps.add("lastMajorVersionReleaseTime", formatter.format(
                            dataset.getMostRecentMajorVersionReleaseDate().toInstant().atZone(ZoneId.systemDefault())));
                }
                // If the modification/permissionmodification time is
                // set and the index time is null or is before the mod time, the relevant index is stale
                timestamps.add("hasStaleIndex",
                        (dataset.getModificationTime() != null && (dataset.getIndexTime() == null
                                || (dataset.getIndexTime().compareTo(dataset.getModificationTime()) <= 0))) ? true
                                : false);
                timestamps.add("hasStalePermissionIndex",
                        (dataset.getPermissionModificationTime() != null && (dataset.getIndexTime() == null
                                || (dataset.getIndexTime().compareTo(dataset.getModificationTime()) <= 0))) ? true
                                : false);
            }
            // More detail if you can see a draft
            if (canSeeDraft) {
                timestamps.add("lastUpdateTime", formatter.format(dataset.getModificationTime().toLocalDateTime()));
                if (dataset.getIndexTime() != null) {
                    timestamps.add("lastIndexTime", formatter.format(dataset.getIndexTime().toLocalDateTime()));
                }
                if (dataset.getPermissionModificationTime() != null) {
                    timestamps.add("lastPermissionUpdateTime",
                            formatter.format(dataset.getPermissionModificationTime().toLocalDateTime()));
                }
                if (dataset.getPermissionIndexTime() != null) {
                    timestamps.add("lastPermissionIndexTime",
                            formatter.format(dataset.getPermissionIndexTime().toLocalDateTime()));
                }
                if (dataset.getGlobalIdCreateTime() != null) {
                    timestamps.add("globalIdCreateTime", formatter
                            .format(dataset.getGlobalIdCreateTime().toInstant().atZone(ZoneId.systemDefault())));
                }

            }
            return ok(timestamps);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }


/****************************
 * Globus Support Section:
 * 
 * Globus transfer in (upload) and out (download) involve three basic steps: The
 * app is launched and makes a callback to the
 * globusUploadParameters/globusDownloadParameters method to get all of the info
 * needed to set up it's display.
 * 
 * At some point after that, the user will make a selection as to which files to
 * transfer and the app will call requestGlobusUploadPaths/requestGlobusDownload
 * to indicate a transfer is about to start. In addition to providing the
 * details of where to transfer the files to/from, Dataverse also grants the
 * Globus principal involved the relevant rw or r permission for the dataset.
 * 
 * Once the transfer is started, the app records the task id and sends it to
 * Dataverse in the addGlobusFiles/monitorGlobusDownload call. Dataverse then
 * monitors the transfer task and when it ultimately succeeds for fails it
 * revokes the principal's permission and, for the transfer in case, adds the
 * files to the dataset. (The dataset is locked until the transfer completes.)
 * 
 * (If no transfer is started within a specified timeout, permissions will
 * automatically be revoked - see the GlobusServiceBean for details.)
 *
 * The option to reference a file at a remote endpoint (rather than transfer it)
 * follows the first two steps of the process above but completes with a call to
 * the normal /addFiles endpoint (as there is no transfer to monitor and the
 * files can be added to the dataset immediately.)
 */

    /**
     * Retrieve the parameters and signed URLs required to perform a globus
     * transfer. This api endpoint is expected to be called as a signed callback
     * after the globus-dataverse app/other app is launched, but it will accept
     * other forms of authentication.
     * 
     * @param crc
     * @param datasetId
     */
    @GET
    @AuthRequired
    @Path("{id}/globusUploadParameters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobusUploadParams(@Context ContainerRequestContext crc, @PathParam("id") String datasetId,
            @QueryParam(value = "locale") String locale) {
        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        AuthenticatedUser authUser;
        try {
            authUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        String storeId = dataset.getEffectiveStorageDriverId();
        // acceptsGlobusTransfers should only be true for an S3 or globus store
        if (!GlobusAccessibleStore.acceptsGlobusTransfers(storeId)
                && !GlobusAccessibleStore.allowsGlobusReferences(storeId)) {
            return badRequest(BundleUtil.getStringFromBundle("datasets.api.globusuploaddisabled"));
        }

        URLTokenUtil tokenUtil = new URLTokenUtil(dataset, authSvc.findApiTokenByUser(authUser), locale);

        boolean managed = GlobusAccessibleStore.isDataverseManaged(storeId);
        String transferEndpoint = null;
        JsonArray referenceEndpointsWithPaths = null;
        if (managed) {
            transferEndpoint = GlobusAccessibleStore.getTransferEndpointId(storeId);
        } else {
            referenceEndpointsWithPaths = GlobusAccessibleStore.getReferenceEndpointsWithPaths(storeId);
        }

        JsonObjectBuilder queryParams = Json.createObjectBuilder();
        queryParams.add("queryParameters",
                Json.createArrayBuilder().add(Json.createObjectBuilder().add("datasetId", "{datasetId}"))
                        .add(Json.createObjectBuilder().add("siteUrl", "{siteUrl}"))
                        .add(Json.createObjectBuilder().add("datasetVersion", "{datasetVersion}"))
                        .add(Json.createObjectBuilder().add("dvLocale", "{localeCode}"))
                        .add(Json.createObjectBuilder().add("datasetPid", "{datasetPid}")));
        JsonObject substitutedParams = tokenUtil.getParams(queryParams.build());
        JsonObjectBuilder params = Json.createObjectBuilder();
        substitutedParams.keySet().forEach((key) -> {
            params.add(key, substitutedParams.get(key));
        });
        params.add("managed", Boolean.toString(managed));
        if (managed) {
            Long maxSize = systemConfig.getMaxFileUploadSizeForStore(storeId);
            if (maxSize != null) {
                params.add("fileSizeLimit", maxSize);
            }
            UploadSessionQuotaLimit limit = fileService.getUploadSessionQuotaLimit(dataset);
            if (limit != null) {
                params.add("remainingQuota", limit.getRemainingQuotaInBytes());
            }
        }
        if (transferEndpoint != null) {
            params.add("endpoint", transferEndpoint);
        } else {
            params.add("referenceEndpointsWithPaths", referenceEndpointsWithPaths);
        }
        int timeoutSeconds = JvmSettings.GLOBUS_CACHE_MAXAGE.lookup(Integer.class);
        JsonArrayBuilder allowedApiCalls = Json.createArrayBuilder();
        String requestCallName = managed ? "requestGlobusTransferPaths" : "requestGlobusReferencePaths";
        allowedApiCalls.add(
                Json.createObjectBuilder().add(URLTokenUtil.NAME, requestCallName).add(URLTokenUtil.HTTP_METHOD, "POST")
                        .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/requestGlobusUploadPaths")
                        .add(URLTokenUtil.TIMEOUT, timeoutSeconds));
        if(managed) {
        allowedApiCalls.add(Json.createObjectBuilder().add(URLTokenUtil.NAME, "addGlobusFiles")
                .add(URLTokenUtil.HTTP_METHOD, "POST")
                .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/addGlobusFiles")
                .add(URLTokenUtil.TIMEOUT, timeoutSeconds));
        } else {
            allowedApiCalls.add(Json.createObjectBuilder().add(URLTokenUtil.NAME, "addFiles")
                    .add(URLTokenUtil.HTTP_METHOD, "POST")
                    .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/addFiles")
                    .add(URLTokenUtil.TIMEOUT, timeoutSeconds));
        }
        allowedApiCalls.add(Json.createObjectBuilder().add(URLTokenUtil.NAME, "getDatasetMetadata")
                .add(URLTokenUtil.HTTP_METHOD, "GET")
                .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/versions/{datasetVersion}")
                .add(URLTokenUtil.TIMEOUT, 5));
        allowedApiCalls.add(
                Json.createObjectBuilder().add(URLTokenUtil.NAME, "getFileListing").add(URLTokenUtil.HTTP_METHOD, "GET")
                        .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/versions/{datasetVersion}/files")
                        .add(URLTokenUtil.TIMEOUT, 5));

        return ok(tokenUtil.createPostBody(params.build(), allowedApiCalls.build()));
    }

    /**
     * Provides specific storageIdentifiers to use for each file amd requests permissions for a given globus user to upload to the dataset
     * 
     * @param crc
     * @param datasetId
     * @param jsonData - an object that must include the id of the globus "principal" involved and the "numberOfFiles" that will be transferred.
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @POST
    @AuthRequired
    @Path("{id}/requestGlobusUploadPaths")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestGlobusUpload(@Context ContainerRequestContext crc, @PathParam("id") String datasetId,
            String jsonBody) throws IOException, ExecutionException, InterruptedException {

        logger.info(" ====  (api allowGlobusUpload) jsonBody   ====== " + jsonBody);

        if (!systemConfig.isGlobusUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE,
                    BundleUtil.getStringFromBundle("file.api.globusUploadDisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        AuthenticatedUser authUser;
        try {
            authUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse e) {
            return e.getResponse();
        }

        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (permissionSvc.requestOn(createDataverseRequest(authUser), dataset)
                .canIssue(UpdateDatasetVersionCommand.class)) {

            JsonObject params = JsonUtil.getJsonObject(jsonBody);
            if (!GlobusAccessibleStore.isDataverseManaged(dataset.getEffectiveStorageDriverId())) {
                try {
                    JsonArray referencedFiles = params.getJsonArray("referencedFiles");
                    if (referencedFiles == null || referencedFiles.size() == 0) {
                        return badRequest("No referencedFiles specified");
                    }
                    JsonObject fileMap = globusService.requestReferenceFileIdentifiers(dataset, referencedFiles);
                    return (ok(fileMap));
                } catch (Exception e) {
                    return badRequest(e.getLocalizedMessage());
                }
            } else {
                try {
                    String principal = params.getString("principal");
                    int numberOfPaths = params.getInt("numberOfFiles");
                    if (numberOfPaths <= 0) {
                        return badRequest("numberOfFiles must be positive");
                    }

                    JsonObject response = globusService.requestAccessiblePaths(principal, dataset, numberOfPaths);
                    switch (response.getInt("status")) {
                    case 201:
                        return ok(response.getJsonObject("paths"));
                    case 400:
                        return badRequest("Unable to grant permission");
                    case 409:
                        return conflict("Permission already exists");
                    default:
                        return error(null, "Unexpected error when granting permission");
                    }

                } catch (NullPointerException | ClassCastException e) {
                    return badRequest("Error retrieving principal and numberOfFiles from JSON request body");

                }
            }
        } else {
            return forbidden("User doesn't have permission to upload to this dataset");
        }

    }

    /** A method analogous to /addFiles that must also include the taskIdentifier of the transfer-in-progress to monitor
     * 
     * @param crc
     * @param datasetId
     * @param jsonData - see /addFiles documentation, aditional "taskIdentifier" key in the main object is required.
     * @param uriInfo
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @POST
    @AuthRequired
    @Path("{id}/addGlobusFiles")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @Operation(summary = "Uploads a Globus file for a dataset", 
               description = "Uploads a Globus file for a dataset")
    @APIResponse(responseCode = "200",
               description = "Globus file uploaded successfully to dataset")
    @Tag(name = "addGlobusFilesToDataset", 
         description = "Uploads a Globus file for a dataset")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA))  
    public Response addGlobusFilesToDataset(@Context ContainerRequestContext crc,
                                            @PathParam("id") String datasetId,
                                            @FormDataParam("jsonData") String jsonData,
                                            @Context UriInfo uriInfo
    ) throws IOException, ExecutionException, InterruptedException {

        logger.info(" ====  (api addGlobusFilesToDataset) jsonData   ====== " + jsonData);

        // -------------------------------------
        // (1) Get the user from the API key
        // -------------------------------------
        AuthenticatedUser authUser;
        try {
            authUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("file.addreplace.error.auth")
            );
        }

        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        
        // Is Globus upload service available? 
        
        // ... on this Dataverse instance?
        if (!systemConfig.isGlobusUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.globusUploadDisabled"));
        }

        // ... and on this specific Dataset? 
        String storeId = dataset.getEffectiveStorageDriverId();
        // acceptsGlobusTransfers should only be true for an S3 or globus store
        if (!GlobusAccessibleStore.acceptsGlobusTransfers(storeId)
                && !GlobusAccessibleStore.allowsGlobusReferences(storeId)) {
            return badRequest(BundleUtil.getStringFromBundle("datasets.api.globusuploaddisabled"));
        }
        
        // Check if the dataset is already locked
        // We are reusing the code and logic used by various command to determine 
        // if there are any locks on the dataset that would prevent the current 
        // users from modifying it:
        try {
            DataverseRequest dataverseRequest = createDataverseRequest(authUser);
            permissionService.checkEditDatasetLock(dataset, dataverseRequest, null); 
        } catch (IllegalCommandException icex) {
            return error(Response.Status.FORBIDDEN, "Dataset " + datasetId + " is locked: " + icex.getLocalizedMessage());
        }
        
        JsonObject jsonObject = null;
        try {
            jsonObject = JsonUtil.getJsonObject(jsonData);
        } catch (Exception ex) {
            logger.fine("Error parsing json: " + jsonData + " " + ex.getMessage());
            return badRequest("Error parsing json body");

        }

        //------------------------------------
        // (2b) Make sure dataset does not have package file
        // --------------------------------------

        for (DatasetVersion dv : dataset.getVersions()) {
            if (dv.isHasPackageFile()) {
                return error(Response.Status.FORBIDDEN, BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                );
            }
        }


        String lockInfoMessage = "Globus Upload API started ";
        DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.GlobusUpload,
                (authUser).getId(), lockInfoMessage);
        if (lock != null) {
            dataset.addLock(lock);
        } else {
            logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
        }

        if(uriInfo != null) {
            logger.info(" ====  (api uriInfo.getRequestUri()) jsonData   ====== " + uriInfo.getRequestUri().toString());
        }

        String requestUrl = SystemConfig.getDataverseSiteUrlStatic();
        
        // Async Call
        try {
            globusService.globusUpload(jsonObject, dataset, requestUrl, authUser);
        } catch (IllegalArgumentException ex) {
            return badRequest("Invalid parameters: "+ex.getMessage());
        }

        return ok("Async call to Globus Upload started ");

    }
    
/**
 * Retrieve the parameters and signed URLs required to perform a globus
 * transfer/download. This api endpoint is expected to be called as a signed
 * callback after the globus-dataverse app/other app is launched, but it will
 * accept other forms of authentication.
 * 
 * @param crc
 * @param datasetId
 * @param locale
 * @param downloadId - an id to a cached object listing the files involved. This is generated via Dataverse and provided to the dataverse-globus app in a signedURL.
 * @return - JSON containing the parameters and URLs needed by the dataverse-globus app. The format is analogous to that for external tools. 
 */
    @GET
    @AuthRequired
    @Path("{id}/globusDownloadParameters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobusDownloadParams(@Context ContainerRequestContext crc, @PathParam("id") String datasetId,
            @QueryParam(value = "locale") String locale, @QueryParam(value = "downloadId") String downloadId) {
        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        AuthenticatedUser authUser = null;
        try {
            authUser = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse e) {
            logger.fine("guest user globus download");
        }
        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        String storeId = dataset.getEffectiveStorageDriverId();
        // acceptsGlobusTransfers should only be true for an S3 or globus store
        if (!(GlobusAccessibleStore.acceptsGlobusTransfers(storeId)
                || GlobusAccessibleStore.allowsGlobusReferences(storeId))) {
            return badRequest(BundleUtil.getStringFromBundle("datasets.api.globusdownloaddisabled"));
        }

        JsonObject files = globusService.getFilesForDownload(downloadId);
        if (files == null) {
            return notFound(BundleUtil.getStringFromBundle("datasets.api.globusdownloadnotfound"));
        }

        URLTokenUtil tokenUtil = new URLTokenUtil(dataset, authSvc.findApiTokenByUser(authUser), locale);

        boolean managed = GlobusAccessibleStore.isDataverseManaged(storeId);
        String transferEndpoint = null;

        JsonObjectBuilder queryParams = Json.createObjectBuilder();
        queryParams.add("queryParameters",
                Json.createArrayBuilder().add(Json.createObjectBuilder().add("datasetId", "{datasetId}"))
                        .add(Json.createObjectBuilder().add("siteUrl", "{siteUrl}"))
                        .add(Json.createObjectBuilder().add("datasetVersion", "{datasetVersion}"))
                        .add(Json.createObjectBuilder().add("dvLocale", "{localeCode}"))
                        .add(Json.createObjectBuilder().add("datasetPid", "{datasetPid}")));
        JsonObject substitutedParams = tokenUtil.getParams(queryParams.build());
        JsonObjectBuilder params = Json.createObjectBuilder();
        substitutedParams.keySet().forEach((key) -> {
            params.add(key, substitutedParams.get(key));
        });
        params.add("managed", Boolean.toString(managed));
        if (managed) {
            transferEndpoint = GlobusAccessibleStore.getTransferEndpointId(storeId);
            params.add("endpoint", transferEndpoint);
        }
        params.add("files", files);
        int timeoutSeconds = JvmSettings.GLOBUS_CACHE_MAXAGE.lookup(Integer.class);
        JsonArrayBuilder allowedApiCalls = Json.createArrayBuilder();
        allowedApiCalls.add(Json.createObjectBuilder().add(URLTokenUtil.NAME, "monitorGlobusDownload")
                .add(URLTokenUtil.HTTP_METHOD, "POST")
                .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/monitorGlobusDownload")
                .add(URLTokenUtil.TIMEOUT, timeoutSeconds));
        allowedApiCalls.add(Json.createObjectBuilder().add(URLTokenUtil.NAME, "requestGlobusDownload")
                .add(URLTokenUtil.HTTP_METHOD, "POST")
                .add(URLTokenUtil.URL_TEMPLATE,
                        "/api/v1/datasets/{datasetId}/requestGlobusDownload?downloadId=" + downloadId)
                .add(URLTokenUtil.TIMEOUT, timeoutSeconds));
        allowedApiCalls.add(Json.createObjectBuilder().add(URLTokenUtil.NAME, "getDatasetMetadata")
                .add(URLTokenUtil.HTTP_METHOD, "GET")
                .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/versions/{datasetVersion}")
                .add(URLTokenUtil.TIMEOUT, 5));
        allowedApiCalls.add(
                Json.createObjectBuilder().add(URLTokenUtil.NAME, "getFileListing").add(URLTokenUtil.HTTP_METHOD, "GET")
                        .add(URLTokenUtil.URL_TEMPLATE, "/api/v1/datasets/{datasetId}/versions/{datasetVersion}/files")
                        .add(URLTokenUtil.TIMEOUT, 5));

        return ok(tokenUtil.createPostBody(params.build(), allowedApiCalls.build()));
    }

    /**
     * Requests permissions for a given globus user to download the specified files
     * the dataset and returns information about the paths to transfer from.
     * 
     * When called directly rather than in response to being given a downloadId, the jsonData can include a "fileIds" key with an array of file ids to transfer.
     * 
     * @param crc
     * @param datasetId
     * @param jsonData - a JSON object that must include the id of the  Globus "principal" that will be transferring the files in the case where Dataverse manages the Globus endpoint. For remote endpoints, the principal is not required.
     * @return - a JSON object containing a map of file ids to Globus endpoint/path
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @POST
    @AuthRequired
    @Path("{id}/requestGlobusDownload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestGlobusDownload(@Context ContainerRequestContext crc, @PathParam("id") String datasetId,
            @QueryParam(value = "downloadId") String downloadId, String jsonBody)
            throws IOException, ExecutionException, InterruptedException {

        logger.info(" ====  (api allowGlobusDownload) jsonBody   ====== " + jsonBody);

        if (!systemConfig.isGlobusDownload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE,
                    BundleUtil.getStringFromBundle("datasets.api.globusdownloaddisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        User user = getRequestUser(crc);

        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        JsonObject body = null;
        if (jsonBody != null) {
            body = JsonUtil.getJsonObject(jsonBody);
        }
        Set<String> fileIds = null;
        if (downloadId != null) {
            JsonObject files = globusService.getFilesForDownload(downloadId);
            if (files != null) {
                fileIds = files.keySet();
            }
        } else {
            if ((body!=null) && body.containsKey("fileIds")) {
                Collection<JsonValue> fileVals = body.getJsonArray("fileIds").getValuesAs(JsonValue.class);
                fileIds = new HashSet<String>(fileVals.size());
                for (JsonValue fileVal : fileVals) {
                    String id = null;
                    switch (fileVal.getValueType()) {
                    case STRING:
                        id = ((JsonString) fileVal).getString();
                        break;
                    case NUMBER:
                        id = ((JsonNumber) fileVal).toString();
                        break;
                    default:
                        return badRequest("fileIds must be numeric or string (ids/PIDs)");
                    }
                    ;
                    fileIds.add(id);
                }
            } else {
                return badRequest("fileIds JsonArray of file ids/PIDs required in POST body");
            }
        }

        if (fileIds.isEmpty()) {
            return notFound(BundleUtil.getStringFromBundle("datasets.api.globusdownloadnotfound"));
        }
        ArrayList<DataFile> dataFiles = new ArrayList<DataFile>(fileIds.size());
        for (String id : fileIds) {
            boolean published = false;
            logger.info("File id: " + id);

            DataFile df = null;
            try {
                df = findDataFileOrDie(id);
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }
            if (!df.getOwner().equals(dataset)) {
                return badRequest("All files must be in the dataset");
            }
            dataFiles.add(df);

            for (FileMetadata fm : df.getFileMetadatas()) {
                if (fm.getDatasetVersion().isPublished()) {
                    published = true;
                    break;
                }
            }

            if (!published) {
                // If the file is not published, they can still download the file, if the user
                // has the permission to view unpublished versions:

                if (!permissionService.hasPermissionsFor(user, df.getOwner(),
                        EnumSet.of(Permission.ViewUnpublishedDataset))) {
                    return forbidden("User doesn't have permission to download file: " + id);
                }
            } else { // published and restricted and/or embargoed
                if (df.isRestricted() || FileUtil.isActivelyEmbargoed(df))
                    // This line also handles all three authenticated session user, token user, and
                    // guest cases.
                    if (!permissionService.hasPermissionsFor(user, df, EnumSet.of(Permission.DownloadFile))) {
                        return forbidden("User doesn't have permission to download file: " + id);
                    }

            }
        }
        // Allowed to download all requested files
        JsonObject files = GlobusUtil.getFilesMap(dataFiles, dataset);
        if (GlobusAccessibleStore.isDataverseManaged(dataset.getEffectiveStorageDriverId())) {
            // If managed, give the principal read permissions
            int status = globusService.setPermissionForDownload(dataset, body.getString("principal"));
            switch (status) {
            case 201:
                return ok(files);
            case 400:
                return badRequest("Unable to grant permission");
            case 409:
                return conflict("Permission already exists");
            default:
                return error(null, "Unexpected error when granting permission");
            }

        }

        return ok(files);
    }

    /**
     * Monitors a globus download and removes permissions on the dir/dataset when
     * the specified transfer task is completed.
     * 
     * @param crc
     * @param datasetId
     * @param jsonData  - a JSON Object containing the key "taskIdentifier" with the
     *                  Globus task to monitor.
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @POST
    @AuthRequired
    @Path("{id}/monitorGlobusDownload")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response monitorGlobusDownload(@Context ContainerRequestContext crc, @PathParam("id") String datasetId,
            String jsonData) throws IOException, ExecutionException, InterruptedException {

        logger.info(" ====  (api deleteglobusRule) jsonData   ====== " + jsonData);

        if (!systemConfig.isGlobusDownload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE,
                    BundleUtil.getStringFromBundle("datasets.api.globusdownloaddisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        User authUser;
        authUser = getRequestUser(crc);

        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        // Async Call
        globusService.globusDownload(jsonData, dataset, authUser);

        return ok("Async call to Globus Download started");

    }

    /**
     * Add multiple Files to an existing Dataset
     *
     * @param idSupplied
     * @param jsonData
     * @return
     */
    @POST
    @AuthRequired
    @Path("{id}/addFiles")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @Operation(summary = "Uploads a set of files to a dataset", 
               description = "Uploads a set of files to a dataset")
    @APIResponse(responseCode = "200",
               description = "Files uploaded successfully to dataset")
    @Tag(name = "addFilesToDataset", 
         description = "Uploads a set of files to a dataset")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA))  
    public Response addFilesToDataset(@Context ContainerRequestContext crc, @PathParam("id") String idSupplied,
            @FormDataParam("jsonData") String jsonData) {

        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        User authUser;
        authUser = getRequestUser(crc);

        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(idSupplied);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        dataset.getLocks().forEach(dl -> {
            logger.info(dl.toString());
        });

        //------------------------------------
        // (2a) Make sure dataset does not have package file
        // --------------------------------------

        for (DatasetVersion dv : dataset.getVersions()) {
            if (dv.isHasPackageFile()) {
                return error(Response.Status.FORBIDDEN,
                        BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                );
            }
        }

        DataverseRequest dvRequest = createDataverseRequest(authUser);

        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(
                dvRequest,
                this.ingestService,
                this.datasetService,
                this.fileService,
                this.permissionSvc,
                this.commandEngine,
                this.systemConfig
        );

        return addFileHelper.addFiles(jsonData, dataset, authUser);

    }

    /**
     * Replace multiple Files to an existing Dataset
     *
     * @param idSupplied
     * @param jsonData
     * @return
     */
    @POST
    @AuthRequired
    @Path("{id}/replaceFiles")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    @Operation(summary = "Replace a set of files to a dataset", 
               description = "Replace a set of files to a dataset")
    @APIResponse(responseCode = "200",
               description = "Files replaced successfully to dataset")
    @Tag(name = "replaceFilesInDataset", 
         description = "Replace a set of files to a dataset")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA)) 
    public Response replaceFilesInDataset(@Context ContainerRequestContext crc,
                                          @PathParam("id") String idSupplied,
                                          @FormDataParam("jsonData") String jsonData) {

        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the ContainerRequestContext
        // -------------------------------------
        User authUser;
        authUser = getRequestUser(crc);

        // -------------------------------------
        // (2) Get the Dataset Id
        // -------------------------------------
        Dataset dataset;

        try {
            dataset = findDatasetOrDie(idSupplied);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        dataset.getLocks().forEach(dl -> {
            logger.info(dl.toString());
        });

        //------------------------------------
        // (2a) Make sure dataset does not have package file
        // --------------------------------------

        for (DatasetVersion dv : dataset.getVersions()) {
            if (dv.isHasPackageFile()) {
                return error(Response.Status.FORBIDDEN,
                        BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile")
                );
            }
        }

        DataverseRequest dvRequest = createDataverseRequest(authUser);

        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(
                dvRequest,
                this.ingestService,
                this.datasetService,
                this.fileService,
                this.permissionSvc,
                this.commandEngine,
                this.systemConfig
        );

        return addFileHelper.replaceFiles(jsonData, dataset, authUser);

    }

    /**
     * API to find curation assignments and statuses
     *
     * @return
     * @throws WrappedResponse
     */
    @GET
    @AuthRequired
    @Path("/listCurationStates")
    @Produces("text/csv")
    public Response getCurationStates(@Context ContainerRequestContext crc) throws WrappedResponse {

        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        List<DataverseRole> allRoles = dataverseRoleService.findAll();
        List<DataverseRole> curationRoles = new ArrayList<DataverseRole>();
        allRoles.forEach(r -> {
            if (r.permissions().contains(Permission.PublishDataset))
                curationRoles.add(r);
        });
        HashMap<String, HashSet<String>> assignees = new HashMap<String, HashSet<String>>();
        curationRoles.forEach(r -> {
            assignees.put(r.getAlias(), null);
        });

        StringBuilder csvSB = new StringBuilder(String.join(",",
                BundleUtil.getStringFromBundle("dataset"),
                BundleUtil.getStringFromBundle("datasets.api.creationdate"),
                BundleUtil.getStringFromBundle("datasets.api.modificationdate"),
                BundleUtil.getStringFromBundle("datasets.api.curationstatus"),
                String.join(",", assignees.keySet())));
        for (Dataset dataset : datasetSvc.findAllWithDraftVersion()) {
            List<RoleAssignment> ras = permissionService.assignmentsOn(dataset);
            curationRoles.forEach(r -> {
                assignees.put(r.getAlias(), new HashSet<String>());
            });
            for (RoleAssignment ra : ras) {
                if (curationRoles.contains(ra.getRole())) {
                    assignees.get(ra.getRole().getAlias()).add(ra.getAssigneeIdentifier());
                }
            }
            DatasetVersion dsv = dataset.getLatestVersion();
            String name = "\"" + dataset.getCurrentName().replace("\"", "\"\"") + "\"";
            String status = dsv.getExternalStatusLabel();
            String url = systemConfig.getDataverseSiteUrl() + dataset.getTargetUrl() + dataset.getGlobalId().asString();
            String date = new SimpleDateFormat("yyyy-MM-dd").format(dsv.getCreateTime());
            String modDate = new SimpleDateFormat("yyyy-MM-dd").format(dsv.getLastUpdateTime());
            String hyperlink = "\"=HYPERLINK(\"\"" + url + "\"\",\"\"" + name + "\"\")\"";
            List<String> sList = new ArrayList<String>();
            assignees.entrySet().forEach(e -> sList.add(e.getValue().size() == 0 ? "" : String.join(";", e.getValue())));
            csvSB.append("\n").append(String.join(",", hyperlink, date, modDate, status == null ? "" : status, String.join(",", sList)));
        }
        csvSB.append("\n");
        return ok(csvSB.toString(), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "datasets.status.csv");
    }

    // APIs to manage archival status

    @GET
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/{version}/archivalStatus")
    public Response getDatasetVersionArchivalStatus(@Context ContainerRequestContext crc,
                                                    @PathParam("id") String datasetId,
                                                    @PathParam("version") String versionNumber,
                                                    @Context UriInfo uriInfo,
                                                    @Context HttpHeaders headers) {

        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
            if (!au.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
            DataverseRequest req = createDataverseRequest(au);
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionNumber, findDatasetOrDie(datasetId), uriInfo,
                    headers);

            if (dsv.getArchivalCopyLocation() == null) {
                return error(Status.NOT_FOUND, "This dataset version has not been archived");
            } else {
                JsonObject status = JsonUtil.getJsonObject(dsv.getArchivalCopyLocation());
                return ok(status);
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/{version}/archivalStatus")
    public Response setDatasetVersionArchivalStatus(@Context ContainerRequestContext crc,
                                                    @PathParam("id") String datasetId,
                                                    @PathParam("version") String versionNumber,
                                                    String newStatus,
                                                    @Context UriInfo uriInfo,
                                                    @Context HttpHeaders headers) {

        logger.fine(newStatus);
        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);

            if (!au.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
            
            //Verify we have valid json after removing any HTML tags (the status gets displayed in the UI, so we want plain text).
            JsonObject update= JsonUtil.getJsonObject(MarkupChecker.stripAllTags(newStatus));
            
            if (update.containsKey(DatasetVersion.ARCHIVAL_STATUS) && update.containsKey(DatasetVersion.ARCHIVAL_STATUS_MESSAGE)) {
                String status = update.getString(DatasetVersion.ARCHIVAL_STATUS);
                if (status.equals(DatasetVersion.ARCHIVAL_STATUS_PENDING) || status.equals(DatasetVersion.ARCHIVAL_STATUS_FAILURE)
                        || status.equals(DatasetVersion.ARCHIVAL_STATUS_SUCCESS)) {

                    DataverseRequest req = createDataverseRequest(au);
                    DatasetVersion dsv = getDatasetVersionOrDie(req, versionNumber, findDatasetOrDie(datasetId),
                            uriInfo, headers);

                    if (dsv == null) {
                        return error(Status.NOT_FOUND, "Dataset version not found");
                    }
                    if (isSingleVersionArchiving()) {
                        for (DatasetVersion version : dsv.getDataset().getVersions()) {
                            if ((!dsv.equals(version)) && (version.getArchivalCopyLocation() != null)) {
                                return error(Status.CONFLICT, "Dataset already archived.");
                            }
                        }
                    }

                    dsv.setArchivalCopyLocation(JsonUtil.prettyPrint(update));
                    dsv = datasetversionService.merge(dsv);
                    logger.fine("status now: " + dsv.getArchivalCopyLocationStatus());
                    logger.fine("message now: " + dsv.getArchivalCopyLocationMessage());

                    return ok("Status updated");
                }
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        } catch (JsonException| IllegalStateException ex) {
            return error(Status.BAD_REQUEST, "Unable to parse provided JSON");
        }
        return error(Status.BAD_REQUEST, "Unacceptable status format");
    }
    
    @DELETE
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/{version}/archivalStatus")
    public Response deleteDatasetVersionArchivalStatus(@Context ContainerRequestContext crc,
                                                       @PathParam("id") String datasetId,
                                                       @PathParam("version") String versionNumber,
                                                       @Context UriInfo uriInfo,
                                                       @Context HttpHeaders headers) {

        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
            if (!au.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }

            DataverseRequest req = createDataverseRequest(au);
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionNumber, findDatasetOrDie(datasetId), uriInfo,
                    headers);
            if (dsv == null) {
                return error(Status.NOT_FOUND, "Dataset version not found");
            }
            dsv.setArchivalCopyLocation(null);
            dsv = datasetversionService.merge(dsv);

            return ok("Status deleted");

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    private boolean isSingleVersionArchiving() {
        String className = settingsService.getValueForKey(SettingsServiceBean.Key.ArchiverClassName, null);
        if (className != null) {
            Class<? extends AbstractSubmitToArchiveCommand> clazz;
            try {
                clazz =  Class.forName(className).asSubclass(AbstractSubmitToArchiveCommand.class);
                return ArchiverUtil.onlySingleVersionArchiving(clazz, settingsService);
            } catch (ClassNotFoundException e) {
                logger.warning(":ArchiverClassName does not refer to a known Archiver");
            } catch (ClassCastException cce) {
                logger.warning(":ArchiverClassName does not refer to an Archiver class");
            }
        }
        return false;
    }
    
    // This method provides a callback for an external tool to retrieve it's
    // parameters/api URLs. If the request is authenticated, e.g. by it being
    // signed, the api URLs will be signed. If a guest request is made, the URLs
    // will be plain/unsigned.
    // This supports the cases where a tool is accessing a restricted resource (e.g.
    // for a draft dataset), or public case.
    @GET
    @AuthRequired
    @Path("{id}/versions/{version}/toolparams/{tid}")
    public Response getExternalToolDVParams(@Context ContainerRequestContext crc,
                                            @PathParam("tid") long externalToolId,
                                            @PathParam("id") String datasetId,
                                            @PathParam("version") String version,
                                            @QueryParam(value = "locale") String locale) {
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            DatasetVersion target = getDatasetVersionOrDie(req, version, findDatasetOrDie(datasetId), null, null);
            if (target == null) {
                return error(BAD_REQUEST, "DatasetVersion not found.");
            }
            
            ExternalTool externalTool = externalToolService.findById(externalToolId);
            if(externalTool==null) {
                return error(BAD_REQUEST, "External tool not found.");
            }
            if (!ExternalTool.Scope.DATASET.equals(externalTool.getScope())) {
                return error(BAD_REQUEST, "External tool does not have dataset scope.");
            }
            ApiToken apiToken = null;
            User u = getRequestUser(crc);
            apiToken = authSvc.getValidApiTokenForUser(u);

            URLTokenUtil eth = new ExternalToolHandler(externalTool, target.getDataset(), apiToken, locale);
            return ok(eth.createPostBody(eth.getParams(JsonUtil.getJsonObject(externalTool.getToolParameters())), JsonUtil.getJsonArray(externalTool.getAllowedApiCalls())));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @Path("summaryFieldNames")
    public Response getDatasetSummaryFieldNames() {
        String customFieldNames = settingsService.getValueForKey(SettingsServiceBean.Key.CustomDatasetSummaryFields);
        String[] fieldNames = DatasetUtil.getDatasetSummaryFieldNames(customFieldNames);
        JsonArrayBuilder fieldNamesArrayBuilder = Json.createArrayBuilder();
        for (String fieldName : fieldNames) {
            fieldNamesArrayBuilder.add(fieldName);
        }
        return ok(fieldNamesArrayBuilder);
    }

    @GET
    @Path("privateUrlDatasetVersion/{privateUrlToken}")
    public Response getPrivateUrlDatasetVersion(@PathParam("privateUrlToken") String privateUrlToken, @QueryParam("returnOwners") boolean returnOwners) {
        PrivateUrlUser privateUrlUser = privateUrlService.getPrivateUrlUserFromToken(privateUrlToken);
        if (privateUrlUser == null) {
            return notFound("Private URL user not found");
        }
        boolean isAnonymizedAccess = privateUrlUser.hasAnonymizedAccess();
        String anonymizedFieldTypeNames = settingsSvc.getValueForKey(SettingsServiceBean.Key.AnonymizedFieldTypeNames);
        if(isAnonymizedAccess && anonymizedFieldTypeNames == null) {
            throw new NotAcceptableException("Anonymized Access not enabled");
        }
        DatasetVersion dsv = privateUrlService.getDraftDatasetVersionFromToken(privateUrlToken);
        if (dsv == null || dsv.getId() == null) {
            return notFound("Dataset version not found");
        }
        JsonObjectBuilder responseJson;
        if (isAnonymizedAccess) {
            List<String> anonymizedFieldTypeNamesList = new ArrayList<>(Arrays.asList(anonymizedFieldTypeNames.split(",\\s")));
            responseJson = json(dsv, anonymizedFieldTypeNamesList, true, returnOwners);
        } else {
            responseJson = json(dsv, null, true, returnOwners);
        }
        return ok(responseJson);
    }

    @GET
    @Path("privateUrlDatasetVersion/{privateUrlToken}/citation")
    public Response getPrivateUrlDatasetVersionCitation(@PathParam("privateUrlToken") String privateUrlToken) {
        PrivateUrlUser privateUrlUser = privateUrlService.getPrivateUrlUserFromToken(privateUrlToken);
        if (privateUrlUser == null) {
            return notFound("Private URL user not found");
        }
        DatasetVersion dsv = privateUrlService.getDraftDatasetVersionFromToken(privateUrlToken);
        return (dsv == null || dsv.getId() == null) ? notFound("Dataset version not found")
                : ok(dsv.getCitation(true, privateUrlUser.hasAnonymizedAccess()));
    }

    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/citation")
    public Response getDatasetVersionCitation(@Context ContainerRequestContext crc,
                                              @PathParam("id") String datasetId,
                                              @PathParam("versionId") String versionId,
                                              @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                              @Context UriInfo uriInfo,
                                              @Context HttpHeaders headers) {
        boolean checkFilePerms = false;
        return response(req -> ok(
                getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers,
                        includeDeaccessioned, checkFilePerms).getCitation(true, false)),
                getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{id}/versions/{versionId}/deaccession")
    public Response deaccessionDataset(@Context ContainerRequestContext crc, @PathParam("id") String datasetId, @PathParam("versionId") String versionId, String jsonBody, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        if (DS_VERSION_DRAFT.equals(versionId) || DS_VERSION_LATEST.equals(versionId)) {
            return badRequest(BundleUtil.getStringFromBundle("datasets.api.deaccessionDataset.invalid.version.identifier.error", List.of(DS_VERSION_LATEST_PUBLISHED)));
        }
        return response(req -> {
            DatasetVersion datasetVersion = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers);
            try {
                JsonObject jsonObject = JsonUtil.getJsonObject(jsonBody);
                datasetVersion.setVersionNote(jsonObject.getString("deaccessionReason"));
                String deaccessionForwardURL = jsonObject.getString("deaccessionForwardURL", null);
                if (deaccessionForwardURL != null) {
                    try {
                        datasetVersion.setArchiveNote(deaccessionForwardURL);
                    } catch (IllegalArgumentException iae) {
                        return badRequest(BundleUtil.getStringFromBundle("datasets.api.deaccessionDataset.invalid.forward.url", List.of(iae.getMessage())));
                    }
                }
                execCommand(new DeaccessionDatasetVersionCommand(req, datasetVersion, false));
                
                return ok("Dataset " + 
                        (":persistentId".equals(datasetId) ? datasetVersion.getDataset().getGlobalId().asString() : datasetId) + 
                        " deaccessioned for version " + versionId);
            } catch (JsonParsingException jpe) {
                return error(Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage());
            }
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/guestbookEntryAtRequest")
    public Response getGuestbookEntryOption(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
                                            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        String gbAtRequest = dataset.getGuestbookEntryAtRequest();
        if(gbAtRequest == null || gbAtRequest.equals(DvObjectContainer.UNDEFINED_CODE)) {
            return ok("Not set on dataset, using the default: " + dataset.getEffectiveGuestbookEntryAtRequest());
        }
        return ok(dataset.getEffectiveGuestbookEntryAtRequest());
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/guestbookEntryAtRequest")
    public Response setguestbookEntryAtRequest(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
                                               boolean gbAtRequest,
                                               @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {

        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        Optional<Boolean> gbAtRequestOpt = JvmSettings.GUESTBOOK_AT_REQUEST.lookupOptional(Boolean.class);
        if (!gbAtRequestOpt.isPresent()) {
            return error(Response.Status.FORBIDDEN, "Guestbook Entry At Request cannot be set. This server is not configured to allow it.");
        }
        String choice = Boolean.valueOf(gbAtRequest).toString();
        dataset.setGuestbookEntryAtRequest(choice);
        datasetService.merge(dataset);
        return ok("Guestbook Entry At Request set to: " + choice);
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/guestbookEntryAtRequest")
    public Response resetGuestbookEntryAtRequest(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
                                                 @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {

        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }

        dataset.setGuestbookEntryAtRequest(DvObjectContainer.UNDEFINED_CODE);
        datasetService.merge(dataset);
        return ok("Guestbook Entry At Request reset to default: " + dataset.getEffectiveGuestbookEntryAtRequest());
    }

    @GET
    @AuthRequired
    @Path("{id}/userPermissions")
    public Response getUserPermissionsOnDataset(@Context ContainerRequestContext crc, @PathParam("id") String datasetId) {
        Dataset dataset;
        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        User requestUser = getRequestUser(crc);
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("canViewUnpublishedDataset", permissionService.userOn(requestUser, dataset).has(Permission.ViewUnpublishedDataset));
        jsonObjectBuilder.add("canEditDataset", permissionService.userOn(requestUser, dataset).has(Permission.EditDataset));
        jsonObjectBuilder.add("canPublishDataset", permissionService.userOn(requestUser, dataset).has(Permission.PublishDataset));
        jsonObjectBuilder.add("canManageDatasetPermissions", permissionService.userOn(requestUser, dataset).has(Permission.ManageDatasetPermissions));
        jsonObjectBuilder.add("canDeleteDatasetDraft", permissionService.userOn(requestUser, dataset).has(Permission.DeleteDatasetDraft));
        return ok(jsonObjectBuilder);
    }

    @GET
    @AuthRequired
    @Path("{id}/versions/{versionId}/canDownloadAtLeastOneFile")
    public Response getCanDownloadAtLeastOneFile(@Context ContainerRequestContext crc,
                                                 @PathParam("id") String datasetId,
                                                 @PathParam("versionId") String versionId,
                                                 @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                                 @Context UriInfo uriInfo,
                                                 @Context HttpHeaders headers) {
        return response(req -> {
            DatasetVersion datasetVersion = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers, includeDeaccessioned);
            return ok(permissionService.canDownloadAtLeastOneFile(req, datasetVersion));
        }, getRequestUser(crc));
    }
    
    /**
     * Get the PidProvider that will be used for generating new DOIs in this dataset
     *
     * @return - the id of the effective PID generator for the given dataset
     * @throws WrappedResponse
     */
    @GET
    @AuthRequired
    @Path("{identifier}/pidGenerator")
    public Response getPidGenerator(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @Context HttpHeaders headers) throws WrappedResponse {

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        PidProvider pidProvider = dataset.getEffectivePidGenerator();
        if(pidProvider == null) {
            //This is basically a config error, e.g. if a valid pid provider was removed after this dataset used it
            return error(Response.Status.NOT_FOUND, BundleUtil.getStringFromBundle("datasets.api.pidgenerator.notfound"));
        }
        String pidGeneratorId = pidProvider.getId();
        return ok(pidGeneratorId);
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/pidGenerator")
    public Response setPidGenerator(@Context ContainerRequestContext crc, @PathParam("identifier") String datasetId,
            String generatorId, @Context HttpHeaders headers) throws WrappedResponse {

        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.UNAUTHORIZED, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(datasetId);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
        if (PidUtil.getManagedProviderIds().contains(generatorId)) {
            dataset.setPidGeneratorId(generatorId);
            datasetService.merge(dataset);
            return ok("PID Generator set to: " + generatorId);
        } else {
            return error(Response.Status.NOT_FOUND, "No PID Generator found for the give id");
        }

    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/pidGenerator")
    public Response resetPidGenerator(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @Context HttpHeaders headers) throws WrappedResponse {

        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        Dataset dataset;

        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }

        dataset.setPidGenerator(null);
        datasetService.merge(dataset);
        return ok("Pid Generator reset to default: " + dataset.getEffectivePidGenerator().getId());
    }

    @GET
    @Path("datasetTypes")
    public Response getDatasetTypes() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        List<DatasetType> datasetTypes = datasetTypeSvc.listAll();
        for (DatasetType datasetType : datasetTypes) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("id", datasetType.getId());
            job.add("name", datasetType.getName());
            jab.add(job);
        }
        return ok(jab.build());
    }

    @GET
    @Path("datasetTypes/{idOrName}")
    public Response getDatasetTypes(@PathParam("idOrName") String idOrName) {
        DatasetType datasetType = null;
        if (StringUtils.isNumeric(idOrName)) {
            try {
                long id = Long.parseLong(idOrName);
                datasetType = datasetTypeSvc.getById(id);
            } catch (NumberFormatException ex) {
                return error(NOT_FOUND, "Could not find a dataset type with id " + idOrName);
            }
        } else {
            datasetType = datasetTypeSvc.getByName(idOrName);
        }
        if (datasetType != null) {
            return ok(datasetType.toJson());
        } else {
            return error(NOT_FOUND, "Could not find a dataset type with name " + idOrName);
        }
    }

    @POST
    @AuthRequired
    @Path("datasetTypes")
    public Response addDatasetType(@Context ContainerRequestContext crc, String jsonIn) {
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        if (jsonIn == null || jsonIn.isEmpty()) {
            return error(BAD_REQUEST, "JSON input was null or empty!");
        }

        String nameIn = null;
        try {
            JsonObject jsonObject = JsonUtil.getJsonObject(jsonIn);
            nameIn = jsonObject.getString("name", null);
        } catch (JsonParsingException ex) {
            return error(BAD_REQUEST, "Problem parsing supplied JSON: " + ex.getLocalizedMessage());
        }
        if (nameIn == null) {
            return error(BAD_REQUEST, "A name for the dataset type is required");
        }
        if (StringUtils.isNumeric(nameIn)) {
            // getDatasetTypes supports id or name so we don't want a names that looks like an id
            return error(BAD_REQUEST, "The name of the type cannot be only digits.");
        }

        try {
            DatasetType datasetType = new DatasetType();
            datasetType.setName(nameIn);
            DatasetType saved = datasetTypeSvc.save(datasetType);
            Long typeId = saved.getId();
            String name = saved.getName();
            return ok(saved.toJson());
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, ex.getMessage());
        }
    }

    @DELETE
    @AuthRequired
    @Path("datasetTypes/{id}")
    public Response deleteDatasetType(@Context ContainerRequestContext crc, @PathParam("id") String doomed) {
        AuthenticatedUser user;
        try {
            user = getRequestAuthenticatedUserOrDie(crc);
        } catch (WrappedResponse ex) {
            return error(Response.Status.BAD_REQUEST, "Authentication is required.");
        }
        if (!user.isSuperuser()) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }

        if (doomed == null || doomed.isEmpty()) {
            throw new IllegalArgumentException("ID is required!");
        }

        long idToDelete;
        try {
            idToDelete = Long.parseLong(doomed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID must be a number");
        }

        DatasetType datasetTypeToDelete = datasetTypeSvc.getById(idToDelete);
        if (datasetTypeToDelete == null) {
            return error(BAD_REQUEST, "Could not find dataset type with id " + idToDelete);
        }

        if (DatasetType.DEFAULT_DATASET_TYPE.equals(datasetTypeToDelete.getName())) {
            return error(Status.FORBIDDEN, "You cannot delete the default dataset type: " + DatasetType.DEFAULT_DATASET_TYPE);
        }

        try {
            int numDeleted = datasetTypeSvc.deleteById(idToDelete);
            if (numDeleted == 1) {
                return ok("deleted");
            } else {
                return error(BAD_REQUEST, "Something went wrong. Number of dataset types deleted: " + numDeleted);
            }
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, ex.getMessage());
        }
    }

}
