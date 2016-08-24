package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DOIEZIdServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
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
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
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

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger LOGGER = Logger.getLogger(Datasets.class.getName());
    
    private static final String PERSISTENT_ID_KEY=":persistentId";
    
    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseServiceBean dataverseService;
    
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

        try {
            final DataverseRequest r = createDataverseRequest(findUserOrDie());

            Dataset retrieved = execCommand(new GetDatasetCommand(r, findDatasetOrDie(id)));
            DatasetVersion latest = execCommand(new GetLatestAccessibleDatasetVersionCommand(r, retrieved));
            final JsonObjectBuilder jsonbuilder = json(retrieved);

            return okResponse(jsonbuilder.add("latestVersion", (latest != null) ? json(latest) : null));
        } catch (WrappedResponse ex) {
            return ex.refineResponse("GETting dataset " + id + " failed.");
        }

    }
    
    /* An experimental method for creating a new dataset, from scratch, all from json metadata file
    @POST
    @Path("")
    public Response createDataset(String jsonBody) {
        Dataset importedDataset = null; 
        try {
            final DataverseRequest r = createDataverseRequest(findUserOrDie());
            
            StringReader rdr = new StringReader(jsonBody);
            JsonObject json = Json.createReader(rdr).readObject();
            JsonParser parser = new JsonParser(datasetfieldService, metadataBlockService, settingsService);
            parser.setLenient(true);
            Dataset ds = parser.parseDataset(json);

            
            Dataverse owner = dataverseService.find(1L);
            ds.setOwner(owner);
            ds.getLatestVersion().setDatasetFields(ds.getLatestVersion().initDatasetFields());

            // Check data against required contraints
            List<ConstraintViolation> violations = ds.getVersions().get(0).validateRequired();
            if (!violations.isEmpty()) {
                // For migration and harvest, add NA for missing required values
                for (ConstraintViolation v : violations) {
                    DatasetField f = ((DatasetField) v.getRootBean());
                    f.setSingleValue(DatasetField.NA_VALUE);
                }
            }

            
            Set<ConstraintViolation> invalidViolations = ds.getVersions().get(0).validate();
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            if (!invalidViolations.isEmpty()) {
                for (ConstraintViolation v : invalidViolations) {
                    DatasetFieldValue f = ((DatasetFieldValue) v.getRootBean());
                    boolean fixed = false;
                    boolean converted = false;
                    // TODO: Is this scrubbing something we want to continue doing? 
                    //
                    //if (settingsService.isTrueForKey(SettingsServiceBean.Key.ScrubMigrationData, false)) {
                    //    fixed = processMigrationValidationError(f, cleanupLog, metadataFile.getName());
                    //    converted = true;
                    //    if (fixed) {
                    //        Set<ConstraintViolation<DatasetFieldValue>> scrubbedViolations = validator.validate(f);
                    //        if (!scrubbedViolations.isEmpty()) {
                    //            fixed = false;
                    //        }
                    //    }
                    //}
                    if (!fixed) {
                        String msg = "Field: " + f.getDatasetField().getDatasetFieldType().getDisplayName() + "; "
                                + "Invalid value:  '" + f.getValue() + "'" + " Converted Value:'" + DatasetField.NA_VALUE + "'";
                        Logger.getLogger(Datasets.class.getName()).log(Level.INFO, null, msg);
                        f.setValue(DatasetField.NA_VALUE);
                    }
                }
            }

            //ds.setHarvestedFrom(harvestingClient);
            //ds.setHarvestIdentifier(harvestIdentifier);
            
                importedDataset = engineSvc.submit(new CreateDatasetCommand(ds, r, false, ImportUtil.ImportType.HARVEST));

        } catch (JsonParseException ex) {
            Logger.getLogger(Datasets.class.getName()).log(Level.INFO, null, "Error parsing datasetVersion: " + ex.getMessage());
            return errorResponse(Response.Status.NOT_FOUND, "error parsing dataset");
        } catch (CommandException ex) {
            Logger.getLogger(Datasets.class.getName()).log(Level.INFO, null, "Error excuting Create dataset command: " + ex.getMessage());  
            return errorResponse(Response.Status.NOT_FOUND, "error executing create dataset command");
        } catch (WrappedResponse ex) {
            return ex.refineResponse("Error: "+ex.getWrappedMessageWhenJson());
        }
        
        final JsonObjectBuilder jsonbuilder = json(importedDataset);

        return okResponse(jsonbuilder.add("latestVersion", json(importedDataset.getLatestVersion())));
    } */
    
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
                return errorResponse(Response.Status.NOT_FOUND, "A dataset with the persistentId " + persistentId + " could not be found.");
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
            
            LOGGER.fine("xml to return: " + xml);
            String mediaType = MediaType.TEXT_PLAIN;
            if (instance.isXMLFormat(exporter)){
                mediaType = MediaType.APPLICATION_XML;
            }
            return Response.ok()
                    .entity(xml)
                    .type(mediaType).
                    build();
        } catch (Exception wr) {
            return errorResponse(Response.Status.FORBIDDEN, "Export Failed");
        }
    }

	@DELETE
	@Path("{id}")
	public Response deleteDataset( @PathParam("id") String id) {
		
		try {
			execCommand( new DeleteDatasetCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(id)));
			return okResponse("Dataset " + id + " deleted");
			
		} catch (WrappedResponse ex) {
			return ex.refineResponse( "Failed to delete dataset " + id );
		}
		
	}
        
	@DELETE
	@Path("{id}/destroy")
	public Response destroyDataset( @PathParam("id") String id) {
		try {
			execCommand( new DestroyDatasetCommand(findDatasetOrDie(id), createDataverseRequest(findUserOrDie()) ));
			return okResponse("Dataset " + id + " destroyed");
			
		} catch (WrappedResponse ex) {
			return ex.refineResponse( "Failed to detroy dataset " + id );
		}		
	}
        
	@PUT
	@Path("{id}/citationdate")
	public Response setCitationDate( @PathParam("id") String id, String dsfTypeName) {
            try {
                if ( dsfTypeName.trim().isEmpty() ){
                    throw new WrappedResponse( badRequest("Please provide a dataset field type in the requst body.") );
                }
                DatasetFieldType dsfType = null;
                if (!":publicationDate".equals(dsfTypeName)) {
                    dsfType = datasetFieldSvc.findByName(dsfTypeName);
                    if (dsfType == null) {
                        throw new WrappedResponse( badRequest("Dataset Field Type Name " + dsfTypeName + " not found.") );
                    }
                }

                execCommand(new SetDatasetCitationDateCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(id), dsfType));
                
                return okResponse("Citation Date for dataset " + id + " set to: " + (dsfType != null ? dsfType.getDisplayName() : "default"));

            } catch (WrappedResponse ex) {
                return ex.refineResponse("Unable to set citation date for dataset " + id + ".");
            }
	}    
    
	@DELETE
	@Path("{id}/citationdate")
	public Response useDefaultCitationDate( @PathParam("id") String id) {
            try {
                execCommand(new SetDatasetCitationDateCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(id), null));
                return okResponse("Citation Date for dataset " + id + " set to default");
            } catch (WrappedResponse ex) {
                return ex.refineResponse("Unable to restore default citation date for dataset " + id + ".");
            }
	}         
	
	@GET
	@Path("{id}/versions")
    public Response listVersions( @PathParam("id") String id ) {
        try {
            JsonArrayBuilder bld = Json.createArrayBuilder();
            for ( DatasetVersion dsv : execCommand(
                    new ListVersionsCommand(
                            createDataverseRequest(findUserOrDie()), findDatasetOrDie(id)) ) ) {
                bld.add( json(dsv) );
            }
            return okResponse( bld );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
	
	@GET
	@Path("{id}/versions/{versionId}")
    public Response getVersion( @PathParam("id") String datasetId, @PathParam("versionId") String versionId) {
		
        try {
            DatasetVersion dsv = getDatasetVersionOrDie(createDataverseRequest(findUserOrDie()), versionId, findDatasetOrDie(datasetId));
            
            return (dsv == null || dsv.getId() == null) ? notFound("Dataset version not found")
                                                        : okResponse(json(dsv));
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
	
    @GET
	@Path("{id}/versions/{versionId}/files")
    public Response getVersionFiles( @PathParam("id") String datasetId, @PathParam("versionId") String versionId) {
		
        try {
            
            return okResponse( jsonFileMetadatas(
                                            getDatasetVersionOrDie(createDataverseRequest(findUserOrDie()), 
                                                                versionId, 
                                                                findDatasetOrDie(datasetId)).getFileMetadatas()));
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @GET
	@Path("{id}/versions/{versionId}/metadata")
    public Response getVersionMetadata( @PathParam("id") String datasetId, @PathParam("versionId") String versionId) {
		
        try {
            return okResponse(
                    jsonByBlocks(
                        getDatasetVersionOrDie( createDataverseRequest(findUserOrDie()), versionId, findDatasetOrDie(datasetId) )
                                .getDatasetFields()));
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @GET
	@Path("{id}/versions/{versionNumber}/metadata/{block}")
    public Response getVersionMetadataBlock( @PathParam("id") String datasetId, 
                                             @PathParam("versionNumber") String versionNumber, 
                                             @PathParam("block") String blockName ) {
		
        try {
            DatasetVersion dsv = getDatasetVersionOrDie(createDataverseRequest(findUserOrDie()), versionNumber, findDatasetOrDie(datasetId) );
            
            Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(dsv.getDatasetFields());
            for ( Map.Entry<MetadataBlock, List<DatasetField>> p : fieldsByBlock.entrySet() ) {
                if ( p.getKey().getName().equals(blockName) ) {
                    return okResponse( json(p.getKey(), p.getValue()) );
                }
            }
            return notFound("metadata block named " + blockName + " not found");
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
		
    }
	
    @DELETE
	@Path("{id}/versions/{versionId}")
	public Response deleteDraftVersion( @PathParam("id") String id,  @PathParam("versionId") String versionId ){
        if ( ! ":draft".equals(versionId) ) {
            return badRequest("Only the :draft version can be deleted");
        }
        
        try {
            execCommand( new DeleteDatasetVersionCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(id)) );
            return okResponse("Draft version of dataset " + id + " deleted");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
        
    
    @GET
    @Path("{id}/modifyRegistration")
    public Response updateDatasetTargetURL(@PathParam("id") String id ) {

        try {
            execCommand(new UpdateDatasetTargetURLCommand(findDatasetOrDie(id), createDataverseRequest(findUserOrDie())));
            return okResponse("Dataset " + id + " target url updated");

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

    }
    
    @GET
    @Path("/modifyRegistrationAll")
    public Response updateDatasetTargetURLAll() {
        List<Dataset> allDatasets = datasetService.findAll();

        for (Dataset ds : allDatasets){           
   
            try {
                execCommand(new UpdateDatasetTargetURLCommand(findDatasetOrDie(ds.getId().toString()), createDataverseRequest(findUserOrDie())));
            } catch (WrappedResponse ex) {
                Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        return okResponse("Update All Dataset target url completed");
    }
  
    @PUT
	@Path("{id}/versions/{versionId}")
	public Response updateDraftVersion( String jsonBody, @PathParam("id") String id,  @PathParam("versionId") String versionId ){
        
        if ( ! ":draft".equals(versionId) ) {
            return errorResponse( Response.Status.BAD_REQUEST, "Only the :draft version can be updated");
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
            return okResponse( json(managedVersion) );
                    
        } catch (JsonParseException ex) {
            LOGGER.log(Level.SEVERE, "Semantic error parsing dataset version Json: " + ex.getMessage(), ex);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing dataset version: " + ex.getMessage() );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
            
        }
    }
    
    @GET
    @Path("{id}/actions/:publish") 
    public Response publishDataset( @PathParam("id") String id, @QueryParam("type") String type ) {
        try {
            if ( type == null ) {
                return errorResponse( Response.Status.BAD_REQUEST, "Missing 'type' parameter (either 'major' or 'minor').");
            }
            
            type = type.toLowerCase();
            boolean isMinor;
            switch ( type ) {
                case "minor": isMinor = true; break;
                case "major": isMinor = false; break;
                default: return errorResponse( Response.Status.BAD_REQUEST, "Illegal 'type' parameter value '" + type + "'. It needs to be either 'major' or 'minor'.");
            }
            long dsId;
            try {
                dsId = Long.parseLong(id);
            } catch ( NumberFormatException nfe ) {
                return errorResponse( Response.Status.BAD_REQUEST, "Bad dataset id. Please provide a number.");
            }
            
            Dataset ds = datasetService.find(dsId);
            
            return ( ds == null ) ? notFound("Can't find dataset with id '" + id + "'")
                                  : okResponse( json(execCommand(new PublishDatasetCommand(ds, 
                                                                            createDataverseRequest(findAuthenticatedUserOrDie()),
                                                                            isMinor))) );
        
        }  catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @Path("{id}/links")
    public Response getLinks(@PathParam("id") String idSupplied ) {
        try {
            User u = findUserOrDie();
            if (!u.isSuperuser()) {
                return errorResponse(Response.Status.FORBIDDEN, "Not a superuser");
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
            return okResponse(response);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
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
                            throw new WrappedResponse(errorResponse( Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'"));
                    }
                } catch ( NumberFormatException nfe ) {
                    throw new WrappedResponse( errorResponse( Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'") );
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
    
    Dataset findDatasetOrDie( String id ) throws WrappedResponse {
        Dataset dataset;
        LOGGER.info("Looking for dataset " + id);
        if ( id.equals(PERSISTENT_ID_KEY) ) {
            String persistentId = getRequestParameter(PERSISTENT_ID_KEY.substring(1));
            LOGGER.info("Looking for dataset " + persistentId);
            if ( persistentId == null ) {
                throw new WrappedResponse( 
                        badRequest("When accessing a dataset based on persistent id, "
                                + "a " + PERSISTENT_ID_KEY.substring(1) + " query parameter "
                                + "must be present"));
            }
            dataset = datasetService.findByGlobalId(persistentId);
            if (dataset == null) {
                throw new WrappedResponse( notFound("dataset " + persistentId + " not found") );
            }   
            return dataset;
            
        } else {
            try {
                dataset = datasetService.find( Long.parseLong(id) );
                if (dataset == null) {
                    throw new WrappedResponse( notFound("dataset " + id + " not found") );
                }   
                return dataset;
            } catch ( NumberFormatException nfe ) {
                throw new WrappedResponse( 
                        badRequest("Bad dataset id number: '" + id + "'"));
            }
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
            return errorResponse(Response.Status.FORBIDDEN, "Disabled");
        }
        try {
            User u = findUserOrDie();
            if (!u.isSuperuser()) {
                return errorResponse(Response.Status.FORBIDDEN, "Not a superuser");
            }

            LOGGER.fine("looking up " + persistentId);
            Dataset dataset = datasetService.findByGlobalId(persistentId);
            if (dataset == null) {
                return errorResponse(Response.Status.NOT_FOUND, "A dataset with the persistentId " + persistentId + " could not be found.");
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
            LOGGER.fine("xml to return: " + xml);

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
            return errorResponse(Response.Status.FORBIDDEN, "This is only for API tests.");
        }
        try {
            Dataset dataset = findDatasetOrDie(id);
            RoleAssignee assignee = findAssignee(userOrGroup);
            if (assignee == null) {
                return errorResponse(Response.Status.BAD_REQUEST, "Assignee not found");
            }
            DataverseRole theRole = rolesSvc.findBuiltinRoleByAlias("admin");
            String privateUrlToken = null;
            return okResponse(
                    json(execCommand(new AssignRoleCommand(assignee, theRole, dataset, createDataverseRequest(findUserOrDie()), privateUrlToken))));
        } catch (WrappedResponse ex) {
            LOGGER.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
            return ex.getResponse();
        }
    }

    @GET
    @Path("{identifier}/assignments")
    public Response getAssignments(@PathParam("identifier") String id) {
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for (RoleAssignment ra : execCommand(new ListRoleAssignments(createDataverseRequest(findUserOrDie()), findDatasetOrDie(id)))) {
                jab.add(json(ra));
            }
            return okResponse(jab);
        } catch (WrappedResponse ex) {
            LOGGER.log(Level.WARNING, "Can't list assignments: {0}", ex.getMessage());
            return ex.getResponse();
        }
    }

    @GET
    @Path("{id}/privateUrl")
    public Response getPrivateUrlData(@PathParam("id") String idSupplied) {
        try {
            PrivateUrl privateUrl = execCommand(new GetPrivateUrlCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(idSupplied)));
            if (privateUrl != null) {
                return okResponse(json(privateUrl));
            } else {
                return errorResponse(Response.Status.NOT_FOUND, "Private URL not found.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @Path("{id}/privateUrl")
    public Response createPrivateUrl(@PathParam("id") String idSupplied) {
        try {
            return okResponse(json(execCommand(new CreatePrivateUrlCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(idSupplied)))));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @Path("{id}/privateUrl")
    public Response deletePrivateUrl(@PathParam("id") String idSupplied) {
        try {
            User user = findUserOrDie();
            Dataset dataset = findDatasetOrDie(idSupplied);
            PrivateUrl privateUrl = execCommand(new GetPrivateUrlCommand(createDataverseRequest(user), dataset));
            if (privateUrl != null) {
                execCommand(new DeletePrivateUrlCommand(createDataverseRequest(user), dataset));
                return okResponse("Private URL deleted.");
            } else {
                return errorResponse(Response.Status.NOT_FOUND, "No Private URL to delete.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

}
