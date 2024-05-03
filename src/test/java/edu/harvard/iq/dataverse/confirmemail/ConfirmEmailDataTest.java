package edu.harvard.iq.dataverse.confirmemail;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

public class ConfirmEmailDataTest {

    private ConfirmEmailData instance;
    private AuthenticatedUser user;

    @BeforeEach
    public void setUp() {
        this.user = new AuthenticatedUser();
        this.instance = new ConfirmEmailData(user, 60);
    }

    @AfterEach
    public void tearDown() {
        this.instance = null;
        this.user = null;
    }

    @Test
    public void testConfirmEmailDataNotNull() {
        assertTrue(instance != null);
    }

    @Test
    public void testTokenNotNull() {
        assertTrue(instance.getToken() != null);
    }

    @Test
    public void testTokenCreationTimestampNotNull () {
        assertTrue(instance.getCreated() != null);
    }

    @Test
    public void testTokenExpirationTimestampNotNull () {
        assertTrue(instance.getExpires() != null);
    }

    @Test
    public void testTokenNotExpired() {
        assertFalse(instance.isExpired());
    }

    @Test
    public void testAuthenticatedUserAssigned() {
        assertTrue(user == instance.getAuthenticatedUser());
    }

    @Test
    public void testIdAssigned() {
        long id = 42;
        instance.setId(id);
        assertTrue(42 == instance.getId());
    }

}
