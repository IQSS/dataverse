package edu.harvard.iq.dataverse.authorization.users;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the ROR list logic on AuthenticatedUser and AuthenticatedUserRor.
 */
public class AuthenticatedUserRorTest {

    private static final String HARVARD = "https://ror.org/03vek6s52";
    private static final String UNC = "https://ror.org/0130frc33";
    private static final String MIT = "https://ror.org/042nb2s44";

    private AuthenticatedUser user;

    @BeforeEach
    public void setUp() {
        user = new AuthenticatedUser();
    }

    private List<String> rorIds() {
        return user.getRors().stream().map(AuthenticatedUserRor::getRorId).collect(Collectors.toList());
    }

    private void assertDisplayOrdersAreSequential() {
        List<AuthenticatedUserRor> rors = user.getRors();
        for (int i = 0; i < rors.size(); i++) {
            assertEquals(i, rors.get(i).getDisplayOrder());
        }
    }

    @Test
    public void testNormalizeRorId() {
        assertEquals(HARVARD, AuthenticatedUserRor.normalizeRorId("03vek6s52"));
        assertEquals(HARVARD, AuthenticatedUserRor.normalizeRorId(HARVARD));
        assertEquals(HARVARD, AuthenticatedUserRor.normalizeRorId("  https://ror.org/03VEK6S52/ "));
        assertNull(AuthenticatedUserRor.normalizeRorId(null));
        assertNull(AuthenticatedUserRor.normalizeRorId(""));
        assertNull(AuthenticatedUserRor.normalizeRorId("not-a-ror"));
        // ROR IDs always start with 0
        assertNull(AuthenticatedUserRor.normalizeRorId("13vek6s52"));
        assertNull(AuthenticatedUserRor.normalizeRorId("https://example.org/03vek6s52"));
    }

    @Test
    public void testNoRors() {
        assertTrue(user.getRors().isEmpty());
        assertNull(user.getPrimaryRor());
        assertNull(user.findRor(HARVARD));
    }

    @Test
    public void testAddRorAppendsAndInserts() {
        assertTrue(user.addRor("03vek6s52", null));
        assertTrue(user.addRor(UNC, null));
        assertEquals(Arrays.asList(HARVARD, UNC), rorIds());
        assertEquals(HARVARD, user.getPrimaryRor().getRorId());

        // Insert at position 0 to make it primary
        assertTrue(user.addRor(MIT, 0));
        assertEquals(Arrays.asList(MIT, HARVARD, UNC), rorIds());
        assertEquals(MIT, user.getPrimaryRor().getRorId());
        assertDisplayOrdersAreSequential();
        assertSame(user, user.getPrimaryRor().getAuthenticatedUser());
    }

    @Test
    public void testAddRorDuplicateIsIgnored() {
        assertTrue(user.addRor(HARVARD, null));
        assertFalse(user.addRor("03vek6s52", 0));
        assertEquals(Collections.singletonList(HARVARD), rorIds());
    }

    @Test
    public void testAddRorInvalid() {
        assertThrows(IllegalArgumentException.class, () -> user.addRor("not-a-ror", null));
        assertThrows(IllegalArgumentException.class, () -> user.addRor(HARVARD, 1));
        assertThrows(IllegalArgumentException.class, () -> user.addRor(HARVARD, -1));
        assertTrue(user.getRors().isEmpty());
    }

    @Test
    public void testRemoveRor() {
        user.setRors(Arrays.asList(HARVARD, UNC, MIT));
        assertTrue(user.removeRor("03vek6s52"));
        assertEquals(Arrays.asList(UNC, MIT), rorIds());
        assertEquals(UNC, user.getPrimaryRor().getRorId());
        assertDisplayOrdersAreSequential();

        assertFalse(user.removeRor(HARVARD));
        assertFalse(user.removeRor("not-a-ror"));
    }

    @Test
    public void testSetRorsReordersAndKeepsExistingInstances() {
        user.setRors(Arrays.asList(HARVARD, UNC));
        AuthenticatedUserRor harvard = user.findRor(HARVARD);

        user.setRors(Arrays.asList(MIT, "03vek6s52"));
        assertEquals(Arrays.asList(MIT, HARVARD), rorIds());
        assertDisplayOrdersAreSequential();
        // Existing rows are reused rather than deleted and recreated
        assertSame(harvard, user.findRor(HARVARD));
        assertNull(user.findRor(UNC));

        user.setRors(Collections.emptyList());
        assertTrue(user.getRors().isEmpty());
    }

    @Test
    public void testSetRorsRejectsInvalidAndDuplicates() {
        user.setRors(Arrays.asList(HARVARD));
        assertThrows(IllegalArgumentException.class, () -> user.setRors(Arrays.asList(UNC, "not-a-ror")));
        assertThrows(IllegalArgumentException.class, () -> user.setRors(Arrays.asList(UNC, "0130frc33")));
        // A rejected list leaves the existing RORs untouched
        assertEquals(Collections.singletonList(HARVARD), rorIds());
    }

    @Test
    public void testGetRorsIsReadOnly() {
        user.addRor(HARVARD, null);
        assertThrows(UnsupportedOperationException.class, () -> user.getRors().clear());
    }
}
