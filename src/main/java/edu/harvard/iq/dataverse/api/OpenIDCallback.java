package edu.harvard.iq.dataverse.api;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import fish.payara.security.openid.api.OpenIdContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.json.JsonObjectBuilder;
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

	@Path("token")
	@GET
	public Response token(@Context ContainerRequestContext crc) {
		return Response.seeOther(crc.getUriInfo().getBaseUri().resolve("callback/session")).build();
	}

	@Path("session")
	@GET
	public Response session(@Context ContainerRequestContext crc) {
		final String email = openIdContext.getAccessToken().getJwtClaims().getStringClaim("email").orElse(null);
		final AuthenticatedUser authUser = authSvc.getAuthenticatedUserByEmail(email);
		final JsonObjectBuilder userJson;
        if (authUser != null) {
			userJson = authUser.toJson();
        } else {
			userJson = null;
		}
		return ok(
			jsonObjectBuilder()
			.add("user", userJson)
			.add("session", crc.getCookies().get("JSESSIONID").getValue())
			);
	}
}
