package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DataverseSessionTest {

    @Test
    void testCsrfTokenIsGeneratedAndReused() {
        DataverseSession session = new DataverseSession();

        String token1 = session.getOrCreateApiCsrfToken();
        String token2 = session.getOrCreateApiCsrfToken();

        assertEquals(token1, token2);
        assertTrue(session.matchesApiCsrfToken(token1));
    }

    @Test
    void testCsrfTokenCanBeCleared() {
        DataverseSession session = new DataverseSession();
        String token = session.getOrCreateApiCsrfToken();

        session.clearApiCsrfToken();

        assertFalse(session.matchesApiCsrfToken(token));
    }

    @Test
    void testCsrfTokenClearedWhenUserDemotedToGuest() {
        // getUser(true) demotes deleted/deactivated users to guest; the CSRF
        // token minted for the old identity must not survive the demotion.
        DataverseSession session = new DataverseSession();
        AuthenticatedUser au = new AuthenticatedUser();
        au.setId(7L);
        session.setUser(au);
        String token = session.getOrCreateApiCsrfToken();

        session.authenticationService = Mockito.mock(AuthenticationServiceBean.class);
        when(session.authenticationService.findByID(7L)).thenReturn(null); // deleted

        assertEquals(GuestUser.get(), session.getUser(true));
        assertFalse(session.matchesApiCsrfToken(token));
    }
}
