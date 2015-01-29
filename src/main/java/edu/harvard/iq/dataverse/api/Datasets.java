package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetSpecificPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDraftDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestAccessibleDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListVersionsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseServiceBean dataverseService;

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
    public Response getDataset( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
        
        try {
            User u = findUserOrDie(apiKey);
            Dataset ds = datasetService.find(id);
            if (ds == null) return errorResponse( Response.Status.NOT_FOUND, "dataset not found");
            
            Dataset retrieved = execCommand(new GetDatasetCommand(u, ds), "Getting dataset");
            DatasetVersion latest = execCommand(new GetLatestAccessibleDatasetVersionCommand(u, ds), "Getting latest dataset version");
            final JsonObjectBuilder jsonbuilder = json(retrieved);
            
            return okResponse(jsonbuilder.add("latestVersion", (latest != null) ? json(latest) : null));
        } catch ( WrappedResponse ex ) {
			return ex.getResponse();
		}
        
    }
	
	@DELETE
	@Path("{id}")
	public Response deleteDataset( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		
		try {
            User u = findUserOrDie(apiKey);
            if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");

            Dataset ds = datasetService.find(id);
            if (ds == null) return errorResponse( Response.Status.NOT_FOUND, "dataset not found");
			execCommand( new DeleteDatasetCommand(ds, u), "Delete dataset " + id);
			return okResponse("Dataset " + id + " deleted");
			
		} catch (WrappedResponse ex) {
			return ex.getResponse();
		}
		
	}
        
	@DELETE
	@Path("{id}/destroy")
	public Response destroyDataset( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		
		try {
            User u = findUserOrDie(apiKey);
            if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");

            Dataset ds = datasetService.find(id);
            if (ds == null) return errorResponse( Response.Status.NOT_FOUND, "dataset not found");
			execCommand( new DestroyDatasetCommand(ds, u), "Destroy dataset " + id);
			return okResponse("Dataset " + id + " destroyed");
			
		} catch (WrappedResponse ex) {
			return ex.getResponse();
		}
		
	}        
	
	@GET
	@Path("{id}/versions")
    public Response listVersions( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		
        try {
            User u = findUserOrDie(apiKey);

            Dataset ds = datasetService.find(id);
            if (ds == null) return notFound("dataset not found");
            List<DatasetVersion> retrieved = execCommand(new ListVersionsCommand(u, ds), "Listing Dataset versions");
            JsonArrayBuilder bld = Json.createArrayBuilder();
            for ( DatasetVersion dsv : retrieved ) {
                bld.add( json(dsv) );
            }
            return okResponse( bld );
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
	
	@GET
	@Path("{id}/versions/{versionId}")
    public Response getVersion( @PathParam("id") Long datasetId, @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ) {
		
        try {
            final User u = findUserOrDie(apiKey);
            final Dataset ds = datasetService.find(datasetId);
            if (ds == null) return errorResponse(Response.Status.NOT_FOUND, "dataset " + datasetId + " not found");
            Command<DatasetVersion> cmd = handleVersion( versionId, new DsVersionHandler<Command<DatasetVersion>>(){

                @Override
                public Command<DatasetVersion> handleLatest() {
                    return new GetLatestAccessibleDatasetVersionCommand(u, ds);
                }

                @Override
                public Command<DatasetVersion> handleDraft() {
                    return new GetDraftDatasetVersionCommand(u, ds);
                }

                @Override
                public Command<DatasetVersion> handleSpecific(long major, long minor) {
                    return new GetSpecificPublishedDatasetVersionCommand(u, ds, major, minor);
                }

                @Override
                public Command<DatasetVersion> handleLatestPublished() {
                    return new GetLatestPublishedDatasetVersionCommand(u, ds);
                }
            });
            DatasetVersion dsv = execCommand(cmd, versionId);
            return (dsv == null || dsv.getId() == null)
                    ? errorResponse(Response.Status.NOT_FOUND, "Dataset version not found")
                    : okResponse(json(dsv));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @GET
	@Path("{id}/versions/{versionId}/metadata")
    public Response getVersionMetadata( @PathParam("id") Long datasetId, @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ) {
		
        try {
            User u = findUserOrDie(apiKey);
            Dataset ds = datasetService.find(datasetId);
            if (ds == null) return errorResponse(Response.Status.NOT_FOUND, "dataset " + datasetId + " not found");
            DatasetVersion dsv = getDatasetVersion( u, versionId, ds );
            return (dsv==null)
                    ? errorResponse(Response.Status.NOT_FOUND, "dataset version not found")
                    : okResponse( JsonPrinter.jsonByBlocks(dsv.getDatasetFields())  );
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
		
    }
    
    @GET
	@Path("{id}/versions/{versionNumber}/metadata/{block}")
    public Response getVersionMetadataBlock( @PathParam("id") Long datasetId, 
                                             @PathParam("versionNumber") String versionNumber, 
                                             @PathParam("block") String blockName,
                                             @QueryParam("key") String apiKey ) {
		
        try {
            User u = findUserOrDie(apiKey);
            final Dataset ds = datasetService.find(datasetId);
            if (ds == null) return errorResponse(Response.Status.NOT_FOUND, "dataset " + datasetId + " not found");
            DatasetVersion dsv = getDatasetVersion(u, versionNumber, ds);
            if ( dsv == null ) return errorResponse(Response.Status.NOT_FOUND, "dataset version not found");
            Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(dsv.getDatasetFields());
            for ( Map.Entry<MetadataBlock, List<DatasetField>> p : fieldsByBlock.entrySet() ) {
                if ( p.getKey().getName().equals(blockName) ) {
                    return okResponse( JsonPrinter.json(p.getKey(), p.getValue()) );
                }
            }
            return notFound("metadata block named " + blockName + " not found");
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
		
    }
	
    @DELETE
	@Path("{id}/versions/{versionId}")
	public Response deleteDraftVersion( @PathParam("id") Long id,  @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ){
        if ( ! ":draft".equals(versionId) ) 
            return errorResponse( Response.Status.BAD_REQUEST, "Only the :draft version can be deleted");
        
        
        try {
            User u = findUserOrDie(apiKey);
            Dataset ds = datasetService.find(id);
            if ( ds == null ) return notFound("Can't find dataset with id '" + id + "'");
            execCommand( new DeleteDatasetVersionCommand(u,ds), "Deleting draft version of dataset " + id );
            return okResponse("Draft version of dataset " + id + " deleted");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @PUT
	@Path("{id}/versions/{versionId}")
	public Response updateDraftVersion( String jsonBody, @PathParam("id") Long id,  @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ){
        
        if ( ! ":draft".equals(versionId) ) 
            return errorResponse( Response.Status.BAD_REQUEST, "Only the :draft version can be updated");
        
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            User u = findUserOrDie(apiKey);
            Dataset ds = datasetService.find(id);
            if ( ds == null ) return notFound("Can't find dataset with id '" + id + "'");
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
            DatasetVersion managedVersion = engineSvc.submit( updateDraft
                                                                ? new UpdateDatasetVersionCommand(u, incomingVersion)
                                                                : new CreateDatasetVersionCommand(u, ds, incomingVersion) );
            return okResponse( json(managedVersion) );
                    
        } catch (CommandException ex) {
            logger.log(Level.SEVERE, "Error executing CreateDatasetVersionCommand: " + ex.getMessage(), ex);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + ex.getMessage() );
            
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset version Json: " + ex.getMessage(), ex);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing dataset version: " + ex.getMessage() );
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @POST
    @Path("{id}/actions/:publish") 
    public Response publishDataset( @PathParam("id") String id, @QueryParam("type") String type, @QueryParam("key") String apiKey ) {
        try {
            if ( type == null ) return errorResponse( Response.Status.BAD_REQUEST, "Missing 'type' parameter (either 'major' or 'minor').");
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
            
            AuthenticatedUser u = findUserOrDie(apiKey);
            if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
            
            Dataset ds = datasetService.find(dsId);
            if ( ds == null ) return notFound("Can't find dataset with id '" + id + "'");
            
            ds = execCommand(new PublishDatasetCommand(ds, u, isMinor), "Publish Dataset");
            return okResponse( json(ds) );
        
        }  catch (WrappedResponse ex) {
            return ex.getResponse();
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
                    if (versions.length == 1) {
                        return hdl.handleSpecific(Long.parseLong(versions[0]), (long)0.0);
                    } else if (versions.length == 2) {
                        return hdl.handleSpecific( Long.parseLong(versions[0]), Long.parseLong(versions[1]) );
                    } else {
                        throw new WrappedResponse(errorResponse( Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'"));
                    }
                } catch ( NumberFormatException nfe ) {
                    throw new WrappedResponse( errorResponse( Response.Status.BAD_REQUEST, "Illegal version identifier '" + versionId + "'") );
                }
		}
    }
    
    private DatasetVersion getDatasetVersion( final User u, String versionNumber, final Dataset ds ) throws WrappedResponse {
        return execCommand( handleVersion(versionNumber, new DsVersionHandler<Command<DatasetVersion>>(){

                @Override
                public Command<DatasetVersion> handleLatest() {
                    return new GetLatestAccessibleDatasetVersionCommand(u, ds);
                }

                @Override
                public Command<DatasetVersion> handleDraft() {
                    return new GetDraftDatasetVersionCommand(u, ds);
                }

                @Override
                public Command<DatasetVersion> handleSpecific(long major, long minor) {
                    return new GetSpecificPublishedDatasetVersionCommand(u, ds, major, minor);
                }

                @Override
                public Command<DatasetVersion> handleLatestPublished() {
                    return new GetLatestPublishedDatasetVersionCommand(u, ds);
                }
            }), "Accessing dataset version");
    }
    
}
