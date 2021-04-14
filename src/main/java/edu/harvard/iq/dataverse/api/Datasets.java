package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
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
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.engine.command.impl.AddLockCommand;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CuratePublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetLinkingDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetSpecificPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDraftDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestAccessibleDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ImportFromFileSystemCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.ListVersionsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetResult;
import edu.harvard.iq.dataverse.engine.command.impl.RemoveLockCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ReturnDatasetToAuthorCommand;
import edu.harvard.iq.dataverse.engine.command.impl.SetDatasetCitationDateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.SubmitDatasetForReviewCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetTargetURLCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.S3PackageImporter;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.S3AccessIO;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UnforcedCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.GetDatasetStorageSizeCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDvObjectPIDMetadataCommand;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitations;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitationsServiceBean;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean.MakeDataCountEntry;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.EjbUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import javax.ws.rs.core.UriInfo;
import org.apache.solr.client.solrj.SolrServerException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.amazonaws.services.s3.model.PartETag;
import edu.harvard.iq.dataverse.FileMetadata;
import java.util.Map.Entry;

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());
    
    @Inject DataverseSession session;    

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseServiceBean dataverseService;
    
    @EJB
    UserNotificationServiceBean userNotificationService;
    
    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    AuthenticationServiceBean authenticationServiceBean;
    
    @EJB
    DDIExportServiceBean ddiExportService;
    
    @EJB
    DatasetFieldServiceBean datasetfieldService;

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
    
    @Inject
    MakeDataCountLoggingServiceBean mdcLogService;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;

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
    @Path("{id}")
    public Response getDataset(@PathParam("id") String id, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) {
        return response( req -> {
            final Dataset retrieved = execCommand(new GetDatasetCommand(req, findDatasetOrDie(id)));
            final DatasetVersion latest = execCommand(new GetLatestAccessibleDatasetVersionCommand(req, retrieved));
            final JsonObjectBuilder jsonbuilder = json(retrieved);
            //Report MDC if this is a released version (could be draft if user has access, or user may not have access at all and is not getting metadata beyond the minimum)
            if((latest != null) && latest.isReleased()) {
                MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountEntry(uriInfo, headers, dvRequestService, retrieved);
                mdcLogService.logEntry(entry);
            }
            return ok(jsonbuilder.add("latestVersion", (latest != null) ? json(latest) : null));
        });
    }
    
    // TODO: 
    // This API call should, ideally, call findUserOrDie() and the GetDatasetCommand 
    // to obtain the dataset that we are trying to export - which would handle
    // Auth in the process... For now, Auth isn't necessary - since export ONLY 
    // WORKS on published datasets, which are open to the world. -- L.A. 4.5
    
    @GET
    @Path("/export")
    @Produces({"application/xml", "application/json", "application/html" })
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
            return error(Response.Status.FORBIDDEN, "Export Failed");
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteDataset( @PathParam("id") String id) {
        // Internally, "DeleteDatasetCommand" simply redirects to "DeleteDatasetVersionCommand"
        // (and there's a comment that says "TODO: remove this command")
        // do we need an exposed API call for it? 
        // And DeleteDatasetVersionCommand further redirects to DestroyDatasetCommand, 
        // if the dataset only has 1 version... In other words, the functionality 
        // currently provided by this API is covered between the "deleteDraftVersion" and
        // "destroyDataset" API calls.  
        // (The logic below follows the current implementation of the underlying 
        // commands!)
        
        return response( req -> {
            Dataset doomed = findDatasetOrDie(id);
            DatasetVersion doomedVersion = doomed.getLatestVersion();
            User u = findUserOrDie();
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
        });
    }
        
    @DELETE
    @Path("{id}/destroy")
    public Response destroyDataset(@PathParam("id") String id) {

        return response(req -> {
            // first check if dataset is released, and if so, if user is a superuser
            Dataset doomed = findDatasetOrDie(id);
            User u = findUserOrDie();

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
        });
    }
    
    @DELETE
    @Path("{id}/versions/{versionId}")
    public Response deleteDraftVersion( @PathParam("id") String id,  @PathParam("versionId") String versionId ){
        if ( ! ":draft".equals(versionId) ) {
            return badRequest("Only the :draft version can be deleted");
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
        });
    }
        
    @DELETE
    @Path("{datasetId}/deleteLink/{linkedDataverseId}")
    public Response deleteDatasetLinkingDataverse( @PathParam("datasetId") String datasetId, @PathParam("linkedDataverseId") String linkedDataverseId) {
                boolean index = true;
        return response(req -> {
            execCommand(new DeleteDatasetLinkingDataverseCommand(req, findDatasetOrDie(datasetId), findDatasetLinkingDataverseOrDie(datasetId, linkedDataverseId), index));
            return ok("Link from Dataset " + datasetId + " to linked Dataverse " + linkedDataverseId + " deleted");
        });
    }
        
    @PUT
    @Path("{id}/citationdate")
    public Response setCitationDate( @PathParam("id") String id, String dsfTypeName) {
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
        });
    }    
    
    @DELETE
    @Path("{id}/citationdate")
    public Response useDefaultCitationDate( @PathParam("id") String id) {
        return response( req -> {
            execCommand(new SetDatasetCitationDateCommand(req, findDatasetOrDie(id), null));
            return ok("Citation Date for dataset " + id + " set to default");
        });
    }         
    
    @GET
    @Path("{id}/versions")
    public Response listVersions( @PathParam("id") String id ) {
        return response( req ->
             ok( execCommand( new ListVersionsCommand(req, findDatasetOrDie(id)) )
                                .stream()
                                .map( d -> json(d) )
                                .collect(toJsonArray())));
    }
    
    @GET
    @Path("{id}/versions/{versionId}")
    public Response getVersion( @PathParam("id") String datasetId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return response( req -> {
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers);            
            return (dsv == null || dsv.getId() == null) ? notFound("Dataset version not found")
                                                        : ok(json(dsv));
        });
    }
    
    @GET
    @Path("{id}/versions/{versionId}/files")
    public Response getVersionFiles( @PathParam("id") String datasetId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return response( req -> ok( jsonFileMetadatas(
                         getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers).getFileMetadatas())));
    }
    
    @GET
    @Path("{id}/dirindex")
    @Produces("text/html")
    public Response getFileAccessFolderView(@PathParam("id") String datasetId, @QueryParam("version") String versionId, @QueryParam("folder") String folderName, @QueryParam("original") Boolean originals, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) {

        folderName = folderName == null ? "" : folderName;
        versionId = versionId == null ? ":latest-published" : versionId; 
        
        DatasetVersion version; 
        try {
            DataverseRequest req = createDataverseRequest(findUserOrDie());
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
        response.setHeader("Content-disposition", "attachment; filename=\"" + indexFileName + "\"");

        
        return Response.ok()
                .entity(output)
                //.type("application/html").
                .build();
    }
    
    @GET
    @Path("{id}/versions/{versionId}/metadata")
    public Response getVersionMetadata( @PathParam("id") String datasetId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return response( req -> ok(
                    jsonByBlocks(
                        getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId), uriInfo, headers )
                                .getDatasetFields())));
    }
    
    @GET
    @Path("{id}/versions/{versionNumber}/metadata/{block}")
    public Response getVersionMetadataBlock( @PathParam("id") String datasetId, 
                                             @PathParam("versionNumber") String versionNumber, 
                                             @PathParam("block") String blockName, 
                                             @Context UriInfo uriInfo, 
                                             @Context HttpHeaders headers ) {
        
        return response( req -> {
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionNumber, findDatasetOrDie(datasetId), uriInfo, headers );
            
            Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(dsv.getDatasetFields());
            for ( Map.Entry<MetadataBlock, List<DatasetField>> p : fieldsByBlock.entrySet() ) {
                if ( p.getKey().getName().equals(blockName) ) {
                    return ok(json(p.getKey(), p.getValue()));
                }
            }
            return notFound("metadata block named " + blockName + " not found");
        });
    }
    
    @GET
    @Path("{id}/modifyRegistration")
    public Response updateDatasetTargetURL(@PathParam("id") String id ) {
        return response( req -> {
            execCommand(new UpdateDatasetTargetURLCommand(findDatasetOrDie(id), req));
            return ok("Dataset " + id + " target url updated");
        });
    }
    
    @POST
    @Path("/modifyRegistrationAll")
    public Response updateDatasetTargetURLAll() {
        return response( req -> {
            datasetService.findAll().forEach( ds -> {
                try {
                    execCommand(new UpdateDatasetTargetURLCommand(findDatasetOrDie(ds.getId().toString()), req));
                } catch (WrappedResponse ex) {
                    Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            return ok("Update All Dataset target url completed");
        });
    }
    
    @POST
    @Path("{id}/modifyRegistrationMetadata")
    public Response updateDatasetPIDMetadata(@PathParam("id") String id) {

        try {
            Dataset dataset = findDatasetOrDie(id);
            if (!dataset.isReleased()) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.failure.dataset.must.be.released"));
            }
        } catch (WrappedResponse ex) {
            Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
        }

        return response(req -> {
            execCommand(new UpdateDvObjectPIDMetadataCommand(findDatasetOrDie(id), req));
            List<String> args = Arrays.asList(id);
            return ok(BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.success.for.single.dataset", args));
        });
    }
    
    @GET
    @Path("/modifyRegistrationPIDMetadataAll")
    public Response updateDatasetPIDMetadataAll() {
        return response( req -> {
            datasetService.findAll().forEach( ds -> {
                try {
                    execCommand(new UpdateDvObjectPIDMetadataCommand(findDatasetOrDie(ds.getId().toString()), req));
                } catch (WrappedResponse ex) {
                    Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
                }
            });           
            return ok(BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.success.for.update.all"));
        });
    }
  
    @PUT
    @Path("{id}/versions/{versionId}")
    public Response updateDraftVersion( String jsonBody, @PathParam("id") String id,  @PathParam("versionId") String versionId ){
        
        if ( ! ":draft".equals(versionId) ) {
            return error( Response.Status.BAD_REQUEST, "Only the :draft version can be updated");
        }
        
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            DataverseRequest req = createDataverseRequest(findUserOrDie());
            Dataset ds = findDatasetOrDie(id);
            JsonObject json = Json.createReader(rdr).readObject();
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
            if ( updateDraft ) {
                final DatasetVersion editVersion = ds.getEditVersion();
                editVersion.setDatasetFields(incomingVersion.getDatasetFields());
                editVersion.setTermsOfUseAndAccess( incomingVersion.getTermsOfUseAndAccess() );
                Dataset managedDataset = execCommand(new UpdateDatasetVersionCommand(ds, req));
                managedVersion = managedDataset.getEditVersion();
            } else {
                managedVersion = execCommand(new CreateDatasetVersionCommand(req, ds, incomingVersion));
            }
//            DatasetVersion managedVersion = execCommand( updateDraft
//                                                             ? new UpdateDatasetVersionCommand(req, incomingVersion)
//                                                             : new CreateDatasetVersionCommand(req, ds, incomingVersion));
            return ok( json(managedVersion) );
                    
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset version Json: " + ex.getMessage(), ex);
            return error( Response.Status.BAD_REQUEST, "Error parsing dataset version: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
    }
    
    @PUT
    @Path("{id}/deleteMetadata")
    public Response deleteVersionMetadata(String jsonBody, @PathParam("id") String id) throws WrappedResponse {

        DataverseRequest req = createDataverseRequest(findUserOrDie());

        return processDatasetFieldDataDelete(jsonBody, id, req);
    }

    private Response processDatasetFieldDataDelete(String jsonBody, String id, DataverseRequest req) {
        try (StringReader rdr = new StringReader(jsonBody)) {

            Dataset ds = findDatasetOrDie(id);
            JsonObject json = Json.createReader(rdr).readObject();
            DatasetVersion dsv = ds.getEditVersion();

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


            
            boolean updateDraft = ds.getLatestVersion().isDraft();
            DatasetVersion managedVersion = updateDraft 
                    ? execCommand(new UpdateDatasetVersionCommand(ds, req)).getEditVersion()
                    : execCommand(new CreateDatasetVersionCommand(req, ds, dsv));
            return ok(json(managedVersion));

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
    @Path("{id}/editMetadata")
    public Response editVersionMetadata(String jsonBody, @PathParam("id") String id, @QueryParam("replace") Boolean replace) {

        Boolean replaceData = replace != null;
        DataverseRequest req = null;
        try {
         req = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse ex) {
            logger.log(Level.SEVERE, "Edit metdata error: " + ex.getMessage(), ex);
            return ex.getResponse();
        }

        return processDatasetUpdate(jsonBody, id, req, replaceData);
    }
    
    
    private Response processDatasetUpdate(String jsonBody, String id, DataverseRequest req, Boolean replaceData){
        try (StringReader rdr = new StringReader(jsonBody)) {
           
            Dataset ds = findDatasetOrDie(id);
            JsonObject json = Json.createReader(rdr).readObject();
            DatasetVersion dsv = ds.getEditVersion();
            
            List<DatasetField> fields = new LinkedList<>();
            DatasetField singleField = null; 
            
            JsonArray fieldsJson = json.getJsonArray("fields");
            if( fieldsJson == null ){
                singleField  = jsonParser().parseField(json, Boolean.FALSE);
                fields.add(singleField);
            } else{
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
            boolean updateDraft = ds.getLatestVersion().isDraft();
            DatasetVersion managedVersion;

            if (updateDraft) {
                managedVersion = execCommand(new UpdateDatasetVersionCommand(ds, req)).getEditVersion();
            } else {
                managedVersion = execCommand(new CreateDatasetVersionCommand(req, ds, dsv));
            }

            return ok(json(managedVersion));

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
    @Path("{id}/actions/:publish")
    @Deprecated
    public Response publishDataseUsingGetDeprecated( @PathParam("id") String id, @QueryParam("type") String type ) {
        logger.info("publishDataseUsingGetDeprecated called on id " + id + ". Encourage use of POST rather than GET, which is deprecated.");
        return publishDataset(id, type, false);
    }

    @POST
    @Path("{id}/actions/:publish")
    public Response publishDataset(@PathParam("id") String id, @QueryParam("type") String type, @QueryParam("assureIsIndexed") boolean mustBeIndexed) {
        try {
            if (type == null) {
                return error(Response.Status.BAD_REQUEST, "Missing 'type' parameter (either 'major','minor', or 'updatecurrent').");
            }
            boolean updateCurrent=false;
            AuthenticatedUser user = findAuthenticatedUserOrDie();
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
                if(user.isSuperuser()) {
                  updateCurrent=true;
                } else {
                    return error(Response.Status.FORBIDDEN, "Only superusers can update the current version"); 
                }
                break;
                default:
                return error(Response.Status.BAD_REQUEST, "Illegal 'type' parameter value '" + type + "'. It needs to be either 'major', 'minor', or 'updatecurrent'.");
            }

            Dataset ds = findDatasetOrDie(id);
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
                            if (updateVersion.getArchivalCopyLocation() != null) {
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
                            .add("status", STATUS_OK)
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
    @Path("{id}/move/{targetDataverseAlias}")
    public Response moveDataset(@PathParam("id") String id, @PathParam("targetDataverseAlias") String targetDataverseAlias, @QueryParam("forceMove") Boolean force) {
        try {
            User u = findUserOrDie();            
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
    
    @PUT
    @Path("{linkedDatasetId}/link/{linkingDataverseAlias}") 
    public Response linkDataset(@PathParam("linkedDatasetId") String linkedDatasetId, @PathParam("linkingDataverseAlias") String linkingDataverseAlias) {        
        try{
            User u = findUserOrDie();            
            Dataset linked = findDatasetOrDie(linkedDatasetId);
            Dataverse linking = findDataverseOrDie(linkingDataverseAlias);
            if (linked == null){
                return error(Response.Status.BAD_REQUEST, "Linked Dataset not found.");
            } 
            if (linking == null){
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
    @Path("{id}/links")
    public Response getLinks(@PathParam("id") String idSupplied ) {
        try {
            User u = findUserOrDie();
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
     * @param ra role assignment DTO
     * @param id dataset id
     * @param apiKey
     */
    @POST
    @Path("{identifier}/assignments")
    public Response createAssignment(RoleAssignmentDTO ra, @PathParam("identifier") String id, @QueryParam("key") String apiKey) {
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
                    json(execCommand(new AssignRoleCommand(assignee, theRole, dataset, createDataverseRequest(findUserOrDie()), privateUrlToken))));
        } catch (WrappedResponse ex) {
            List<String> args = Arrays.asList(ex.getMessage());
            logger.log(Level.WARNING, BundleUtil.getStringFromBundle("datasets.api.grant.role.cant.create.assignment.error", args));
            return ex.getResponse();
        }

    }
    
    @DELETE
    @Path("{identifier}/assignments/{id}")
    public Response deleteAssignment(@PathParam("id") long assignmentId, @PathParam("identifier") String dsId) {
        RoleAssignment ra = em.find(RoleAssignment.class, assignmentId);
        if (ra != null) {
            try {
                findDatasetOrDie(dsId);
                execCommand(new RevokeRoleCommand(ra, createDataverseRequest(findUserOrDie())));
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
    @Path("{identifier}/assignments")
    public Response getAssignments(@PathParam("identifier") String id) {
        return response( req -> 
            ok( execCommand(
                       new ListRoleAssignments(req, findDatasetOrDie(id)))
                     .stream().map(ra->json(ra)).collect(toJsonArray())) );
    }

    @GET
    @Path("{id}/privateUrl")
    public Response getPrivateUrlData(@PathParam("id") String idSupplied) {
        return response( req -> {
            PrivateUrl privateUrl = execCommand(new GetPrivateUrlCommand(req, findDatasetOrDie(idSupplied)));
            return (privateUrl != null) ? ok(json(privateUrl)) 
                                        : error(Response.Status.NOT_FOUND, "Private URL not found.");
        });
    }

    @POST
    @Path("{id}/privateUrl")
    public Response createPrivateUrl(@PathParam("id") String idSupplied) {
        return response( req -> 
                ok(json(execCommand(
                        new CreatePrivateUrlCommand(req, findDatasetOrDie(idSupplied))))));
    }

    @DELETE
    @Path("{id}/privateUrl")
    public Response deletePrivateUrl(@PathParam("id") String idSupplied) {
        return response( req -> {
            Dataset dataset = findDatasetOrDie(idSupplied);
            PrivateUrl privateUrl = execCommand(new GetPrivateUrlCommand(req, dataset));
            if (privateUrl != null) {
                execCommand(new DeletePrivateUrlCommand(req, dataset));
                return ok("Private URL deleted.");
            } else {
                return notFound("No Private URL to delete.");
            }
        });
    }

    @GET
    @Path("{id}/thumbnail/candidates")
    public Response getDatasetThumbnailCandidates(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            boolean canUpdateThumbnail = false;
            try {
                canUpdateThumbnail = permissionSvc.requestOn(createDataverseRequest(findUserOrDie()), dataset).canIssue(UpdateDatasetThumbnailCommand.class);
            } catch (WrappedResponse ex) {
                logger.info("Exception thrown while trying to figure out permissions while getting thumbnail for dataset id " + dataset.getId() + ": " + ex.getLocalizedMessage());
            }
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

    // TODO: Rather than only supporting looking up files by their database IDs (dataFileIdSupplied), consider supporting persistent identifiers.
    @POST
    @Path("{id}/thumbnail/{dataFileId}")
    public Response setDataFileAsThumbnail(@PathParam("id") String idSupplied, @PathParam("dataFileId") long dataFileIdSupplied) {
        try {
            DatasetThumbnail datasetThumbnail = execCommand(new UpdateDatasetThumbnailCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(idSupplied), UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail, dataFileIdSupplied, null));
            return ok("Thumbnail set to " + datasetThumbnail.getBase64image());
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @Path("{id}/thumbnail")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDatasetLogo(@PathParam("id") String idSupplied, @FormDataParam("file") InputStream inputStream
    ) {
        try {
            DatasetThumbnail datasetThumbnail = execCommand(new UpdateDatasetThumbnailCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(idSupplied), UpdateDatasetThumbnailCommand.UserIntent.setNonDatasetFileAsThumbnail, null, inputStream));
            return ok("Thumbnail is now " + datasetThumbnail.getBase64image());
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @Path("{id}/thumbnail")
    public Response removeDatasetLogo(@PathParam("id") String idSupplied) {
        try {
            DatasetThumbnail datasetThumbnail = execCommand(new UpdateDatasetThumbnailCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(idSupplied), UpdateDatasetThumbnailCommand.UserIntent.removeThumbnail, null, null));
            return ok("Dataset thumbnail removed.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @GET
    @Path("{identifier}/dataCaptureModule/rsync")
    public Response getRsync(@PathParam("identifier") String id) {
        //TODO - does it make sense to switch this to dataset identifier for consistency with the rest of the DCM APIs?
        if (!DataCaptureModuleUtil.rsyncSupportEnabled(settingsSvc.getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            return error(Response.Status.METHOD_NOT_ALLOWED, SettingsServiceBean.Key.UploadMethods + " does not contain " + SystemConfig.FileUploadMethods.RSYNC + ".");
        }
        Dataset dataset = null;
        try {
            dataset = findDatasetOrDie(id);
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            ScriptRequestResponse scriptRequestResponse = execCommand(new RequestRsyncScriptCommand(createDataverseRequest(user), dataset));
            
            DatasetLock lock = datasetService.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, user.getId(), "script downloaded");
            if (lock == null) {
                logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
                return error(Response.Status.FORBIDDEN, "Failed to lock the dataset (dataset id="+dataset.getId()+")");
            }
            return ok(scriptRequestResponse.getScript(), MediaType.valueOf(MediaType.TEXT_PLAIN));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        } catch (EJBException ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Something went wrong attempting to download rsync script: " + EjbUtil.ejbExceptionToString(ex));
        }
    }
    
    /**
     * This api endpoint triggers the creation of a "package" file in a dataset 
     *    after that package has been moved onto the same filesystem via the Data Capture Module.
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
    @Path("{identifier}/dataCaptureModule/checksumValidation")
    public Response receiveChecksumValidationResults(@PathParam("identifier") String id, JsonObject jsonFromDcm) {
        logger.log(Level.FINE, "jsonFromDcm: {0}", jsonFromDcm);
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = findAuthenticatedUserOrDie();
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
                        JsonObject jsonFromImportJobKickoff = execCommand(new ImportFromFileSystemCommand(createDataverseRequest(findUserOrDie()), dataset, uploadFolder, new Long(totalSize), importMode));
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
                } else if(storageDriverType.equals("s3")) {
                    
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
                        
                    }  catch (IOException e) {
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
    @Path("{id}/submitForReview")
    public Response submitForReview(@PathParam("id") String idSupplied) {
        try {
            Dataset updatedDataset = execCommand(new SubmitDatasetForReviewCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(idSupplied)));
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
    @Path("{id}/returnToAuthor")
    public Response returnToAuthor(@PathParam("id") String idSupplied, String jsonBody) {
        
        if (jsonBody == null || jsonBody.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "You must supply JSON to this API endpoint and it must contain a reason for returning the dataset (field: reasonForReturn).");
        }
        StringReader rdr = new StringReader(jsonBody);
        JsonObject json = Json.createReader(rdr).readObject();
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            String reasonForReturn = null;           
            reasonForReturn = json.getString("reasonForReturn");
            // TODO: Once we add a box for the curator to type into, pass the reason for return to the ReturnDatasetToAuthorCommand and delete this check and call to setReturnReason on the API side.
            if (reasonForReturn == null || reasonForReturn.isEmpty()) {
                return error(Response.Status.BAD_REQUEST, "You must enter a reason for returning a dataset to the author(s).");
            }
            AuthenticatedUser authenticatedUser = findAuthenticatedUserOrDie();
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
@Path("{id}/uploadsid")
@Deprecated
public Response getUploadUrl(@PathParam("id") String idSupplied) {
	try {
		Dataset dataset = findDatasetOrDie(idSupplied);

		boolean canUpdateDataset = false;
		try {
			canUpdateDataset = permissionSvc.requestOn(createDataverseRequest(findUserOrDie()), dataset).canIssue(UpdateDatasetVersionCommand.class);
		} catch (WrappedResponse ex) {
			logger.info("Exception thrown while trying to figure out permissions while getting upload URL for dataset id " + dataset.getId() + ": " + ex.getLocalizedMessage());
			throw ex;
		}
		if (!canUpdateDataset) {
            return error(Response.Status.FORBIDDEN, "You are not permitted to upload files to this dataset.");
        }
        S3AccessIO<?> s3io = FileUtil.getS3AccessForDirectUpload(dataset);
        if(s3io == null) {
        	return error(Response.Status.NOT_FOUND,"Direct upload not supported for files in this dataset: " + dataset.getId());
		}
		String url = null;
        String storageIdentifier = null;
		try {
			url = s3io.generateTemporaryS3UploadUrl();
        	storageIdentifier = FileUtil.getStorageIdentifierFromLocation(s3io.getStorageLocation());
        } catch (IOException io) {
        	logger.warning(io.getMessage());
        	throw new WrappedResponse(io, error( Response.Status.INTERNAL_SERVER_ERROR, "Could not create process direct upload request"));
		}
        
		JsonObjectBuilder response = Json.createObjectBuilder()
	            .add("url", url)
	            .add("storageIdentifier", storageIdentifier );
		return ok(response);
	} catch (WrappedResponse wr) {
		return wr.getResponse();
	}
}

@GET
@Path("{id}/uploadurls")
public Response getMPUploadUrls(@PathParam("id") String idSupplied, @QueryParam("size") long fileSize) {
	try {
		Dataset dataset = findDatasetOrDie(idSupplied);

		boolean canUpdateDataset = false;
		try {
			canUpdateDataset = permissionSvc.requestOn(createDataverseRequest(findUserOrDie()), dataset)
					.canIssue(UpdateDatasetVersionCommand.class);
		} catch (WrappedResponse ex) {
			logger.info(
					"Exception thrown while trying to figure out permissions while getting upload URLs for dataset id "
							+ dataset.getId() + ": " + ex.getLocalizedMessage());
			throw ex;
		}
		if (!canUpdateDataset) {
			return error(Response.Status.FORBIDDEN, "You are not permitted to upload files to this dataset.");
		}
		S3AccessIO<DataFile> s3io = FileUtil.getS3AccessForDirectUpload(dataset);
		if (s3io == null) {
			return error(Response.Status.NOT_FOUND,
					"Direct upload not supported for files in this dataset: " + dataset.getId());
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
@Path("mpupload")
public Response abortMPUpload(@QueryParam("globalid") String idSupplied, @QueryParam("storageidentifier") String storageidentifier, @QueryParam("uploadid") String uploadId) {
	try {
		Dataset dataset = datasetSvc.findByGlobalId(idSupplied);
		//Allow the API to be used within a session (e.g. for direct upload in the UI)
		User user =session.getUser();
		if (!user.isAuthenticated()) {
			try {
				user = findAuthenticatedUserOrDie();
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
@Path("mpupload")
public Response completeMPUpload(String partETagBody, @QueryParam("globalid") String idSupplied, @QueryParam("storageidentifier") String storageidentifier, @QueryParam("uploadid") String uploadId)  {
	try {
		Dataset dataset = datasetSvc.findByGlobalId(idSupplied);
		//Allow the API to be used within a session (e.g. for direct upload in the UI)
		User user =session.getUser();
		if (!user.isAuthenticated()) {
			try {
				user=findAuthenticatedUserOrDie();
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
			JsonReader jsonReader = Json.createReader(new StringReader(partETagBody));
			JsonObject object = jsonReader.readObject();
			jsonReader.close();
			for(String partNo : object.keySet()) {
				eTagList.add(new PartETag(Integer.parseInt(partNo), object.getString(partNo)));
			}
			for(PartETag et: eTagList) {
				logger.info("Part: " + et.getPartNumber() + " : " + et.getETag());
			}
		} catch (JsonException je) {
			logger.info("Unable to parse eTags from: " + partETagBody);
			throw new WrappedResponse(je, error( Response.Status.INTERNAL_SERVER_ERROR, "Could not complete multipart upload"));
		}
		try {
			S3AccessIO.completeMultipartUpload(idSupplied, storageidentifier, uploadId, eTagList);
		} catch (IOException io) {
			logger.warning("Multipart upload completion failed for uploadId: " + uploadId +" storageidentifier=" + storageidentifier + " globalId: " + idSupplied);
			logger.warning(io.getMessage());
			try {
				S3AccessIO.abortMultipartUpload(idSupplied, storageidentifier, uploadId);
			} catch (IOException e) {
				logger.severe("Also unable to abort the upload (and release the space on S3 for uploadId: " + uploadId +" storageidentifier=" + storageidentifier + " globalId: " + idSupplied);
				logger.severe(io.getMessage());
			}

			throw new WrappedResponse(io, error( Response.Status.INTERNAL_SERVER_ERROR, "Could not complete multipart upload")); 
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
    @Path("{id}/add")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addFileToDataset(@PathParam("id") String idSupplied,
                    @FormDataParam("jsonData") String jsonData,
                    @FormDataParam("file") InputStream fileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                    @FormDataParam("file") final FormDataBodyPart formDataBodyPart
                    ){

        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }

        // -------------------------------------
        // (1) Get the user from the API key
        // -------------------------------------
        User authUser;
        try {
            authUser = findUserOrDie();
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN,
                    BundleUtil.getStringFromBundle("file.addreplace.error.auth")
                    );
        }
        
        
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
        msgt("(api) jsonData: " +  jsonData);

        try {
            optionalFileParams = new OptionalFileParams(jsonData);
        } catch (DataFileTagException ex) {
            return error( Response.Status.BAD_REQUEST, ex.getMessage());            
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
				// ToDo - check that storageIdentifier is valid
				if (optionalFileParams.hasFileName()) {
					newFilename = optionalFileParams.getFileName();
					if (optionalFileParams.hasMimetype()) {
						newFileContentType = optionalFileParams.getMimeType();
					}
				}
			} else {
				return error(BAD_REQUEST,
						"You must upload a file or provide a storageidentifier, filename, and mimetype.");
			}
		} else {
			newFilename = contentDispositionHeader.getFileName();
			newFileContentType = formDataBodyPart.getMediaType().toString();
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
            return error(addFileHelper.getHttpErrorCode(), addFileHelper.getErrorMessagesAsString("\n"));
        }else{
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


    
    private void msg(String m){
        //System.out.println(m);
        logger.fine(m);
    }
    private void dashes(){
        msg("----------------");
    }
    private void msgt(String m){
        dashes(); msg(m); dashes();
    }
    
    
    public static <T> T handleVersion( String versionId, DsVersionHandler<T> hdl )
        throws WrappedResponse {
        switch (versionId) {
            case ":latest": return hdl.handleLatest();
            case ":draft": return hdl.handleDraft();
            case ":latest-published": return hdl.handleLatestPublished();
            default:
                try {
                    String[] versions = versionId.split("\\.");
                    switch (versions.length) {
                        case 1:
                            return hdl.handleSpecific(Long.parseLong(versions[0]), (long)0.0);
                        case 2:
                            return hdl.handleSpecific( Long.parseLong(versions[0]), Long.parseLong(versions[1]) );
                        default:
                            throw new WrappedResponse(error( Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'"));
                    }
                } catch ( NumberFormatException nfe ) {
                    throw new WrappedResponse( error( Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'") );
                }
        }
    }
    
    private DatasetVersion getDatasetVersionOrDie( final DataverseRequest req, String versionNumber, final Dataset ds, UriInfo uriInfo, HttpHeaders headers) throws WrappedResponse {
        DatasetVersion dsv = execCommand( handleVersion(versionNumber, new DsVersionHandler<Command<DatasetVersion>>(){

                @Override
                public Command<DatasetVersion> handleLatest() {
                    return new GetLatestAccessibleDatasetVersionCommand(req, ds);
                }

                @Override
                public Command<DatasetVersion> handleDraft() {
                    return new GetDraftDatasetVersionCommand(req, ds);
                }
  
                @Override
                public Command<DatasetVersion> handleSpecific(long major, long minor) {
                    return new GetSpecificPublishedDatasetVersionCommand(req, ds, major, minor);
                }

                @Override
                public Command<DatasetVersion> handleLatestPublished() {
                    return new GetLatestPublishedDatasetVersionCommand(req, ds);
                }
            }));
        if ( dsv == null || dsv.getId() == null ) {
            throw new WrappedResponse( notFound("Dataset version " + versionNumber + " of dataset " + ds.getId() + " not found") );
        }
        if (dsv.isReleased()) {
            MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountEntry(uriInfo, headers, dvRequestService, ds);
            mdcLogService.logEntry(entry);
        }
        return dsv;
    }
    
    @GET
    @Path("{identifier}/locks")
    public Response getLocks(@PathParam("identifier") String id, @QueryParam("type") DatasetLock.Reason lockType) {

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
    @Path("{identifier}/locks")
    public Response deleteLocks(@PathParam("identifier") String id, @QueryParam("type") DatasetLock.Reason lockType) {

        return response(req -> {
            try {
                AuthenticatedUser user = findAuthenticatedUserOrDie();
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
                        try {
                            indexService.indexDataset(dataset, true);
                        } catch (IOException | SolrServerException e) {
                            String failureLogText = "Post lock removal indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
                            failureLogText += "\r\n" + e.getLocalizedMessage();
                            LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);

                        }
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
                    try {
                        indexService.indexDataset(dataset, true);
                    } catch (IOException | SolrServerException e) {
                        String failureLogText = "Post lock removal indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
                        failureLogText += "\r\n" + e.getLocalizedMessage();
                        LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);

                    }
                    return ok("lock type " + lock.getReason() + " removed");
                }
                return ok("no lock type " + lockType + " on the dataset");
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }

        });

    }
    
    @POST
    @Path("{identifier}/lock/{type}")
    public Response lockDataset(@PathParam("identifier") String id, @PathParam("type") DatasetLock.Reason lockType) {
        return response(req -> {
            try {
                AuthenticatedUser user = findAuthenticatedUserOrDie();
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
                try {
                    indexService.indexDataset(dataset, true);
                } catch (IOException | SolrServerException e) {
                    String failureLogText = "Post add lock indexing failed. You can kickoff a re-index of this dataset with: \r\n curl http://localhost:8080/api/admin/index/datasets/" + dataset.getId().toString();
                    failureLogText += "\r\n" + e.getLocalizedMessage();
                    LoggingUtil.writeOnSuccessFailureLog(null, failureLogText, dataset);

                }

                return ok("dataset locked with lock type " + lockType);
            } catch (WrappedResponse wr) {
                return wr.getResponse();
            }

        });
    }
    
    @GET
    @Path("{id}/makeDataCount/citations")
    public Response getMakeDataCountCitations(@PathParam("id") String idSupplied) {
        
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            JsonArrayBuilder datasetsCitations = Json.createArrayBuilder();
            List<DatasetExternalCitations> externalCitations = datasetExternalCitationsService.getDatasetExternalCitationsByDataset(dataset);
            for (DatasetExternalCitations citation : externalCitations ){
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
    @Path("{identifier}/storagesize")
    public Response getStorageSize(@PathParam("identifier") String dvIdtf,  @QueryParam("includeCached") boolean includeCached,  
        @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {       
      
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasize.storage"),
                execCommand(new GetDatasetStorageSizeCommand(req, findDatasetOrDie(dvIdtf), includeCached,GetDatasetStorageSizeCommand.Mode.STORAGE, null)))));
    }
    
    @GET
    @Path("{identifier}/versions/{versionId}/downloadsize")
    public Response getDownloadSize(@PathParam("identifier") String dvIdtf, @PathParam("versionId") String version,   
        @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {       
      
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasize.download"),
                execCommand(new GetDatasetStorageSizeCommand(req, findDatasetOrDie(dvIdtf), false, GetDatasetStorageSizeCommand.Mode.DOWNLOAD, getDatasetVersionOrDie(req, version , findDatasetOrDie(dvIdtf), uriInfo, headers))))));
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
                monthYear = yyyymm + "-01";
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
        }
    }
    
    @GET
    @Path("{identifier}/storageDriver")
    public Response getFileStore(@PathParam("identifier") String dvIdtf,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse { 
        
        Dataset dataset; 
        
        try {
            dataset = findDatasetOrDie(dvIdtf);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "No such dataset");
        }
            
        return response(req -> ok(dataset.getEffectiveStorageDriverId()));
    }
    
    @PUT
    @Path("{identifier}/storageDriver")
    public Response setFileStore(@PathParam("identifier") String dvIdtf,
            String storageDriverLabel,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {
        
        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = findAuthenticatedUserOrDie();
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
    @Path("{identifier}/storageDriver")
    public Response resetFileStore(@PathParam("identifier") String dvIdtf,
            @Context UriInfo uriInfo, @Context HttpHeaders headers) throws WrappedResponse {
    
        // Superuser-only:
        AuthenticatedUser user;
        try {
            user = findAuthenticatedUserOrDie();
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
    @Path("{identifier}/timestamps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimestamps(@PathParam("identifier") String id) {

        Dataset dataset = null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            dataset = findDatasetOrDie(id);
            User u = findUserOrDie();
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
}
