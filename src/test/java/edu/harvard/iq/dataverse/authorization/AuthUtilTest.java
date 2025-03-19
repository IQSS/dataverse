package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class AuthUtilTest {

    @Mock
    private SystemConfig systemConfig;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure the mock SystemConfig
        when(systemConfig.isSignupDisabledForRemoteAuthProvider(anyString())).thenReturn(false);
        when(systemConfig.isSignupDisabledForRemoteAuthProvider("orcid")).thenReturn(true);
    }
    
    @ParameterizedTest
    @CsvSource(value = {
        "NULL,NULL,NULL",
        "Homer,Homer,NULL",
        "Simpson,NULL,Simpson",
        "Homer Simpson,Homer,Simpson",
        "Homer Simpson,Homer,Simpson"
    }, nullValues = "NULL")
    void testGetDisplayName(String expectedDisplayName, String displayFirst, String displayLast) {
        assertEquals(expectedDisplayName, AuthUtil.getDisplayName(displayFirst, displayLast));
    }
    
    /**
     * Test of isNonLocalLoginEnabled method, of class AuthUtil.
     */
    @Test
    public void testIsNonLocalSignupEnabled() {
        System.out.println("isNonLocalSignupEnabled");
        
        assertFalse(AuthUtil.isNonLocalSignupEnabled(null, systemConfig));
        
        Collection<AuthenticationProvider> shibOnly = new HashSet<>();
        shibOnly.add(new ShibAuthenticationProvider());
        assertTrue(AuthUtil.isNonLocalSignupEnabled(shibOnly, systemConfig));
        

        Collection<AuthenticationProvider> orcidOnly = new HashSet<>();
        orcidOnly.add(new OrcidOAuth2AP(null, null, null));
        assertFalse(AuthUtil.isNonLocalSignupEnabled(orcidOnly, systemConfig));

        Collection<AuthenticationProvider> manyNonLocal = new HashSet<>();
        manyNonLocal.add(new ShibAuthenticationProvider());
        manyNonLocal.add(new GitHubOAuth2AP(null, null));
        manyNonLocal.add(new GoogleOAuth2AP(null, null));
        manyNonLocal.add(new OrcidOAuth2AP(null, null, null));
        assertTrue(AuthUtil.isNonLocalSignupEnabled(manyNonLocal, systemConfig));
        
        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null, null, null));
        // only builtin provider
        assertFalse(AuthUtil.isNonLocalSignupEnabled(onlyBuiltin, systemConfig));
    }

    @Test
    public void testIsSignupDisabledForRemoteAuthProvider() {
        assertTrue(systemConfig.isSignupDisabledForRemoteAuthProvider("orcid"));
        assertFalse(systemConfig.isSignupDisabledForRemoteAuthProvider("github"));
        assertFalse(systemConfig.isSignupDisabledForRemoteAuthProvider("google"));
    }
}