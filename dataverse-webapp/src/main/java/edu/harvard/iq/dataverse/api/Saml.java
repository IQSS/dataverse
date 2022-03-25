package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.api.dto.SamlIdentityProviderDTO;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlIdpManagementService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Either;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("saml")
public class Saml extends AbstractApiBean {
    private static final String SUPERUSER_REQUIRED_WARNING = "Only superuser is allowed to access the endpoint!";

    private SamlIdpManagementService idpManagementSerivce;

    // -------------------- CONSTRUCTORS --------------------

    public Saml() { }

    @Inject
    public Saml(SamlIdpManagementService idpManagementSerivce) {
        this.idpManagementSerivce = idpManagementSerivce;
    }

    // -------------------- LOGIC --------------------

    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllProviders() throws WrappedResponse {
        findSuperuserOrDie();
        return ok(idpManagementSerivce.listAll());
    }

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProvider(@PathParam("id") Long id) throws WrappedResponse {
        findSuperuserOrDie();
        Optional<SamlIdentityProviderDTO> provider = idpManagementSerivce.listSingle(id);
        return provider.isPresent()
                ? ok(provider.get())
                : notFound("Provider with id " + id + " was not found");
    }

    @Path("/")
    @POST
    @ApiWriteOperation
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(List<SamlIdentityProviderDTO> providers) throws WrappedResponse {
        findSuperuserOrDie();
        return providers != null
                ? handleResult(idpManagementSerivce.create(providers), "Provider(s) added")
                : badRequest("No providers data");
    }

    @Path("/")
    @PUT
    @ApiWriteOperation
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(SamlIdentityProviderDTO provider) throws WrappedResponse {
        findSuperuserOrDie();
        return provider != null
                ? handleResult(idpManagementSerivce.update(provider), "Provider updated")
                : badRequest("Incomplete or null input json");
    }

    @Path("/{id}")
    @DELETE
    @ApiWriteOperation
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) throws WrappedResponse {
        findSuperuserOrDie();
        return handleResult(idpManagementSerivce.delete(id), "Provider deleted");
    }

    // -------------------- PRIVATE --------------------

    private <T> Response handleResult(Either<String, T> result, String okMessage) {
        return result.isRight()
                ? ok(result.get(), okMessage)
                : badRequest(result.getLeft());
    }
}
