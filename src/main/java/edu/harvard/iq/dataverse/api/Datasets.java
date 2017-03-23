package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetSpecificPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDraftDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestAccessibleDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.ListVersionsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.SetDatasetCitationDateCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetTargetURLCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());
    
    private static final String PERSISTENT_ID_KEY=":persistentId";
    
    @Inject DataverseSession session;    

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DOIEZIdServiceBean doiEZIdServiceBean;

    @EJB
    DDIExportServiceBean ddiExportService;
    
    @EJB
    SystemConfig systemConfig;
    
    @EJB
    DatasetFieldServiceBean datasetfieldService;

    @EJB
    MetadataBlockServiceBean metadataBlockService;
    
    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    DataFileServiceBean fileService;

    @EJB
    IngestServiceBean ingestService;

    @EJB
    EjbDataverseEngine commandEngine;
     
    /**
     * Used to consolidate the way we parse and handle dataset versions.
     * @param <T> 
     */
    private interface DsVersionHandler<T> {
        T handleLatest();
        T handleDraft();
        T handleSpecific( long major, long minor );
        T handleLatestPublished();
    }
	
    @GET
    @Path("{id}")
    public Response getDataset(@PathParam("id") String id) {
        return response( req -> {
            final Dataset retrieved = execCommand(new GetDatasetCommand(req, findDatasetOrDie(id)));
            final DatasetVersion latest = execCommand(new GetLatestAccessibleDatasetVersionCommand(req, retrieved));
            final JsonObjectBuilder jsonbuilder = json(retrieved);

            return allowCors(ok(jsonbuilder.add("latestVersion", (latest != null) ? json(latest) : null)));
        });
    }
    
    // TODO: 
    // This API call should, ideally, call findUserOrDie() and the GetDatasetCommand 
    // to obtain the dataset that we are trying to export - which would handle
    // Auth in the process... For now, Auth isn't necessary - since export ONLY 
    // WORKS on published datasets, which are open to the world. -- L.A. 4.5
    
    @GET
    @Path("/export")
    @Produces({"application/xml", "application/json"})
    public Response exportDataset(@QueryParam("persistentId") String persistentId, @QueryParam("exporter") String exporter) {

        try {
            Dataset dataset = datasetService.findByGlobalId(persistentId);
            if (dataset == null) {
                return error(Response.Status.NOT_FOUND, "A dataset with the persistentId " + persistentId + " could not be found.");
            }
            
            ExportService instance = ExportService.getInstance();
            
            String xml = instance.getExportAsString(dataset, exporter);
            // I'm wondering if this going to become a performance problem 
            // with really GIANT datasets,
            // the fact that we are passing these exports, blobs of JSON, and, 
            // especially, DDI XML as complete strings. It would be nicer 
            // if we could stream instead - and the export service already can
            // give it to as as a stream; then we could start sending the 
            // output to the remote client as soon as we got the first bytes, 
            // without waiting for the whole thing to be generated and buffered... 
            // (the way Access API streams its output). 
            // -- L.A., 4.5
            
            logger.fine("xml to return: " + xml);
            String mediaType = MediaType.TEXT_PLAIN;
            if (instance.isXMLFormat(exporter)){
                mediaType = MediaType.APPLICATION_XML;
            }
            return allowCors(Response.ok()
                    .entity(xml)
                    .type(mediaType).
                    build());
        } catch (Exception wr) {
            return error(Response.Status.FORBIDDEN, "Export Failed");
        }
    }

	@DELETE
	@Path("{id}")
	public Response deleteDataset( @PathParam("id") String id) {
		return response( req -> {
			execCommand( new DeleteDatasetCommand(req, findDatasetOrDie(id)));
			return ok("Dataset " + id + " deleted");
        });
	}
        
	@DELETE
	@Path("{id}/destroy")
	public Response destroyDataset( @PathParam("id") String id) {
		return response( req -> {
			execCommand( new DestroyDatasetCommand(findDatasetOrDie(id), req) );
			return ok("Dataset " + id + " destroyed");
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
        return allowCors(response( req -> 
             ok( execCommand( new ListVersionsCommand(req, findDatasetOrDie(id)) )
                                .stream()
                                .map( d -> json(d) )
                                .collect(toJsonArray()))));
    }
	
	@GET
	@Path("{id}/versions/{versionId}")
    public Response getVersion( @PathParam("id") String datasetId, @PathParam("versionId") String versionId) {
        return allowCors(response( req -> {
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId));            
            return (dsv == null || dsv.getId() == null) ? notFound("Dataset version not found")
                                                        : ok(json(dsv));
        }));
    }
	
    @GET
	@Path("{id}/versions/{versionId}/files")
    public Response getVersionFiles( @PathParam("id") String datasetId, @PathParam("versionId") String versionId) {
        return allowCors(response( req -> ok( jsonFileMetadatas(
                         getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId)).getFileMetadatas()))));
    }
    
    @GET
    @Path("{id}/versions/{versionId}/metadata")
    public Response getVersionMetadata( @PathParam("id") String datasetId, @PathParam("versionId") String versionId) {
		return allowCors(response( req -> ok(
                    jsonByBlocks(
                        getDatasetVersionOrDie(req, versionId, findDatasetOrDie(datasetId) )
                                .getDatasetFields()))));
    }
    
    @GET
	@Path("{id}/versions/{versionNumber}/metadata/{block}")
    public Response getVersionMetadataBlock( @PathParam("id") String datasetId, 
                                             @PathParam("versionNumber") String versionNumber, 
                                             @PathParam("block") String blockName ) {
		
        return allowCors(response( req -> {
            DatasetVersion dsv = getDatasetVersionOrDie(req, versionNumber, findDatasetOrDie(datasetId) );
            
            Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(dsv.getDatasetFields());
            for ( Map.Entry<MetadataBlock, List<DatasetField>> p : fieldsByBlock.entrySet() ) {
                if ( p.getKey().getName().equals(blockName) ) {
                    return ok( json(p.getKey(), p.getValue()) );
                }
            }
            return notFound("metadata block named " + blockName + " not found");
        }));
    }
	
    @DELETE
	@Path("{id}/versions/{versionId}")
	public Response deleteDraftVersion( @PathParam("id") String id,  @PathParam("versionId") String versionId ){
        if ( ! ":draft".equals(versionId) ) {
            return badRequest("Only the :draft version can be deleted");
        }

        return response( req -> {
            execCommand( new DeleteDatasetVersionCommand(req, findDatasetOrDie(id)) );
            return ok("Draft version of dataset " + id + " deleted");
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
    
    @GET
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
            boolean updateDraft = ds.getLatestVersion().isDraft();
            DatasetVersion managedVersion = execCommand( updateDraft
                                                             ? new UpdateDatasetVersionCommand(req, incomingVersion)
                                                             : new CreateDatasetVersionCommand(req, ds, incomingVersion));
            return ok( json(managedVersion) );
                    
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset version Json: " + ex.getMessage(), ex);
            return error( Response.Status.BAD_REQUEST, "Error parsing dataset version: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
    }
    
    /**
     * @deprecated This was shipped as a GET but should have been a POST, see https://github.com/IQSS/dataverse/issues/2431
     */
    @GET
    @Path("{id}/actions/:publish")
    @Deprecated
    public Response publishDataseUsingGetDeprecated( @PathParam("id") String id, @QueryParam("type") String type ) {
        logger.info("publishDataseUsingGetDeprecated called on id " + id + ". Encourage use of POST rather than GET, which is deprecated.");
        return publishDataset(id, type);
    }

    @POST
    @Path("{id}/actions/:publish")
    public Response publishDataset(@PathParam("id") String id, @QueryParam("type") String type) {
        try {
            if (type == null) {
                return error(Response.Status.BAD_REQUEST, "Missing 'type' parameter (either 'major' or 'minor').");
            }

            type = type.toLowerCase();
            boolean isMinor;
            switch (type) {
                case "minor":
                    isMinor = true;
                    break;
                case "major":
                    isMinor = false;
                    break;
                default:
                    return error(Response.Status.BAD_REQUEST, "Illegal 'type' parameter value '" + type + "'. It needs to be either 'major' or 'minor'.");
            }

            Dataset ds = findDatasetOrDie(id);

            return ok(json(execCommand(new PublishDatasetCommand(ds,
                    createDataverseRequest(findAuthenticatedUserOrDie()),
                    isMinor))));

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
     * @todo Implement this for real as part of
     * https://github.com/IQSS/dataverse/issues/2579
     */
    @GET
    @Path("ddi")
    @Produces({"application/xml", "application/json"})
    @Deprecated
    public Response getDdi(@QueryParam("id") long id, @QueryParam("persistentId") String persistentId, @QueryParam("dto") boolean dto) {
        boolean ddiExportEnabled = systemConfig.isDdiExportEnabled();
        if (!ddiExportEnabled) {
            return error(Response.Status.FORBIDDEN, "Disabled");
        }
        try {
            User u = findUserOrDie();
            if (!u.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Not a superuser");
            }

            logger.fine("looking up " + persistentId);
            Dataset dataset = datasetService.findByGlobalId(persistentId);
            if (dataset == null) {
                return error(Response.Status.NOT_FOUND, "A dataset with the persistentId " + persistentId + " could not be found.");
            }

            String xml = "<codeBook>XML_BEING_COOKED</codeBook>";
            if (dto) {
                /**
                 * @todo We can only assume that this should not be hard-coded
                 * to getLatestVersion
                 */
                final JsonObjectBuilder datasetAsJson = jsonAsDatasetDto(dataset.getLatestVersion());
                xml = DdiExportUtil.datasetDtoAsJson2ddi(datasetAsJson.toString());
            } else {
                OutputStream outputStream = new ByteArrayOutputStream();
                ddiExportService.exportDataset(dataset.getId(), outputStream, null, null);
                xml = outputStream.toString();
            }
            logger.fine("xml to return: " + xml);

            return Response.ok()
                    .entity(xml)
                    .type(MediaType.APPLICATION_XML).
                    build();
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    /**
     * @todo Make this real. Currently only used for API testing. Copied from
     * the equivalent API endpoint for dataverses and simplified with values
     * hard coded.
     */
    @POST
    @Path("{identifier}/assignments")
    public Response createAssignment(String userOrGroup, @PathParam("identifier") String id, @QueryParam("key") String apiKey) {
        boolean apiTestingOnly = true;
        if (apiTestingOnly) {
            return error(Response.Status.FORBIDDEN, "This is only for API tests.");
        }
        try {
            Dataset dataset = findDatasetOrDie(id);
            RoleAssignee assignee = findAssignee(userOrGroup);
            if (assignee == null) {
                return error(Response.Status.BAD_REQUEST, "Assignee not found");
            }
            DataverseRole theRole = rolesSvc.findBuiltinRoleByAlias("admin");
            String privateUrlToken = null;
            return ok(
                    json(execCommand(new AssignRoleCommand(assignee, theRole, dataset, createDataverseRequest(findUserOrDie()), privateUrlToken))));
        } catch (WrappedResponse ex) {
            logger.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
            return ex.getResponse();
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
            for (DatasetThumbnail datasetThumbnail : DatasetUtil.getThumbnailCandidates(dataset, considerDatasetLogoAsCandidate)) {
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
    public InputStream getDatasetThumbnail(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            return DatasetUtil.getThumbnailAsInputStream(dataset);
        } catch (WrappedResponse ex) {
            return null;
        }
    }

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

    /**
     * Add a File to an existing Dataset
     * 
     * @param idSupplied
     * @param datasetId
     * @param jsonData
     * @param testFileInputStream
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

          
        // -------------------------------------
        // (1) Get the user from the API key
        // -------------------------------------
        User authUser;
        try {
            authUser = findUserOrDie();
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN,
                    ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.auth")
                    );
        }
        
        // -------------------------------------
        // (2) Get the Dataset Id
        //  
        // -------------------------------------
        Dataset dataset;
        
        Long datasetId;
        try {
            dataset = findDatasetOrDie(idSupplied);
        } catch (WrappedResponse wr) {
            return wr.getResponse();           
        }
        
               
        // -------------------------------------
        // (3) Get the file name and content type
        // -------------------------------------
        String newFilename = contentDispositionHeader.getFileName();
        String newFileContentType = formDataBodyPart.getMediaType().toString();
      
        
        // (2a) Load up optional params via JSON
        //---------------------------------------
        OptionalFileParams optionalFileParams = null;
        msgt("(api) jsonData: " +  jsonData);

        try {
            optionalFileParams = new OptionalFileParams(jsonData);
        } catch (DataFileTagException ex) {
            return error( Response.Status.BAD_REQUEST, ex.getMessage());            
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
                                fileInputStream,
                                optionalFileParams);


        if (addFileHelper.hasError()){
            return error(addFileHelper.getHttpErrorCode(), addFileHelper.getErrorMessagesAsString("\n"));
        }else{
            String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.add");        
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
                return ok(addFileHelper.getSuccessResultAsJsonObjectBuilder());
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
    

   
    private Dataset findDatasetOrDie(String id) throws WrappedResponse {
        Dataset dataset;
        if (id.equals(PERSISTENT_ID_KEY)) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            if (persistentId == null) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset_id_is_null", Collections.singletonList(PERSISTENT_ID_KEY.substring(1)))));
            }
            dataset = datasetService.findByGlobalId(persistentId);
            if (dataset == null) {
                throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.persistentId", Collections.singletonList(persistentId))));
            }
            return dataset;

        } else {
            try {
                dataset = datasetService.find(Long.parseLong(id));
                if (dataset == null) {
                    throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.id", Collections.singletonList(id))));
                }
                return dataset;
            } catch (NumberFormatException nfe) {
                throw new WrappedResponse(
                        badRequest(BundleUtil.getStringFromBundle("find.dataset.error.dataset.not.found.bad.id", Collections.singletonList(id))));
            }
        }
    }
    
    
    private <T> T handleVersion( String versionId, DsVersionHandler<T> hdl )
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
    
    private DatasetVersion getDatasetVersionOrDie( final DataverseRequest req, String versionNumber, final Dataset ds ) throws WrappedResponse {
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
        return dsv;
    }
    
}
