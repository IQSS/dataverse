package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import fish.payara.security.openid.api.OpenIdContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("oidc")
public class OIDCSession extends AbstractApiBean {
    @Inject
    OpenIdContext openIdContext;

    @Inject
    protected AuthenticationServiceBean authSvc;

    /**
     * Retrieve OIDC session and tokens
     * 
     * @param crc
     * @return
     */
    @Path("session")
    @GET
    public Response session(@Context ContainerRequestContext crc) {
        final UserRecordIdentifier userRecordIdentifier = oidcLoginBackingBean.getUserRecordIdentifier();
        if (userRecordIdentifier == null) {
            return notFound("user record identifier not found");
        }
        final AuthenticatedUser authUser = authSvc.lookupUser(userRecordIdentifier);
        if (authUser != null) {
            try {
                return ok(
                        jsonObjectBuilder()
                                .add("user", JsonPrinter.json(authUser))
                                .add("session", crc.getCookies().get("JSESSIONID").getValue())
                                .add("accessToken", openIdContext.getAccessToken().getToken()));
            } catch (Exception e) {
                return badRequest(e.getMessage());
            }
        } else {
            return notFound("user with record identifier " + userRecordIdentifier + " not found");
        }
    }
}
