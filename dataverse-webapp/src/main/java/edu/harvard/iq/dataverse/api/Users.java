package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.users.ChangeUserIdentifierService;

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

    @POST
    @ApiWriteOperation
    @Path("{identifier}/changeIdentifier/{newIdentifier}")
    public Response changeAuthenticatedUserIdentifier(@PathParam("identifier") String oldIdentifier, @PathParam("newIdentifier")  String newIdentifier) {
        try {
            User user;
            user = findUserOrDie();
            changeUserIdentifierService.changeUserIdentifier(user, oldIdentifier, newIdentifier);

            return ok("UserIdentifier changed from " + oldIdentifier + " to " + newIdentifier);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        } catch (EJBException ise) {
            return badRequest(ise.getCause().getMessage());
        } catch (Exception e){
            return error(Response.Status.BAD_REQUEST, "Error calling ChangeUserIdentifierCommand: " + e.getLocalizedMessage());
        }
    }

}
