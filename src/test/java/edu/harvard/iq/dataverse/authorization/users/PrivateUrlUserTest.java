package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateUrlUserTest {

    private PrivateUrlUser privateUrlUser;

    @BeforeEach
    void setup() {
        privateUrlUser = new PrivateUrlUser(42L);
    }

    @Test
    void testGetDatasetId() {
        assertEquals(42L, privateUrlUser.getDatasetId());
    }

    @Test
    void testIsAuthenticated() {
        assertFalse(privateUrlUser.isAuthenticated());
    }

    @Test
    void testIsSuperuser() {
        assertFalse(privateUrlUser.isSuperuser());
    }

    @Test
    void getIdentifier() {
        assertEquals(PrivateUrlUser.PREFIX + 42L, privateUrlUser.getIdentifier());
    }

    @Test
    void testGetDisplayInfo() {
        RoleAssigneeDisplayInfo displayInfo = privateUrlUser.getDisplayInfo();
        assertEquals("Preview URL Enabled", displayInfo.getTitle());
        assertNull(displayInfo.getEmailAddress());
    }
}
