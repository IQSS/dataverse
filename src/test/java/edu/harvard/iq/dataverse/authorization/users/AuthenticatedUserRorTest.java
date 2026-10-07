package edu.harvard.iq.dataverse.authorization.users;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ROR normalization in AuthenticatedUserRor. Changing a user's RORs goes
 * through AuthenticationServiceBean and is covered by UsersIT.
 */
public class AuthenticatedUserRorTest {

    private static final String HARVARD = "https://ror.org/03vek6s52";
    private static final String UNC = "https://ror.org/0130frc33";

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
    public void testNormalizeRorIds() {
        assertEquals(Arrays.asList(UNC, HARVARD), AuthenticatedUserRor.normalizeRorIds(Arrays.asList("0130frc33", HARVARD)));
        assertEquals(Collections.emptyList(), AuthenticatedUserRor.normalizeRorIds(Collections.emptyList()));
    }

    @Test
    public void testNormalizeRorIdsRejectsInvalidAndDuplicates() {
        List<String> invalid = Arrays.asList(HARVARD, "not-a-ror");
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedUserRor.normalizeRorIds(invalid));
        // Duplicates are detected after normalization
        List<String> duplicates = Arrays.asList(HARVARD, "03vek6s52");
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedUserRor.normalizeRorIds(duplicates));
    }

    @Test
    public void testNewUserHasNoRors() {
        AuthenticatedUser user = new AuthenticatedUser();
        assertTrue(user.getRors().isEmpty());
        assertNull(user.getPrimaryRor());
        assertThrows(UnsupportedOperationException.class, () -> user.getRors().clear());
    }
}
