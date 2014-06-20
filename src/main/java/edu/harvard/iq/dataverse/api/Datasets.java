package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("datasets")
public class Datasets extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Datasets.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseServiceBean dataverseService;

    
	
	@GET
	@Path("{id}")
    public Response getDataset( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
        Dataset ds = datasetService.find(id);
        return (ds != null) ? okResponse(json(ds))
							: notFound("dataset not found");
        
    }
	
	@DELETE
	@Path("{id}")
	public Response deleteDataset( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
        Dataset ds = datasetService.find(id);
        if (ds == null) return errorResponse( Response.Status.NOT_FOUND, "dataset not found");
		
		try {
			engineSvc.submit( new DeleteDatasetCommand(ds, u));
			return okResponse("Dataset " + id + " deleted");
			
		} catch (CommandExecutionException ex) {
			// internal error
			logger.log( Level.SEVERE, "Error deleting dataset " + id + ":  " + ex.getMessage(), ex );
			return errorResponse( Response.Status.FORBIDDEN, "Can't delete dataset: " + ex.getMessage() );
			
		} catch (CommandException ex) {
			return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Can't delete dataset: " + ex.getMessage() );
		}
		
	}
	
	@GET
	@Path("{id}/versions")
    public Response listVersions( @PathParam("id") Long id, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse( Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(id);
        if (ds == null) return notFound("dataset not found");
		
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( DatasetVersion dsv : ds.getVersions() ) {
			bld.add( json(dsv) );
		}
		
		return okResponse( bld );
    }
	
	@GET
	@Path("{id}/versions/{versionId}")
    public Response getVersion( @PathParam("id") Long datasetId, @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(datasetId);
        if (ds == null) return errorResponse(Response.Status.NOT_FOUND, "dataset " + datasetId + " not found");
		
		DatasetVersion dsv = null;
		switch (versionId) {
			case ":latest":
				dsv = ds.getLatestVersion();
				break;
			case ":edit":
				dsv = ds.getEditVersion();
				break;
			default:
				try {
					long versionNumericId = Long.parseLong(versionId);
					for ( DatasetVersion aDsv : ds.getVersions() ) {
						if ( aDsv.getId().equals(versionNumericId) ) {
							dsv = aDsv;
							break; // for, not while
						}
					}
				} catch ( NumberFormatException nfe ) {
					return errorResponse( Response.Status.BAD_REQUEST, "Illegal id number '" + versionId + "'");
				}	
                break;
		}
		
		return (dsv==null)
				? errorResponse(Response.Status.NOT_FOUND, "dataset version not found")
				: okResponse( json(dsv)  );
    }
    
    @GET
	@Path("{id}/versions/{versionId}/metadata")
    public Response getVersionMetadata( @PathParam("id") Long datasetId, @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(datasetId);
        if (ds == null) return errorResponse(Response.Status.NOT_FOUND, "dataset " + datasetId + " not found");
		
		DatasetVersion dsv = null;
		switch (versionId) {
			case ":latest":
				dsv = ds.getLatestVersion();
				break;
			case ":edit":
				dsv = ds.getEditVersion();
				break;
			default:
				try {
					long versionNumericId = Long.parseLong(versionId);
					for ( DatasetVersion aDsv : ds.getVersions() ) {
						if ( aDsv.getId().equals(versionNumericId) ) {
							dsv = aDsv;
							break; // for, not while
						}
					}
				} catch ( NumberFormatException nfe ) {
					return errorResponse( Response.Status.BAD_REQUEST, "Illegal id number '" + versionId + "'");
				}	
                break;
		}
		
		return (dsv==null)
				? errorResponse(Response.Status.NOT_FOUND, "dataset version not found")
				: okResponse( JsonPrinter.jsonByBlocks(dsv.getDatasetFields())  );
    }
    
    @GET
	@Path("{id}/versions/{versionNumber}/metadata/{block}")
    public Response getVersionMetadataBlock( @PathParam("id") Long datasetId, 
                                             @PathParam("versionNumber") String versionNumber, 
                                             @PathParam("block") String blockName,
                                             @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
		// TODO filter by what the user can see.
		
        Dataset ds = datasetService.find(datasetId);
        if (ds == null) return errorResponse(Response.Status.NOT_FOUND, "dataset " + datasetId + " not found");
		
		DatasetVersion dsv = null;
		switch (versionNumber) {
			case ":latest":
				dsv = ds.getLatestVersion();
				break;
			case ":edit":
				dsv = ds.getEditVersion();
				break;
			default:
				try {
                    String[] comps = versionNumber.split("\\.");
                    long majorVersion = Long.parseLong(comps[0]);
                    long minorVersion = comps.length > 1 ? Long.parseLong(comps[1]) : 0;
					for ( DatasetVersion aDsv : ds.getVersions() ) {
						if ( aDsv.getVersionNumber().equals(majorVersion) &&
                              aDsv.getMinorVersionNumber().equals(minorVersion)) {
							dsv = aDsv;
							break; // for, not switch
						}
					}
				} catch ( NumberFormatException nfe ) {
					return errorResponse( Response.Status.BAD_REQUEST, "Illegal version number '" + versionNumber + "'. Values are :latest, :edit and x.y");
				}	
                break;
		}
		
        if ( dsv == null ) return errorResponse(Response.Status.NOT_FOUND, "dataset version not found");
        Map<MetadataBlock, List<DatasetField>> fieldsByBlock = DatasetField.groupByBlock(dsv.getDatasetFields());
        for ( Map.Entry<MetadataBlock, List<DatasetField>> p : fieldsByBlock.entrySet() ) {
            if ( p.getKey().getName().equals(blockName) ) {
                return okResponse( JsonPrinter.json(p.getKey(), p.getValue()) );
            }
        }
		return notFound("metadata block named " + blockName + " not found");
    }
	
	@GET
	@Path("{id}/versions/{versionId}/files/")
	public String listFiles() {
		// TODO implement
		return error("Not implemented yet");
	}
	
    @PUT
	@Path("{id}/versions/{versionId}")
	public Response updateDraftVersion( String jsonBody, @PathParam("id") Long id,  @PathParam("versionId") String versionId, @QueryParam("key") String apiKey ){
        
        if ( ! ":edit".equals(versionId) ) 
            return errorResponse( Response.Status.BAD_REQUEST, "Only the :edit version can be put on server");
        
        DataverseUser u = userSvc.findByUserName(apiKey);
        if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
        Dataset ds = datasetService.find(id);
        if ( ds == null ) return notFound("Can't find dataset with id '" + id + "'");
        
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            JsonObject json = Json.createReader(rdr).readObject();
            DatasetVersion version = jsonParser().parseDatasetVersion(json);
            version.setDataset(ds);
            boolean updateDraft = ds.getLatestVersion().isDraft();
            DatasetVersion managedVersion = engineSvc.submit( updateDraft
                                                                ? new UpdateDatasetVersionCommand(u, version)
                                                                : new CreateDatasetVersionCommand(u, ds, version) );
            return okResponse( json(managedVersion) );
                    
        } catch (CommandException ex) {
            logger.log(Level.SEVERE, "Error executing CreateDatasetVersionCommand: " + ex.getMessage(), ex);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + ex.getMessage() );
            
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset version Json: " + ex.getMessage(), ex);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing dataset version: " + ex.getMessage() );
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
            long dsId=0;
            try {
                dsId = Long.parseLong(id);
            } catch ( NumberFormatException nfe ) {
                return errorResponse( Response.Status.BAD_REQUEST, "Bad dataset id. Please provide a number.");
            }
            
            DataverseUser u = userSvc.findByUserName(apiKey);
            if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
            
            Dataset ds = datasetService.find(dsId);
            if ( ds == null ) return notFound("Can't find dataset with id '" + id + "'");
            
            ds = engineSvc.submit( new PublishDatasetCommand(ds, u, isMinor));
            return okResponse( json(ds) );
            
        } catch (IllegalCommandException ex) {
            return errorResponse( Response.Status.FORBIDDEN, "Error publishing the dataset: " + ex.getMessage() );
            
        } catch (CommandException ex) {
            Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, "Error while publishing a Dataset", ex);
            return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Error publishing the dataset: " + ex.getMessage() );
        }
    }
    
}
