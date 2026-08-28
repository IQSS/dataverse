package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.signing.FixedSigningSecret;
import jakarta.json.JsonObject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminRequestSignedUrlTest {

    private static final String SECRET = "test-only-signing-secret-0123456789";

    private Admin admin;
    private AuthenticatedUser superuser;
    private ContainerRequestContext crc;

    @BeforeEach
    void setUp() {
        admin = new Admin();
        admin.authSvc = mock(AuthenticationServiceBean.class);
        admin.signingSecretService = FixedSigningSecret.withSecret(SECRET);

        superuser = new AuthenticatedUser();
        superuser.setUserIdentifier("dataverseAdmin");
        superuser.setSuperuser(true);

        crc = mock(ContainerRequestContext.class);
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER)).thenReturn(superuser);
    }

    private JsonObject urlInfo(String url) {
        return JsonUtil.createObjectBuilder().add("url", url).build();
    }

    @Test
    void restrictedToSuperusers() {
        superuser.setSuperuser(false);

        Response response = admin.getSignedUrl(crc, urlInfo("http://localhost:8080/api/v1/datasets/1"));

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void missingUrlIsRejected() {
        Response response = admin.getSignedUrl(crc, JsonUtil.createObjectBuilder().build());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void urlWithReservedParameterIsRejectedNamingIt() {
        Response response = admin.getSignedUrl(crc, urlInfo("http://localhost:8080/api/v1/datasets/1?token=abc"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("token"));
    }

    @Test
    void missingApiTokenIsAConflict() {
        when(admin.authSvc.findApiTokenByUser(superuser)).thenReturn(null);

        Response response = admin.getSignedUrl(crc, urlInfo("http://localhost:8080/api/v1/datasets/1"));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }

    @Test
    void unknownUserFallsBackToTheSuperuserAndSignsWithSecretPlusToken() {
        ApiToken apiToken = new ApiToken();
        apiToken.setTokenString("superuser-token");
        when(admin.authSvc.getAuthenticatedUser("no-such-user")).thenReturn(null);
        when(admin.authSvc.findApiTokenByUser(superuser)).thenReturn(apiToken);

        Response response = admin.getSignedUrl(crc, JsonUtil.createObjectBuilder()
                .add("url", "http://localhost:8080/api/v1/datasets/1")
                .add("user", "no-such-user").build());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String entity = response.getEntity().toString();
        String signedUrl = entity.substring(entity.indexOf("http://"), entity.indexOf('"', entity.indexOf("http://")));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl, superuser.getUserIdentifier(), "GET", SECRET + "superuser-token"),
                "the URL must be signed for the superuser with secret + the superuser's token: " + signedUrl);
    }
}
