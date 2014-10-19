package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import javax.ejb.Stateless;
import javax.ws.rs.DELETE;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Util API for managing roles. Might not make it to the production version.
 * @author michael
 */
@Stateless
@Path("roles")
public class Roles extends AbstractApiBean {
	
	private static final Logger logger = Logger.getLogger(Roles.class.getName());
	
	@EJB
	BuiltinUserServiceBean usersSvc;
	
	@EJB
	DataverseServiceBean dvSvc;
	
	@GET
	public Response list() {
		JsonArrayBuilder rolesArrayBuilder = Json.createArrayBuilder();
		for ( DataverseRole role : rolesSvc.findAll() ) {
			rolesArrayBuilder.add(json(role));
		}
        
        return okResponse(rolesArrayBuilder);
	}
	
	@GET
	@Path("{id}")
	public Response viewRole( @PathParam("id") Long id ) {
		DataverseRole role = rolesSvc.find(id);
		if ( role == null ) {
			return notFound("role with id " + id + " not found");
		} else  {
			return okResponse( json(role) );
		}
	}
	
	@DELETE
	@Path("{id}")
	public Response deleteRole( @PathParam("id") Long id ) {
		DataverseRole role = rolesSvc.find(id);
		if ( role == null ) {
			return notFound( "role with id " + id + " not found");
		} else  {
			em.remove(role);
			return okResponse("role " + id + " deleted.");
		}
	}
	
	@POST
	@Path("assignments")
	public Response assignRole( @FormParam("username") String username, 
			@FormParam("roleId") long roleId, 
			@FormParam("definitionPointId") long dvObjectId,
			@QueryParam("key") String key ) {
		
        User issuer = findUserByApiToken(key);
		if ( issuer == null ) return errorResponse( Status.UNAUTHORIZED, "invalid api key '" + key +"'" );
		
        RoleAssignee ras = findAssignee(username);
		if ( ras == null ) return errorResponse( Status.BAD_REQUEST, "no user with username " + username );
		
        DvObject d = dvSvc.find( dvObjectId );
		if ( d == null ) return errorResponse( Status.BAD_REQUEST, "no DvObject with id " + dvObjectId );
		DataverseRole r = rolesSvc.find(roleId);
		if ( r == null ) return errorResponse( Status.BAD_REQUEST, "no role with id " + roleId );
		
		try {
			return okResponse( json(execCommand( new AssignRoleCommand(ras,r,d, issuer), "Assign Role")) );
			
		} catch (FailedCommandResult ex) {
			logger.log( Level.WARNING, "Error Assigning role", ex );
			return ex.getResponse();
		}
	}
	
	@POST
	public Response createNewRole( RoleDTO roleDto,
								 @QueryParam("dvo") String dvoIdtf,
								 @QueryParam("key") String key ) {
        
        User issuer = usersSvc.findByIdentifier(key);
		if ( issuer == null ) return errorResponse( Status.UNAUTHORIZED, "invalid api key '" + key +"'" );
		
		Dataverse d = findDataverse(dvoIdtf);
		if ( d == null ) return errorResponse( Status.BAD_REQUEST, "no dataverse with id " + dvoIdtf );
		
		try {
			return okResponse(json(execCommand(new CreateRoleCommand(roleDto.asRole(), issuer, d), "Create New Role")));
		} catch ( FailedCommandResult ce ) {
			return ce.getResponse();
		}
	}

}
