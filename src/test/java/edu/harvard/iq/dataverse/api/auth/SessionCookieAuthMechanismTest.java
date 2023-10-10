package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.auth.doubles.ContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@LocalJvmSettings
class SessionCookieAuthMechanismTest {

    private SessionCookieAuthMechanism sut;

    @BeforeEach
    public void setUp() {
        sut = new SessionCookieAuthMechanism();
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-session-auth")
    void testFindUserFromRequest_FeatureFlagDisabled() throws WrappedAuthErrorResponse {
        sut.session = Mockito.mock(DataverseSession.class);

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertNull(actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    void testFindUserFromRequest_FeatureFlagEnabled_UserAuthenticated() throws WrappedAuthErrorResponse {
        DataverseSession dataverseSessionStub = Mockito.mock(DataverseSession.class);
        User testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(dataverseSessionStub.getUser()).thenReturn(testAuthenticatedUser);
        sut.session = dataverseSessionStub;

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertEquals(testAuthenticatedUser, actual);
    }
}
