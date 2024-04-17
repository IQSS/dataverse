package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 * @author michael
 */
public class AuthenticatedUserDisplayInfoTest {

    @Test
    public void testCopyConstructor() {
        AuthenticatedUserDisplayInfo src = new AuthenticatedUserDisplayInfo("fn", "ln",
                "email@address.com", "0000-0001-2345-6789", "Harvard", "https://ror.org/04k0tth05", "test");
        AuthenticatedUserDisplayInfo src2 = new AuthenticatedUserDisplayInfo("fn", "ln",
                "email@address.com", "0000-0001-2345-6789", "Harvard", "https://ror.org/04k0tth05", "test");
        assertEquals(src, src2);

        AuthenticatedUserDisplayInfo otherSrc = new AuthenticatedUserDisplayInfo("xfn", "ln",
                "email@address.com", "0000-0001-2345-6789", "Harvard", "https://ror.org/04k0tth05", "test");
        assertFalse(src.equals(otherSrc));
        final AuthenticatedUserDisplayInfo copyOfSrc = new AuthenticatedUserDisplayInfo(src);

        assertNotSame(src, copyOfSrc);
        assertEquals(src, copyOfSrc);
    }

}
