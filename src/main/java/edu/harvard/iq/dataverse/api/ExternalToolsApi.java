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

@Path("externalTools")
public class ExternalToolsApi extends AbstractApiBean {

    @Inject
    ExternalTools externalTools;

    @GET
    public Response getExternalTools() {
        return externalTools.getExternalTools();
    }

    @GET
    @Path("{id}")
    public Response getExternalTool(@PathParam("id") long externalToolIdFromUser) {
        return externalTools.getExternalTool(externalToolIdFromUser);
    }

    @POST
    @AuthRequired
    public Response addExternalTool(@Context ContainerRequestContext crc, String manifest) {
        Response notAuthorized = authorize(crc);
        return notAuthorized == null ? externalTools.addExternalTool(manifest) : notAuthorized;
    }

    @DELETE
    @AuthRequired
    @Path("{id}")
    public Response deleteExternalTool(@Context ContainerRequestContext crc, @PathParam("id") long externalToolIdFromUser) {
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
