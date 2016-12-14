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
import org.junit.Ignore;

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

    @Test
    public void testGetTip() {
        System.out.println("testGetTip");

        // no shib, no providers
        assertEquals(null, AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(false, null));

        // yes shib, no providers
        assertEquals("Your Institution", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(true, null));

        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null));
        // no shib, only builtin provider
        assertEquals(null, AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(false, onlyBuiltin));

        Collection<AuthenticationProvider> gitHubAndGoogle = new HashSet<>();
        gitHubAndGoogle.add(new GitHubOAuth2AP(null, null));
        gitHubAndGoogle.add(new GoogleOAuth2AP(null, null));
        // yes shib, yes non local providers
        assertEquals("Your Institution, GitHub, or Google", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(true, gitHubAndGoogle));
        // no shib, yes non local providers
        assertEquals("GitHub or Google", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(false, gitHubAndGoogle));

        Collection<AuthenticationProvider> gitHubGoogleAndOrcid = new HashSet<>();
        gitHubGoogleAndOrcid.add(new GitHubOAuth2AP(null, null));
        gitHubGoogleAndOrcid.add(new GoogleOAuth2AP(null, null));
        gitHubGoogleAndOrcid.add(new OrcidOAuth2AP(null, null, null));
        assertEquals("Your Institution, GitHub, ORCID, or Google", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(true, gitHubGoogleAndOrcid));

    }

}
