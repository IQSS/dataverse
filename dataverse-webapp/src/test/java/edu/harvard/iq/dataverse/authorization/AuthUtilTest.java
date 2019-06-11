package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.ArrayList;
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

        AuthUtil authUtil = new AuthUtil();

        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(null));

        Collection<AuthenticationProvider> shibOnly = new HashSet<>();
        shibOnly.add(new ShibAuthenticationProvider());
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(shibOnly));

        Collection<AuthenticationProvider> manyNonLocal = new HashSet<>();
        manyNonLocal.add(new ShibAuthenticationProvider());
        manyNonLocal.add(new GitHubOAuth2AP(null, null));
        manyNonLocal.add(new GoogleOAuth2AP(null, null));
        manyNonLocal.add(new OrcidOAuth2AP(null, null, null));
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(manyNonLocal));

        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null, null, null));
        // only builtin provider
        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(onlyBuiltin));

    }

    @Test
    public void testGetDisplayName() {
        AuthUtil authUtil = new AuthUtil();
        assertEquals(null, AuthUtil.getDisplayName(null, null));
        assertEquals("Homer", AuthUtil.getDisplayName("Homer", null));
        assertEquals("Simpson", AuthUtil.getDisplayName(null, "Simpson"));
        assertEquals("Homer Simpson", AuthUtil.getDisplayName("Homer", "Simpson"));
        assertEquals("Homer Simpson", AuthUtil.getDisplayName(" Homer", "Simpson"));
    }

}
