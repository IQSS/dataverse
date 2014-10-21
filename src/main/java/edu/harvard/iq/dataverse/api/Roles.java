package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteRoleCommand;
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
	@Path("{id}")
	public Response viewRole( @PathParam("id") Long id, @QueryParam("key") String key ) {
        try {
            User u = findUserOrDie(key);
            DataverseRole role = rolesSvc.find(id);
            if ( role == null ) {
                return notFound("role with id " + id + " not found");
            } else  {
                return ( permissionSvc.userOn(u, role.getOwner()).has(Permission.AssignRole) ) 
                    ? okResponse( json(role) )
                        : errorResponse(Status.UNAUTHORIZED, "");
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
	}
	
	@DELETE
	@Path("{id}")
	public Response deleteRole( @PathParam("id") Long id, @PathParam("key") String key ) {
		DataverseRole role = rolesSvc.find(id);
		if ( role == null ) {
			return notFound( "role with id " + id + " not found");
		} else  {
            try {
                execCommand( new DeleteRoleCommand(findUserOrDie(key), role), "Deleting role " + id );
                return okResponse("role " + id + " deleted.");
                
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
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
			
		} catch (WrappedResponse ex) {
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
		} catch ( WrappedResponse ce ) {
			return ce.getResponse();
		}
	}

}
