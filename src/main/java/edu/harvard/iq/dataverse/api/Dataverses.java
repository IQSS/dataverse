package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.api.dto.ExplicitGroupDTO;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AddRoleAssigneesToExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListDataverseContentCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListExplicitGroupsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListFacetsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListMetadataBlocksCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.ListRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RemoveRoleAssigneesFromExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateExplicitGroupCommand;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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
import javax.ws.rs.core.Response.Status;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;

/**
 * A REST API for dataverses.
 * @author michael
 */
@Stateless
@Path("dataverses")
public class Dataverses extends AbstractApiBean {
       
	private static final Logger LOGGER = Logger.getLogger(Dataverses.class.getName());
    
    @EJB
    ExplicitGroupServiceBean explicitGroupSvc;
    
	@POST
	public Response addRoot( String body ) {
        LOGGER.info("Creating root dataverse");
		return addDataverse( body, "");
	}
	
	@POST
	@Path("{identifier}")
	public Response addDataverse( String body, @PathParam("identifier") String parentIdtf ) {
		
        Dataverse d;
        JsonObject dvJson;
        try ( StringReader rdr = new StringReader(body) ) {
            dvJson = Json.createReader(rdr).readObject();
            d = jsonParser().parseDataverse(dvJson);
        } catch ( JsonParsingException jpe ) {
            LOGGER.log(Level.SEVERE, "Json: {0}", body);
            return error( Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        } catch (JsonParseException ex) {
            Logger.getLogger(Dataverses.class.getName()).log(Level.SEVERE, "Error parsing dataverse from json: " + ex.getMessage(), ex);
            return error( Response.Status.BAD_REQUEST,
                    "Error parsing the POSTed json into a dataverse: " + ex.getMessage() );
        }
        
		try {
            if ( ! parentIdtf.isEmpty() ) {
                Dataverse owner = findDataverseOrDie( parentIdtf );
                d.setOwner(owner);
            }

            // set the dataverse - contact relationship in the contacts
            for (DataverseContact dc : d.getDataverseContacts()) {
                dc.setDataverse(d);
            }

            AuthenticatedUser u = findAuthenticatedUserOrDie();
            d = execCommand( new CreateDataverseCommand(d, createDataverseRequest(u), null, null) );
			return created( "/dataverses/"+d.getAlias(), json(d) );
        } catch ( WrappedResponse ww ) {
                    Throwable cause = ww.getCause();
                    StringBuilder sb = new StringBuilder();
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
                    String error = sb.toString();
                    if (!error.isEmpty()) {
                        LOGGER.log(Level.INFO, error);
                        return ww.refineResponse(error);
                    }
            return ww.getResponse();
            
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
            LOGGER.log(Level.SEVERE, sb.toString());
            return error( Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + sb.toString() );
        } catch ( Exception ex ) {
			LOGGER.log(Level.SEVERE, "Error creating dataverse", ex);
			return error( Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + ex.getMessage() );
            
        }
	}
    
    @POST
    @Path("{identifier}/datasets")
    public Response createDataset( String jsonBody, @PathParam("identifier") String parentIdtf  ) {
        try {
            User u = findUserOrDie();
            Dataverse owner = findDataverseOrDie(parentIdtf);
            
            JsonObject json;
            try ( StringReader rdr = new StringReader(jsonBody) ) {
                json = Json.createReader(rdr).readObject();
            } catch ( JsonParsingException jpe ) {
                LOGGER.log(Level.SEVERE, "Json: {0}", jsonBody);
                return error( Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
            }
            
            Dataset ds = new Dataset();
            ds.setOwner(owner);
          
            JsonObject jsonVersion = json.getJsonObject("datasetVersion");
            if ( jsonVersion == null) {
                return error(Status.BAD_REQUEST, "Json POST data are missing datasetVersion object.");
            }
            try {
                try {
                    DatasetVersion version = new DatasetVersion();
                    version.setDataset(ds);
                    // Use the two argument version so that the version knows which dataset it's associated with.
                    version = jsonParser().parseDatasetVersion(jsonVersion, version);
                    
                    // force "initial version" properties
                    version.setMinorVersionNumber(null);
                    version.setVersionNumber(null);
                    version.setVersionState(DatasetVersion.VersionState.DRAFT);
                    LinkedList<DatasetVersion> versions = new LinkedList<>();
                    versions.add(version);
                    version.setDataset(ds);
                    
                    ds.setVersions( versions );
                } catch ( javax.ejb.TransactionRolledbackLocalException rbe ) {
                    throw rbe.getCausedByException();
                }
            } catch (JsonParseException ex) {
                LOGGER.log( Level.INFO, "Error parsing dataset version from Json", ex);
                return error(Status.BAD_REQUEST, "Error parsing datasetVersion: " + ex.getMessage() );
            } catch ( Exception e ) {
                LOGGER.log( Level.WARNING, "Error parsing dataset version from Json", e);
                return error(Status.INTERNAL_SERVER_ERROR, "Error parsing datasetVersion: " + e.getMessage() );
            }
            
            Dataset managedDs = execCommand(new CreateDatasetCommand(ds, createDataverseRequest(u)));
            return created( "/datasets/" + managedDs.getId(),
                    Json.createObjectBuilder().add("id", managedDs.getId()) );
                
        } catch ( WrappedResponse ex ) {
            return ex.getResponse();
        }
    }
	
	@GET
	@Path("{identifier}")
	public Response viewDataverse( @PathParam("identifier") String idtf ) {
        return response( req -> ok(json(execCommand(
                                    new GetDataverseCommand(req, findDataverseOrDie(idtf))))));
	}
	
	@DELETE
	@Path("{identifier}")
	public Response deleteDataverse( @PathParam("identifier") String idtf ) {
        return response( req -> {
			execCommand( new DeleteDataverseCommand(req, findDataverseOrDie(idtf)));
			return ok( "Dataverse " + idtf  +" deleted");
        });
	}
	
	@GET
	@Path("{identifier}/metadatablocks")
	public Response listMetadataBlocks( @PathParam("identifier") String dvIdtf ) {
        return response( req ->ok(
                execCommand( new ListMetadataBlocksCommand(req, findDataverseOrDie(dvIdtf)))
                .stream().map(brief::json).collect( toJsonArray() )
        ));
	}
	
    @POST
    @Path("{identifier}/metadatablocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMetadataBlocks( @PathParam("identifier")String dvIdtf, String blockIds ) {
        
        List<MetadataBlock> blocks = new LinkedList<>();
        try {
            for ( JsonValue blockId : Util.asJsonArray(blockIds).getValuesAs(JsonValue.class) ) {
                MetadataBlock blk = (blockId.getValueType()==ValueType.NUMBER)
                                        ? findMetadataBlock( ((JsonNumber)blockId).longValue() )
                                        : findMetadataBlock( ((JsonString)blockId).getString() );
                if ( blk == null ) {
                    return error(Response.Status.BAD_REQUEST, "Can't find metadata block '"+ blockId + "'");
                }
                blocks.add( blk );
            }
        } catch( Exception e ) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        
        try {
            execCommand( new UpdateDataverseMetadataBlocksCommand.SetBlocks(createDataverseRequest(findUserOrDie()), findDataverseOrDie(dvIdtf), blocks));
            return ok("Metadata blocks of dataverse " + dvIdtf + " updated.");
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
 
    @GET
    @Path("{identifier}/metadatablocks/:isRoot")
    public Response getMetadataRoot_legacy( @PathParam("identifier")String dvIdtf ) {
        return getMetadataRoot(dvIdtf);
    }
    
    @GET
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataRoot( @PathParam("identifier")String dvIdtf ) {
        return response( req -> {
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);
            if ( permissionSvc.request(req)
                                .on(dataverse)
                                  .has(Permission.EditDataverse) ) {
                return ok( dataverse.isMetadataBlockRoot() );
            } else {
                return error( Status.FORBIDDEN, "Not authorized" );
            }
        });
    }
    
    @POST
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setMetadataRoot_legacy( @PathParam("identifier")String dvIdtf, String body  ) {
        return setMetadataRoot(dvIdtf, body);
    }
    
    @PUT
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setMetadataRoot( @PathParam("identifier")String dvIdtf, String body  ) {
        return response( req -> {
            final boolean root = parseBooleanOrDie(body);
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execCommand(new UpdateDataverseMetadataBlocksCommand.SetRoot(req, dataverse, root));
            return ok("Dataverse " + dataverse.getName() + " is now a metadata  " + (root? "" : "non-") + "root");
        });
    }
    
    @GET
    @Path("{identifier}/facets/")
    public Response listFacets( @PathParam("identifier") String dvIdtf ) {
        return response( req -> ok(
                        execCommand(new ListFacetsCommand(req, findDataverseOrDie(dvIdtf)) )
                            .stream().map(f->json(f)).collect(toJsonArray())));
    }

    @POST
    @Path("{identifier}/facets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setFacets( @PathParam("identifier")String dvIdtf, String facetIds ) {
        
        List<DatasetFieldType> facets = new LinkedList<>();
        for ( JsonString facetId : Util.asJsonArray(facetIds).getValuesAs(JsonString.class) ) {
            DatasetFieldType dsfType = findDatasetFieldType(facetId.getString());
            if ( dsfType == null ) {
                return error(Response.Status.BAD_REQUEST, "Can't find dataset field type '"+ facetId + "'");
            } else if (!dsfType.isFacetable()) {
                return error(Response.Status.BAD_REQUEST, "Dataset field type '"+ facetId + "' is not facetable");              
            }
            facets.add( dsfType );
        }
        
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            // by passing null for Featured Dataverses and DataverseFieldTypeInputLevel, those are not changed
            execCommand( new UpdateDataverseCommand(dataverse, facets, null, createDataverseRequest(findUserOrDie()), null) );
            return ok("Facets of dataverse " + dvIdtf + " updated.");
            
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }    
    
	@GET
	@Path("{identifier}/contents")
	public Response listContent( @PathParam("identifier") String dvIdtf ) {
		DvObject.Visitor<JsonObjectBuilder> ser = new DvObject.Visitor<JsonObjectBuilder>() {

			@Override
			public JsonObjectBuilder visit(Dataverse dv) {
				return Json.createObjectBuilder().add("type", "dataverse")
						.add("id", dv.getId())
						.add("title",dv.getName() );
			}

			@Override
			public JsonObjectBuilder visit(Dataset ds) {
				return json(ds).add("type", "dataset");
			}

			@Override
			public JsonObjectBuilder visit(DataFile df) { throw new UnsupportedOperationException("Files don't live directly in Dataverses"); }
		};
        
        return response( req -> ok(
            execCommand(new ListDataverseContentCommand(req, findDataverseOrDie(dvIdtf)))
                .stream()
                .map( dvo->(JsonObjectBuilder)dvo.accept(ser))
                .collect(toJsonArray())
        ));
	}
	
    @GET
	@Path("{identifier}/roles")
	public Response listRoles( @PathParam("identifier") String dvIdtf ) {
        return response( req -> ok(
                execCommand( new ListRolesCommand(req, findDataverseOrDie(dvIdtf)) )
                .stream().map(r->json(r))
                .collect( toJsonArray() )
        ));
	}
    
	@POST
	@Path("{identifier}/roles")
	public Response createRole( RoleDTO roleDto, @PathParam("identifier") String dvIdtf ) {
		return response( req -> ok( json(execCommand(new CreateRoleCommand(roleDto.asRole(), req, findDataverseOrDie(dvIdtf))))));
	}
	
	@GET
	@Path("{identifier}/assignments")
	public Response listAssignments( @PathParam("identifier") String dvIdtf) {
        return response( req -> ok(
                execCommand(new ListRoleAssignments(req, findDataverseOrDie(dvIdtf)))
                .stream()
                .map( a -> json(a) )
                .collect(toJsonArray())
        ));
	}
	
	@POST
	@Path("{identifier}/assignments")
	public Response createAssignment( RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
		
		try {
            final DataverseRequest req = createDataverseRequest(findUserOrDie());
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);

            RoleAssignee assignee = findAssignee(ra.getAssignee());
            if ( assignee==null ) {
                return error( Status.BAD_REQUEST, "Assignee not found" );
            }

            DataverseRole theRole;
            Dataverse dv = dataverse;
            theRole = null;
            while ( (theRole==null) && (dv!=null) ) {
                for ( DataverseRole aRole : rolesSvc.availableRoles(dv.getId()) ) {
                    if ( aRole.getAlias().equals(ra.getRole()) ) {
                        theRole = aRole;
                        break;
                    }
                }
                dv = dv.getOwner();
            }
            if ( theRole == null ) {
                return error( Status.BAD_REQUEST, "Can't find role named '" + ra.getRole() + "' in dataverse " + dataverse);
            }
                    String privateUrlToken = null;

			return ok(json(execCommand(new AssignRoleCommand(assignee, theRole, dataverse, req, privateUrlToken))));
			
		} catch (WrappedResponse ex) {
			LOGGER.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
			return ex.getResponse();
		}
	}
	
	@DELETE
	@Path("{identifier}/assignments/{id}")
	public Response deleteAssignment( @PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf ) {
		RoleAssignment ra = em.find( RoleAssignment.class, assignmentId );
		if ( ra != null ) {
            try {
                findDataverseOrDie(dvIdtf);
                execCommand( new RevokeRoleCommand(ra, createDataverseRequest(findUserOrDie())));
                return ok("Role " + ra.getRole().getName() 
                                            + " revoked for assignee " + ra.getAssigneeIdentifier()
                                            + " in " + ra.getDefinitionPoint().accept(DvObject.NamePrinter) );
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
		} else {
			return error( Status.NOT_FOUND, "Role assignment " + assignmentId + " not found" );
		}
	}
	
    @POST
    @Path("{identifier}/actions/:publish") 
    public Response publishDataverse( @PathParam("identifier") String dvIdtf ) {
        try {
            Dataverse dv = findDataverseOrDie(dvIdtf);
            return ok( json(execCommand( new PublishDataverseCommand(createDataverseRequest(findAuthenticatedUserOrDie()), dv))) );
            
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
        
    @POST
    @Path("{identifier}/groups/") 
    public Response createExplicitGroup( ExplicitGroupDTO dto, @PathParam("identifier") String dvIdtf) {
        return response( req ->{
            ExplicitGroupProvider prv = explicitGroupSvc.getProvider();
            ExplicitGroup newGroup = dto.apply(prv.makeGroup());
            
            newGroup = execCommand( new CreateExplicitGroupCommand(req, findDataverseOrDie(dvIdtf), newGroup));
            
            String groupUri = String.format("%s/groups/%s", dvIdtf, newGroup.getGroupAliasInOwner());
            return created( groupUri, json(newGroup) );
        });
    }
    
    @GET
    @Path("{identifier}/groups/") 
    public Response listGroups( @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey ) {
        return response( req -> ok(
            execCommand(new ListExplicitGroupsCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream().map( eg->json(eg))
                        .collect( toJsonArray() )
        ));
    }
    
    @GET
    @Path("{identifier}/groups/{aliasInOwner}") 
    public Response getGroupByOwnerAndAliasInOwner( @PathParam("identifier") String dvIdtf,
                                                    @PathParam("aliasInOwner") String grpAliasInOwner ){
        return response( req -> ok(json(findExplicitGroupOrDie(findDataverseOrDie(dvIdtf),
                                                      req,
                                                      grpAliasInOwner))));
    }
    
    @PUT
    @Path("{identifier}/groups/{aliasInOwner}") 
    public Response updateGroup(ExplicitGroupDTO groupDto, 
                                @PathParam("identifier") String dvIdtf,
                                @PathParam("aliasInOwner") String grpAliasInOwner )
    {
        return response( req-> ok(json(execCommand( 
                             new UpdateExplicitGroupCommand(req,
                                     groupDto.apply( findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)))))));
    }
    
    @DELETE
    @Path("{identifier}/groups/{aliasInOwner}") 
    public Response deleteGroup(@PathParam("identifier") String dvIdtf,
                                @PathParam("aliasInOwner") String grpAliasInOwner )
    {
        return response( req -> {
            execCommand( new DeleteExplicitGroupCommand(req,
                                findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)) );            
            return ok( "Group " + dvIdtf + "/" + grpAliasInOwner + " deleted" );
        });
    }
    
    @POST
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees") 
    @Consumes("application/json")
    public Response addRoleAssingees(List<String> roleAssingeeIdentifiers, 
                                @PathParam("identifier") String dvIdtf,
                                @PathParam("aliasInOwner") String grpAliasInOwner)
    {
        return response( req -> ok( 
                    json(
                      execCommand( 
                              new AddRoleAssigneesToExplicitGroupCommand(req, 
                                      findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner),
                                      new TreeSet<>(roleAssingeeIdentifiers))))));
    }
    
    @PUT
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}") 
    public Response addRoleAssingee( @PathParam("identifier") String dvIdtf,
                                     @PathParam("aliasInOwner") String grpAliasInOwner, 
                                     @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        return addRoleAssingees(Collections.singletonList(roleAssigneeIdentifier), dvIdtf, grpAliasInOwner);
    }
    
    @DELETE
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}") 
    public Response deleteRoleAssingee( @PathParam("identifier") String dvIdtf,
                                        @PathParam("aliasInOwner") String grpAliasInOwner, 
                                        @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier ) {
        return response( req ->ok(json(execCommand( 
                              new RemoveRoleAssigneesFromExplicitGroupCommand(req, 
                                      findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner),
                                      Collections.singleton(roleAssigneeIdentifier))))));
    }
    
    private ExplicitGroup findExplicitGroupOrDie( DvObject dv, DataverseRequest req, String groupIdtf ) throws WrappedResponse {
        ExplicitGroup eg = execCommand(new GetExplicitGroupCommand(req, dv, groupIdtf) );
        if ( eg == null ) throw new WrappedResponse( notFound("Can't find " + groupIdtf + " in dataverse " + dv.getId()));
        return eg;
    }

    @GET
    @Path("{identifier}/links")
    public Response listLinks(@PathParam("identifier") String dvIdtf ) {
        try {
            User u = findUserOrDie();
            Dataverse dv = findDataverseOrDie(dvIdtf);
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            List<Dataverse> dvsThisDvHasLinkedToList = dataverseSvc.findDataversesThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder dvsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThisDvHasLinkedToList) {
                dvsThisDvHasLinkedToBuilder.add(dataverse.getAlias());
            }

            List<Dataverse> dvsThatLinkToThisDvList = dataverseSvc.findDataversesThatLinkToThisDvId(dv.getId());
            JsonArrayBuilder dvsThatLinkToThisDvBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThatLinkToThisDvList) {
                dvsThatLinkToThisDvBuilder.add(dataverse.getAlias());
            }

            List<Dataset> datasetsThisDvHasLinkedToList = dataverseSvc.findDatasetsThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder datasetsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataset dataset : datasetsThisDvHasLinkedToList) {
                datasetsThisDvHasLinkedToBuilder.add(dataset.getLatestVersion().getTitle());
            }

            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("dataverses that the " + dv.getAlias() + " dataverse has linked to", dvsThisDvHasLinkedToBuilder);
            response.add("dataverses that link to the " + dv.getAlias(), dvsThatLinkToThisDvBuilder);
            response.add("datasets that the " + dv.getAlias() + " has linked to", datasetsThisDvHasLinkedToBuilder);
            return ok(response);

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

}
