package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import fish.payara.security.openid.api.OpenIdContext;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("callback")
public class OpenIDCallback extends AbstractApiBean {
    @Inject
    OpenIdContext openIdContext;

    @Inject
    protected AuthenticationServiceBean authSvc;

    @EJB
    OpenIDConfigBean openIdConfigBean;

    /**
     * Callback URL for the OIDC log in. It redirects to either JSF, SPA or API
     * after log in according to the target config.
     * 
     * @param crc
     * @return
     */
    @Path("token")
    @GET
    public Response token(@Context ContainerRequestContext crc) {
        /*final int emailVerified = openIdContext.getAccessToken().getJwtClaims().getIntClaim("email_verified")
                .orElse(0);
        if (emailVerified == 0) {
            openIdContext.logout(httpRequest, httpResponse);
        }*/
        switch (openIdConfigBean.getTarget()) {
            case "JSF":
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("oauth2/callback.xhtml")).build();
            case "SPA":
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("spa/")).build();
            case "API":
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("callback/session")).build();
            default:
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("spa/")).build();
        }
    }

    /**
     * Retrieve OIDC session and tokens (it is also where API target login redirects to)
     * 
     * @param crc
     * @return
     */
    @Path("session")
    @GET
    public Response session(@Context ContainerRequestContext crc) {
        try {
            final String email = openIdContext.getAccessToken().getJwtClaims().getStringClaim("email").orElse(null);
            final AuthenticatedUser authUser = authSvc.getAuthenticatedUserByEmail(email);
            if (authUser != null) {
                return ok(
                        jsonObjectBuilder()
                                .add("user", authUser.toJson())
                                .add("session", crc.getCookies().get("JSESSIONID").getValue())
                                .add("accessToken", openIdContext.getAccessToken().getToken())
                                .add("identityToken", openIdContext.getAccessToken().getToken())
                                );
            } else {
                return notFound("user with email " + email + " not found");
            }
        } catch (final Exception ignore) {
            return authenticatedUserRequired();
        }
    }
}
