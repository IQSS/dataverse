package edu.harvard.iq.dataverse.api.auth;

import static edu.harvard.iq.dataverse.api.auth.BearerTokenAuthMechanism.BEARER_TOKEN_DETECTED_NO_OIDC_PROVIDER_CONFIGURED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.auth.doubles.BearerTokenKeyContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;

@LocalJvmSettings
@JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-bearer-auth")
class BearerTokenAuthMechanismTest {

    private static final String TEST_API_KEY = "test-api-key";

    private BearerTokenAuthMechanism sut;

    @BeforeEach
    public void setUp() {
        sut = new BearerTokenAuthMechanism();
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);
    }

    @Test
    void testFindUserFromRequest_no_token() throws WrappedAuthErrorResponse {
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake(null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
    void testFindUserFromRequest_no_OidcProvider() {
        Mockito.when(sut.authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Collections.emptySet());
        
        ContainerRequestContext testContainerRequest = new BearerTokenKeyContainerRequestTestFake("Bearer " +TEST_API_KEY);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        //then
        assertEquals(BEARER_TOKEN_DETECTED_NO_OIDC_PROVIDER_CONFIGURED, wrappedAuthErrorResponse.getMessage());
    }
}
