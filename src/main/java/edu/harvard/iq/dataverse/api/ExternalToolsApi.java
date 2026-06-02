package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("externalTools")
@Tag(name = "External Tools", description = "External tool registration and lookup operations.")
public class ExternalToolsApi extends AbstractApiBean {

    @Inject
    ExternalTools externalTools;

    @GET
    @Operation(summary = "Lists external tools",
            description = "Returns all registered external tools.")
    public Response getExternalTools() {
        //ToDo - allow filtering by scope, tool type, file content type, etc?
        return externalTools.getExternalTools();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Returns an external tool",
            description = "Returns the registered external tool with the specified numeric id.")
    public Response getExternalTool(
            @Parameter(description = "Numeric id of the external tool to return.", required = true)
            @PathParam("id") long externalToolIdFromUser) {
        return externalTools.getExternalTool(externalToolIdFromUser);
    }

    @POST
    @AuthRequired
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Registers an external tool",
            description = "Registers an external tool from its manifest when the authenticated user is a superuser.")
    public Response addExternalTool(@Context ContainerRequestContext crc,
            @RequestBody(description = "External tool manifest JSON to register.")
            String manifest) {
        Response notAuthorized = authorize(crc);
        return notAuthorized == null ? externalTools.addExternalTool(manifest) : notAuthorized;
    }

    @DELETE
    @AuthRequired
    @Path("{id}")
    @SecurityRequirement(name = "DataverseApiKey")
    @Operation(summary = "Deletes an external tool",
            description = "Deletes the registered external tool with the specified numeric id when the authenticated user is a superuser.")
    public Response deleteExternalTool(@Context ContainerRequestContext crc,
            @Parameter(description = "Numeric id of the external tool to delete.", required = true)
            @PathParam("id") long externalToolIdFromUser) {
        Response notAuthorized = authorize(crc);
        return notAuthorized == null ? externalTools.deleteExternalTool(externalToolIdFromUser) : notAuthorized;
    }

    private Response authorize(ContainerRequestContext crc) {
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, "Superusers only.");
        }
        return null;
    }
}
