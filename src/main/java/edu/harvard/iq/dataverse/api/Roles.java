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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Util API for managing roles. Might not make it to the production version.
 * @author michael
 */
@Stateless
@Path("roles")
@Tag(name = "Roles", description = "Role definition and role selection operations.")
public class Roles extends AbstractApiBean {
	
	@GET
    @AuthRequired
	@Path("{id}")
    @Operation(summary = "Returns a role",
            description = "Returns a role definition when the authenticated user can manage permissions on the role owner.")
	public Response viewRole(@Context ContainerRequestContext crc,
            @Parameter(description = "Role id or alias to return.", required = true)
            @PathParam("id") String id) {
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
    @Operation(summary = "Deletes a role",
            description = "Deletes a non-builtin role definition from its owner dataverse.")
    public Response deleteRole(@Context ContainerRequestContext crc,
            @Parameter(description = "Role id or alias to delete.", required = true)
            @PathParam("id") String id) {
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
    @Operation(summary = "Creates a role",
            description = "Creates a role definition in the specified dataverse and returns the saved role.")
	public Response createNewRole(@Context ContainerRequestContext crc,
                                  @RequestBody(description = "Role definition containing alias, name, permissions, and role metadata.")
                                  RoleDTO roleDto,
                                  @Parameter(description = "Dataverse identifier where the role is created.", required = true)
                                  @QueryParam("dvo") String dvoIdtf) {
        return response( req -> ok(json(execCommand(
                                  new CreateRoleCommand(roleDto.asRole(),
                                                        req,findDataverseOrDie(dvoIdtf))))), getRequestUser(crc));
	}

    @GET
    @AuthRequired
    @Path("userSelectable")
    @Operation(summary = "Lists selectable roles",
            description = "Returns dataverse roles that the authenticated requester can select for role assignment.")
    public Response getUserSelectableRoles(@Context ContainerRequestContext crc) {
        return response(req -> ok(jsonDataverseRoles(roleAssigneeSvc.getSelectableDataverseRolesFor(req))), getRequestUser(crc));
    }
}
