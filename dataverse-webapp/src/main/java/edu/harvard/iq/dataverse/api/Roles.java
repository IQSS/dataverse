package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
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

/**
 * Util API for managing roles. Might not make it to the production version.
 * @author michael
 */
@Stateless
@Path("roles")
public class Roles extends AbstractApiBean {
	
	@GET
	@Path("{id}")
	public Response viewRole( @PathParam("id") Long id) {
        return response( ()-> {
            final User user = findUserOrDie(); 
            final DataverseRole role = findRoleOrDie(id);
            return ( permissionSvc.userOn(user, role.getOwner()).has(Permission.ManageDataversePermissions) ) 
                    ? ok( json(role) ) : permissionError("Permission required to view roles.");
        });
	}
	
	@DELETE
	@Path("{id}")
	public Response deleteRole( @PathParam("id") Long id ) {
        return response( req -> {
            execCommand( new DeleteRoleCommand(req, findRoleOrDie(id)) );
            return ok("role " + id + " deleted.");
        });
	}
	
	@POST
	public Response createNewRole( RoleDTO roleDto,
                                   @QueryParam("dvo") String dvoIdtf ) {
        return response( req -> ok(json(execCommand(
                                  new CreateRoleCommand(roleDto.asRole(),
                                                        req,findDataverseOrDie(dvoIdtf))))));
	}
    
    private DataverseRole findRoleOrDie( long id ) throws WrappedResponse {
        DataverseRole role = rolesSvc.find(id);
		if ( role != null ) {
            return role;
        }
		throw new WrappedResponse(notFound( "role with id " + id + " not found"));
    }
}
