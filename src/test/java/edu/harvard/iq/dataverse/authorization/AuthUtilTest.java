package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.Collection;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthUtilTest {

    /**
     * Test of isNonLocalLoginEnabled method, of class AuthUtil.
     */
    @Test
    public void testIsNonLocalLoginEnabled() {
        System.out.println("isNonLocalLoginEnabled");

        // no shib, no providers!
        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(false, null));

        // yes shib, no providers
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(true, null));

        Collection<AuthenticationProvider> manyNonLocal = new HashSet<>();
        manyNonLocal.add(new ShibAuthenticationProvider());
        manyNonLocal.add(new GitHubOAuth2AP(null, null));
        manyNonLocal.add(new GoogleOAuth2AP(null, null));
        manyNonLocal.add(new OrcidOAuth2AP(null, null, null));
        // yes shib, yes non local providers
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(true, manyNonLocal));
        // no shib, yes non local providers
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(false, manyNonLocal));

        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null));
        // no shib, only builtin provider
        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(false, onlyBuiltin));

    }

}
