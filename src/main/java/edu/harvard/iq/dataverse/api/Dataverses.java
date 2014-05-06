package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListDataverseContentCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.stream.JsonParsingException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A REST API for dataverses. To be unified with {@link Dataverses}.
 * @author michael
 */
@Stateless
@Path("dvs")
public class Dataverses extends AbstractApiBean {
	private static final Logger logger = Logger.getLogger(Dataverses.class.getName());
	
	@GET
	public String list() {
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( Dataverse d : dataverseSvc.findAll() ) {
			bld.add(json(d));
		}
		return ok( bld.build() );
	}
	
	@POST
	public String addRoot( Dataverse d, @QueryParam("key") String apiKey ) {
		return addDataverse(d, "", apiKey);
	}
	
	@POST
	@Path("{identifier}")
	public String addDataverse( Dataverse d, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		if ( ! parentIdtf.isEmpty() ) {
			Dataverse owner = findDataverse(parentIdtf);
			if ( owner == null ) {
				return error( "Can't find dataverse with identifier='" + parentIdtf + "'");
			}
			d.setOwner(owner);
		}
		
		try {
			d = engineSvc.submit( new CreateDataverseCommand(d, u) );
			return ok( json(d) );
		} catch (CommandException ex) {
			logger.log(Level.SEVERE, "Error creating dataverse", ex);
			return error("Error creating dataverse: " + ex.getMessage() );
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
                return error(sb.toString());
            }
	}
    
    @POST
    @Path("{identifier}/datasets")
    public Response createDataset( String jsonBody, @PathParam("identifier") String parentIdtf, @QueryParam("key") String apiKey ) {
        DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return errorResponse( Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
		
        Dataverse owner = findDataverse(parentIdtf);
        if ( owner == null ) {
            return errorResponse( Response.Status.NOT_FOUND, "Can't find dataverse with identifier='" + parentIdtf + "'");
        }
        
        JsonObject json;
        try ( StringReader rdr = new StringReader(jsonBody) ) {
            json = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonBody);
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
            version.setVersion(1l);
            version.setVersionNumber(1l);
            version.setVersionState(DatasetVersion.VersionState.DRAFT);
            
            ds.setVersions( Collections.singletonList(version) );
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
	public String viewDataverse( @PathParam("identifier") String idtf ) {
		Dataverse d = findDataverse(idtf);
		return ( d==null) ? error("Can't find dataverse with identifier '" + idtf + "'")
						  : ok( json(d) );
	}
	
	@DELETE
	@Path("{identifier}")
	public String deleteDataverse( @PathParam("identifier") String idtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");
		
		Dataverse d = findDataverse(idtf);
		if ( d == null ) return error("Can't find dataverse with identifier '" + idtf + "'");
		
		try {
			engineSvc.submit( new DeleteDataverseCommand(u, d) );
			return ok( "Dataverse " + idtf  +" deleted");
		} catch ( CommandException ex ) {
			logger.log(Level.SEVERE, "Error deleting dataverse", ex);
			return error("Error creating dataverse: " + ex.getMessage() );
		 }
	}
	
	@GET
	@Path("{identifier}/roles")
	public String listRoles( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		JsonArrayBuilder jab = Json.createArrayBuilder();
		for ( DataverseRole r : dataverse.getRoles() ){
			jab.add( json(r) );
		}
		return ok(jab);
	}
	
	@GET
	@Path("{identifier}/metadatablocks")
	public String listMetadataBlocks( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		JsonArrayBuilder jab = Json.createArrayBuilder();
		for ( MetadataBlock blk : dataverse.getMetadataBlocks()){
			jab.add( brief.json(blk) );
		}
        
		return ok(jab);
	}
	
    @POST
    @Path("{identifier}/metadatablocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMetadataBlocks( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String blockIds ) {
        DataverseUser u = userSvc.findByUserName(apiKey);
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
    @Produces("application/json")
    public Response getMetadataRoot( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey  ) {
        DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return badApiKey(apiKey);

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) 
			return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
        return okResponseWithValue( dataverse.isMetadataBlockRoot() );
    }
    
    @POST
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces("application/json")
    public Response setMetadataRoot( @PathParam("identifier")String dvIdtf, @QueryParam("key") String apiKey, String body  ) {
        
        if ( ! Util.isBoolean(body) ) {
            return errorResponse(Response.Status.BAD_REQUEST, "Illegal value '" + body + "'. Try 'true' or 'false'");
        }
        boolean root = Util.isTrue(body);
        
        DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return badApiKey(apiKey);

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) 
			return notFound( "Can't find dataverse with identifier='" + dvIdtf + "'");
		
        return execute( new UpdateDataverseMetadataBlocksCommand.SetRoot(u, dataverse, root) );
    }
    
    
	@GET
	@Path("{identifier}/contents")
	public String listContent( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
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
				jab.add( json(ds).add("type", "dataset") );
				return null;
			}

			@Override
			public Void visit(DataFile df) { throw new UnsupportedOperationException("Files don't live directly in Dataverses"); }
		};
		try {
			for ( DvObject o : engineSvc.submit( new ListDataverseContentCommand(u, dataverse)) ) {
				o.accept(ser);
			}
		} catch (CommandException ex) {
			return error(ex.getMessage());
		}
		return ok(jab);
	}
	
	@POST
	@Path("{identifier}/roles")
	public String createRole( RoleDTO roleDto, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		try {
			return ok(json(engineSvc.submit( new CreateRoleCommand(roleDto.asRole(), u, dataverse) )));
		} catch ( CommandException ce ) {
			return error( ce.getMessage() );
		}
	}
	
	@GET
	@Path("{identifier}/assignments")
	public String listAssignments( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser u = userSvc.findByUserName(apiKey);
		if ( u == null ) return error( "Invalid apikey '" + apiKey + "'");

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		try {
			JsonArrayBuilder jab = Json.createArrayBuilder();
			for ( RoleAssignment ra : engineSvc.submit(new ListRoleAssignments(u, dataverse)) ){
				jab.add( json(ra) );
			}
			return ok(jab);
			
		} catch (CommandException ex) {
			return error( "can't list assignments: " + ex.getMessage() );
		}
	}
	
	@POST
	@Path("{identifier}/assignments")
	public String createAssignment( RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser actingUser = userSvc.findByUserName(apiKey);
		if ( actingUser == null ) return error( "Invalid apikey '" + apiKey + "'"); 

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		DataverseUser grantedUser = (ra.getUserName()!=null) ? findUser(ra.getUserName()) : userSvc.find(ra.getUserId());
		if ( grantedUser==null ) {
			return error("Can't find user using " + ra.getUserName() + "/" + ra.getUserId() );
		}
		
		DataverseRole theRole;
		if ( ra.getRoleId() != 0 ) {
			theRole = rolesSvc.find(ra.getRoleId());
			if ( theRole == null ) {
				return error("Can't find role with id " + ra.getRoleId() );
			}
			
		} else {
			Dataverse dv = dataverse;
			theRole = null;
			while ( (theRole==null) && (dv!=null) ) {
				for ( DataverseRole aRole : dv.getRoles() ) {
					if ( aRole.getAlias().equals(ra.getRoleAlias()) ) {
						theRole = aRole;
						break;
					}
				}
				dv = dv.getOwner();
			}
			if ( theRole == null ) {
				return error("Can't find role named '" + ra.getRoleAlias() + "' in dataverse " + dataverse);
			}
		}
		
		try {
			RoleAssignment roleAssignment = engineSvc.submit( new AssignRoleCommand(grantedUser, theRole, dataverse, actingUser));
			return ok(json(roleAssignment));
			
		} catch (CommandException ex) {
			logger.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
			return error(ex.getMessage());
		}
	}
	
	@DELETE
	@Path("{identifier}/assignments/{id}")
	public String deleteAssignment( @PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		DataverseUser actingUser = userSvc.findByUserName(apiKey);
		if ( actingUser == null ) return error( "Invalid apikey '" + apiKey + "'"); 

		Dataverse dataverse = findDataverse(dvIdtf);
		if ( dataverse == null ) {
			return error( "Can't find dataverse with identifier='" + dvIdtf + "'");
		}
		
		RoleAssignment ra = em.find( RoleAssignment.class, assignmentId );
		if ( ra != null ) {
			em.remove( ra );
			em.flush();
			return "Role assignment " + assignmentId + " removed";
		} else {
			return "Role assignment " + assignmentId + " not found";
		}
	}
	
	@GET
	@Path(":gv")
	public String toGraphviz() {
		StringBuilder sb = new StringBuilder();
		StringBuilder edges = new StringBuilder();
		
		sb.append( "digraph dataverses {");
		for ( Dataverse dv : dataverseSvc.findAll() ) {
			sb.append("dv").append(dv.getId()).append(" [label=\"").append(dv.getAlias()).append( "\"]\n");
			if ( dv.getOwner() != null ) {
				edges.append("dv").append(dv.getOwner().getId())
						.append("->")
					.append("dv").append(dv.getId())
					.append("\n");
			}
		}
		
		sb.append("\n");
		sb.append( edges );
		
		sb.append( "}" );
		return sb.toString();
	}
	
}
