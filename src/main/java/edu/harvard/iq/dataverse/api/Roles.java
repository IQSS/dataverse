package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import javax.ejb.EJB;
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
	
	@EJB
	BuiltinUserServiceBean usersSvc;
	
	@EJB
	DataverseServiceBean dvSvc;
	
	@GET
	@Path("{id}")
	public Response viewRole( @PathParam("id") Long id) {
        try {
            DataverseRole role = rolesSvc.find(id);
            if ( role == null ) {
                return notFound("role with id " + id + " not found");
            } else  {
                return ( permissionSvc.userOn(findUserOrDie(), role.getOwner()).has(Permission.ManageDataversePermissions) ) 
                    ? okResponse( json(role) )
                        : errorResponse(Status.UNAUTHORIZED, "");
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
	}
	
	@DELETE
	@Path("{id}")
	public Response deleteRole( @PathParam("id") Long id ) {
		DataverseRole role = rolesSvc.find(id);
		if ( role == null ) {
			return notFound( "role with id " + id + " not found");
		} else  {
            try {
                execCommand( new DeleteRoleCommand(createDataverseRequest(findUserOrDie()), role) );
                return okResponse("role " + id + " deleted.");
                
            } catch (WrappedResponse ex) {
                return ex.refineResponse( "Cannot delete role " + id + "." );
            }
		}
	}
	
	@POST
	public Response createNewRole( RoleDTO roleDto,
								 @QueryParam("dvo") String dvoIdtf ) {
        
        Dataverse d = findDataverse(dvoIdtf);
		if ( d == null ) return errorResponse( Status.BAD_REQUEST, "no dataverse with id " + dvoIdtf );
		
		try {
			return okResponse(json(execCommand(new CreateRoleCommand(roleDto.asRole(), createDataverseRequest(findUserOrDie()), d))));
		} catch ( WrappedResponse ce ) {
			return ce.refineResponse("Role creation failed.");
		}
	}

}
