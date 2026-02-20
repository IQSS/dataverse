package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteRoleCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * Util API for managing roles. Might not make it to the production version.
 * @author michael
 */
@Stateless
@Path("roles")
public class Roles extends AbstractApiBean {
	
	@GET
    @AuthRequired
	@Path("{id}")
	public Response viewRole(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        return response( ()-> {
            final User user = getRequestUser(crc);
            final DataverseRole role = findRoleOrDie(id);
            return ( permissionSvc.userOn(user, role.getOwner()).has(Permission.ManageDataversePermissions) ) 
                    ? ok( json(role) ) : permissionError("Permission required to view roles.");
        });
	}
	
    @DELETE
    @AuthRequired
    @Path("{id}")
    public Response deleteRole(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        return response(req -> {
            DataverseRole role = findRoleOrDie(id);
            List<String> args = Arrays.asList(role.getName());
            if (role.getOwner() == null) {
                throw new WrappedResponse(forbidden(BundleUtil.getStringFromBundle("find.dataverse.role.error.role.builtin.not.allowed", args)));
            }
            execCommand(new DeleteRoleCommand(req, role));
            return ok("role " + role.getName() + " deleted.");
        }, getRequestUser(crc));
    }
	
	@POST
    @AuthRequired
	public Response createNewRole(@Context ContainerRequestContext crc,
                                  RoleDTO roleDto,
                                  @QueryParam("dvo") String dvoIdtf) {
        return response( req -> ok(json(execCommand(
                                  new CreateRoleCommand(roleDto.asRole(),
                                                        req,findDataverseOrDie(dvoIdtf))))), getRequestUser(crc));
	}

    @GET
    @AuthRequired
    @Path("userSelectable")
    public Response getUserSelectableRoles(@Context ContainerRequestContext crc) {
        return response(req -> ok(jsonDataverseRoles(roleAssigneeSvc.getSelectableDataverseRolesFor(req))), getRequestUser(crc));
    }
}
