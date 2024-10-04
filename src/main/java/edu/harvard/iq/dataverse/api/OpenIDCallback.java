package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

import java.net.URI;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OIDCLoginBackingBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
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

    @EJB
    OIDCLoginBackingBean oidcLoginBackingBean;

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
        switch (openIdConfigBean.getTarget()) {
            case "JSF":
                return Response
                        .seeOther(URI.create(SystemConfig.getDataverseSiteUrlStatic() + "/oauth2/callback.xhtml"))
                        .build();
            case "SPA":
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("spa/")).build();
            case "API":
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("callback/session")).build();
            default:
                return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("spa/")).build();
        }
    }

    /**
     * Retrieve OIDC session and tokens (it is also where API target login redirects
     * to)
     * 
     * @param crc
     * @return
     */
    @Path("session")
    @GET
    public Response session(@Context ContainerRequestContext crc) {
        final String email = oidcLoginBackingBean.getVerifiedEmail();
        final AuthenticatedUser authUser = authSvc.getAuthenticatedUserByEmail(email);
        if (authUser != null) {
            oidcLoginBackingBean.storeBearerToken();
            return ok(
                    jsonObjectBuilder()
                            .add("user", authUser.toJson())
                            .add("session", crc.getCookies().get("JSESSIONID").getValue())
                            .add("accessToken", openIdContext.getAccessToken().getToken())
                            .add("identityToken", openIdContext.getIdentityToken().getToken()));
        } else {
            return notFound("user with email " + email + " not found");
        }
    }
}
