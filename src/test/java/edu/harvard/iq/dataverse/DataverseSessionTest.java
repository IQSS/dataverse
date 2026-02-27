package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
