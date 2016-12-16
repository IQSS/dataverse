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
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null));
        // only builtin provider
        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(onlyBuiltin));

    }

    @Test
    public void testGetTip() {
        System.out.println("testGetTip");

        assertEquals(null, AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(null));

        Collection<AuthenticationProvider> onlyshib = new HashSet<>();
        onlyshib.add(new ShibAuthenticationProvider());
        assertEquals("Your Institution", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(onlyshib));

        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null));
        assertEquals(null, AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(onlyBuiltin));

        Collection<AuthenticationProvider> gitHubAndGoogle = new HashSet<>();
        gitHubAndGoogle.add(new GitHubOAuth2AP(null, null));
        gitHubAndGoogle.add(new GoogleOAuth2AP(null, null));
        assertEquals("GitHub or Google", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(gitHubAndGoogle));

        Collection<AuthenticationProvider> shibGitHubAndGoogle = new ArrayList<>();
        shibGitHubAndGoogle.add(new ShibAuthenticationProvider());
        shibGitHubAndGoogle.add(new GitHubOAuth2AP(null, null));
        shibGitHubAndGoogle.add(new GoogleOAuth2AP(null, null));
        assertEquals("Your Institution, GitHub, or Google", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(shibGitHubAndGoogle));

        Collection<AuthenticationProvider> gitHubGoogleAndOrcid = new ArrayList<>();
        gitHubGoogleAndOrcid.add(new ShibAuthenticationProvider());
        gitHubGoogleAndOrcid.add(new GitHubOAuth2AP(null, null));
        gitHubGoogleAndOrcid.add(new OrcidOAuth2AP(null, null, null));
        gitHubGoogleAndOrcid.add(new GoogleOAuth2AP(null, null));
        assertEquals("Your Institution, GitHub, ORCID, or Google", AuthUtil.getNamesOfRemoteAuthProvidersWithSeparators(gitHubGoogleAndOrcid));

    }

}
