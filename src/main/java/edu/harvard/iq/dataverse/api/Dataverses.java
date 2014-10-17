package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListDataverseContentCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.stream.JsonParsingException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A REST API for dataverses.
 * @author michael
 */
@Stateless
@Path("dvs")
public class Dataverses extends AbstractApiBean {
	private static final Logger logger = Logger.getLogger(Dataverses.class.getName());
	
	@GET
	public String list() {
        // FIXME remove this, this goes against permissions.
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( Dataverse d : dataverseSvc.findAll() ) {
			bld.add(json(d));
		}
		return ok( bld.build() );
	}
	
	@POST
	public Response addRoot( Dataverse d, @QueryParam("key") String apiKey ) {
		return addDataverse(d, "", apiKey);
	}
	
	@POST
	@Path("{identifier}")
	public Response addDataverse( Dataverse d, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
		User u = findUserByApiToken(apiKey);
		if ( u == null ) return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
		if ( ! parentIdtf.isEmpty() ) {
			Dataverse owner = findDataverse(parentIdtf);
			if ( owner == null ) {
				return errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
			}
			d.setOwner(owner);
		}
		
		try {
			d = engineSvc.submit( new CreateDataverseCommand(d, u) );
			return okResponse( json(d) );
		} catch (CommandException ex) {
			logger.log(Level.SEVERE, "Error creating dataverse", ex);
			return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + ex.getMessage() );
        } catch (EJBException ex) {
                Throwable cause = ex;
                StringBuilder sb = new StringBuilder();
                sb.append("Error creating dataverse.");
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    if (cause instanceof ConstraintViolationException) {
                        ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                        for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                            sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                    .append(violation.getPropertyPath()).append(" at ")
                                    .append(violation.getLeafBean()).append(" - ")
                                    .append(violation.getMessage());
                        }
                    }
                }
                logger.log(Level.SEVERE, sb.toString());
                return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + sb.toString() );
            }
	}
    
    @POST
    @Path("{identifier}/datasets")
    public Response createDataset( String jsonBody, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey ) {
        User u = findUserByApiToken(apiKey);
		if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
        Dataverse owner = findDataverse(parentIdtf);
        if ( owner == null ) {
            return errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
        }
        
        JsonObject json;
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            json = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: {0}", jsonBody);
            return errorResponse( Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        Dataset ds = new Dataset();
        ds.setOwner(owner);
        ds.setIdentifier( json.getString("identifier"));
        ds.setAuthority(  json.getString("authority"));
        ds.setProtocol(   json.getString("protocol"));
        JsonObject jsonVersion = json.getJsonObject("initialVersion");
        if ( jsonVersion == null) {
            return errorResponse(Status.BAD_REQUEST, "Json POST data are missing initialVersion object.");
        }
        try {
            try {
                DatasetVersion version = jsonParser().parseDatasetVersion(jsonVersion);

                // force "initial version" properties
                version.setMinorVersionNumber(0l);
                version.setVersionNumber(1l);
                version.setVersionState(DatasetVersion.VersionState.DRAFT);
                LinkedList<DatasetVersion> versions = new LinkedList<>();
                versions.add(version);
                version.setDataset(ds);
                
                ds.setVersions( versions );
            } catch ( javax.ejb.TransactionRolledbackLocalException rbe ) {
                throw rbe.getCausedByException();
            }
        } catch (JsonParseException ex) {
            logger.log( Level.INFO, "Error parsing dataset version from Json", ex);
            return errorResponse(Status.BAD_REQUEST, "Error parsing initialVersion: " + ex.getMessage() );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error parsing dataset version from Json", e);
            return errorResponse(Status.INTERNAL_SERVER_ERROR, "Error parsing initialVersion: " + e.getMessage() );
        }
        
        try {
            Dataset managedDs = engineSvc.submit( new CreateDatasetCommand(ds, u));
            return okResponse( Json.createObjectBuilder().add("id", managedDs.getId()) );
            
        } catch (CommandException ex) {
            String incidentId = UUID.randomUUID().toString();
            logger.log(Level.SEVERE, "Error creating new dataset: " + ex.getMessage() + " incidentId:" + incidentId, ex);
            return errorResponse(Status.INTERNAL_SERVER_ERROR, "Error executing command. More data in the server logs. Incident id is " + incidentId);
        }
    }
	
	@GET
	@Path("{identifier}")
	public Response viewDataverse( @PathParam("identifier") String idtf, @QueryParam("key") String apiKey ) {
		Dataverse d = findDataverse(idtf);
        if (d == null) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
        
        User u = findUserByApiToken(apiKey);
        if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
        
        try {
			Dataverse retrieved = execCommand( new GetDataverseCommand(u, d), "Get Dataverse" );
			return okResponse( json(retrieved));
		} catch ( FailedCommandResult ex ) {
			return ex.getResponse();
		}
        
		/*return ( d==null) ? errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier '" + idtf + "'")
						  : okResponse( json(d) ); */
	}
	
	@DELETE
	@Path("{identifier}")
	public Response deleteDataverse( @PathParam("identifier") String idtf, @QueryParam("key") String apiKey ) {
		User u = findUserByApiToken(apiKey);
		if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
		Dataverse d = findDataverse(idtf);
		if ( d == null ) return errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier '" + idtf + "'");
		
		try {
			execCommand( new DeleteDataverseCommand(u, d), "Delete Dataverse" );
			return okResponse( "Dataverse " + idtf  +" deleted");
		} catch ( FailedCommandResult ex ) {
			return ex.getResponse();
		}
	}
	
	@GET
	@Path("{identifier}/roles")
	public Response listRoles( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		User u = findUserByApiToken(apiKey);
		if ( u == null ) return errorResponse( Status.FORBIDDEN, "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return errorResponse( Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		JsonArrayBuilder jab = Json.createArrayBuilder();
		for ( DataverseRole r : dataverse.getRoles() ){
			jab.add( json(r) );
		}
		return okResponse(jab);
	}
	
	@GET
	@Path("{identifier}/metadatablocks")
	public Response listMetadataBlocks( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		User u = findUserByApiToken(apiKey);
		if ( u == null ) return errorResponse( Status.FORBIDDEN, "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return errorResponse( Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		JsonArrayBuilder jab = Json.createArrayBuilder();
		for ( MetadataBlock blk : dataverse.getMetadataBlocks()){
			jab.add( brief.json(blk) );
		}
        
		return okResponse(jab);
	}
	
    @POST
    @Path("{identifier}/metadatablocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMetadataBlocks( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String blockIds ) {
        User u = findUserByApiToken(apiKey);
		if ( u == null ) return badApiKey(apiKey);

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
        
        List<MetadataBlock> blocks = new LinkedList<>();
        
        for ( JsonString blockId : Util.asJsonArray(blockIds).getValuesAs(JsonString.class) ) {
            MetadataBlock blk = findMetadataBlock(blockId.getString());
            if ( blk == null ) {
                return errorResponse(Response.Status.BAD_REQUEST, "Can't find metadata block '"+ blockId + "'");
            }
            blocks.add( blk );
        }
        
        return execute( new UpdateDataverseMetadataBlocksCommand.SetBlocks(u, dataverse, blocks) );
    }
    
    @GET
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataRoot( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey  ) {
        User u = findUserByApiToken(apiKey);
		if ( u == null ) return badApiKey(apiKey);

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) 
			return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
        return okResponseWithValue( dataverse.isMetadataBlockRoot() );
    }
    
    @POST
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMetadataRoot( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String body  ) {
        
        if ( ! Util.isBoolean(body) ) {
            return errorResponse(Response.Status.BAD_REQUEST, "Illegal value '" + body + "'. Try 'true' or 'false'");
        }
        boolean root = Util.isTrue(body);
        
        User u = findUserByApiToken(apiKey);
		if ( u == null ) return badApiKey(apiKey);

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) 
			return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
        return execute( new UpdateDataverseMetadataBlocksCommand.SetRoot(u, dataverse, root) );
    }
    
    
	@GET
	@Path("{identifier}/contents")
	public Response listContent( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		User u = findUserByApiToken(apiKey);
		if ( u == null ) return errorResponse( Status.FORBIDDEN, "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return errorResponse( Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		final JsonArrayBuilder jab = Json.createArrayBuilder();
		DvObject.Visitor<Void> ser = new DvObject.Visitor<Void>() {

			@Override
			public Void visit(Dataverse dv) {
				jab.add( Json.createObjectBuilder().add("type", "dataverse")
						.add("id", dv.getId())
						.add("title",dv.getName() ));
				return null;
			}

			@Override
			public Void visit(Dataset ds) {
                // TODO: check for permission to view drafts
				jab.add( json(ds).add("type", "dataset") );
				return null;
			}

			@Override
			public Void visit(DataFile df) { throw new UnsupportedOperationException("Files don't live directly in Dataverses"); }
		};
		try {
			for ( DvObject o : execCommand(new ListDataverseContentCommand(u, dataverse), "List Dataverse") ) {
				o.accept(ser);
			}
		} catch (FailedCommandResult ex) {
			return ex.getResponse();
		}
		return okResponse(jab);
	}
	
	@POST
	@Path("{identifier}/roles")
	public Response createRole( RoleDTO roleDto, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		
        User u = findUserByApiToken(apiKey);
		if ( u == null ) return badApiKey(apiKey);
		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
        
		try {
			return okResponse( json(execCommand(new CreateRoleCommand(roleDto.asRole(), u, dataverse), "Create Role")));
        } catch ( FailedCommandResult ce ) {
			return ce.getResponse();
		}
	}
	
	@GET
	@Path("{identifier}/assignments")
	public Response listAssignments( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		User u = findUserByApiToken(apiKey);
		if ( u == null ) return badApiKey(apiKey);
		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
		try {
			JsonArrayBuilder jab = Json.createArrayBuilder();
			for ( RoleAssignment ra : execCommand(new ListRoleAssignments(u, dataverse), "Role Assignment Listing") ){
				jab.add( json(ra) );
			}
			return okResponse(jab);
			
		} catch (FailedCommandResult ex) {
			return ex.getResponse();
		}
	}
	
	@POST
	@Path("{identifier}/assignments")
	public Response createAssignment( RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		User actingUser = findUserByApiToken(apiKey);
		if ( actingUser == null ) return badApiKey(apiKey);
		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
        RoleAssignee assignee = findAssignee(ra.getAssignee());
		if ( assignee==null ) {
			return errorResponse( Status.BAD_REQUEST, "Assignee not found" );
		}
		
		DataverseRole theRole;
        Dataverse dv = dataverse;
        theRole = null;
        while ( (theRole==null) && (dv!=null) ) {
            for ( DataverseRole aRole : dv.getRoles() ) {
                if ( aRole.getAlias().equals(ra.getRole()) ) {
                    theRole = aRole;
                    break;
                }
            }
            dv = dv.getOwner();
        }
        if ( theRole == null ) {
            return errorResponse( Status.BAD_REQUEST, "Can't find role named '" + ra.getRole() + "' in dataverse " + dataverse);
        }
		
		try {
			RoleAssignment roleAssignment = execCommand( new AssignRoleCommand(assignee, theRole, dataverse, actingUser), "Assign role");
			return okResponse(json(roleAssignment));
			
		} catch (FailedCommandResult ex) {
			logger.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
			return ex.getResponse();
		}
	}
	
	@DELETE
	@Path("{identifier}/assignments/{id}")
	public Response deleteAssignment( @PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		User actingUser = findUserByApiToken(apiKey);
		if ( actingUser == null ) return badApiKey(apiKey);
		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
		RoleAssignment ra = em.find( RoleAssignment.class, assignmentId );
		if ( ra != null ) {
			em.remove( ra );
			em.flush();
			return okResponse("Role assignment " + assignmentId + " removed");
		} else {
			return errorResponse( Status.NOT_FOUND, "Role assignment " + assignmentId + " not found" );
		}
	}
	
    @POST
    @Path("{identifier}/actions/:publish") 
    public Response publishDataverse( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
        try {

            Dataverse dv = findDataverse(dvIdtf);
            if ( dv == null ) {
                return errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + dvIdtf + "'");
            }
            
            User u = findUserByApiToken(apiKey);
            if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
            
            dv = engineSvc.submit( new PublishDataverseCommand(u, dv) );
            return okResponse( json(dv) );
            
        } catch (IllegalCommandException ex) {
            return errorResponse( Response.Status.FORBIDDEN, "Error publishing dataverse: " + ex.getMessage() );
            
        } catch (CommandException ex) {
            Logger.getLogger(Datasets.class.getName()).log(Level.SEVERE, "Error while publishing a Dataverse", ex);
            return errorResponse( Response.Status.INTERNAL_SERVER_ERROR, "Error publishing the dataset: " + ex.getMessage() );
        }
    }
}
