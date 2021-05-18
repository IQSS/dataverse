package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.users.ChangeUserIdentifierService;
import edu.harvard.iq.dataverse.users.MergeInAccountService;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 *
 * @author madunlap
 */
@Path("users")
public class Users extends AbstractApiBean {

    @Inject
    private ChangeUserIdentifierService changeUserIdentifierService;

    @Inject
    private MergeInAccountService mergeInAccountService;

    @POST
    @ApiWriteOperation
    @Path("{consumedIdentifier}/mergeIntoUser/{baseIdentifier}")
    public Response mergeInAuthenticatedUser(@PathParam("consumedIdentifier") String consumedIdentifier, @PathParam("baseIdentifier") String baseIdentifier) {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
            mergeInAccountService.mergeAccounts(consumedIdentifier, baseIdentifier);

            return ok("All account data for " + consumedIdentifier + " has been merged into " + baseIdentifier + ".");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        } catch (EJBException ise) {
            return badRequest(ise.getCause().getMessage());
        } catch (Exception e){
            return error(Response.Status.BAD_REQUEST, "Error calling MergeInAccountService: " + e.getLocalizedMessage());
        }
    }

    @POST
    @ApiWriteOperation
    @Path("{identifier}/changeIdentifier/{newIdentifier}")
    public Response changeAuthenticatedUserIdentifier(@PathParam("identifier") String oldIdentifier, @PathParam("newIdentifier")  String newIdentifier) {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
            changeUserIdentifierService.changeUserIdentifier(oldIdentifier, newIdentifier);

            return ok("UserIdentifier changed from " + oldIdentifier + " to " + newIdentifier);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        } catch (EJBException ise) {
            return badRequest(ise.getCause().getMessage());
        } catch (Exception e){
            return error(Response.Status.BAD_REQUEST, "Error calling ChangeUserIdentifierService: " + e.getLocalizedMessage());
        }
    }

}
