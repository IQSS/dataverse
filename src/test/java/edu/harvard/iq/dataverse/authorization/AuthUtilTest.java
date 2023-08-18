package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthUtilTest {
    
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
    public void testIsNonLocalLoginEnabled() {
        System.out.println("isNonLocalLoginEnabled");
        
        AuthUtil authUtil = new AuthUtil();
        
        assertFalse(AuthUtil.isNonLocalLoginEnabled(null));
        
        Collection<AuthenticationProvider> shibOnly = new HashSet<>();
        shibOnly.add(new ShibAuthenticationProvider());
        assertTrue(AuthUtil.isNonLocalLoginEnabled(shibOnly));
        
        Collection<AuthenticationProvider> manyNonLocal = new HashSet<>();
        manyNonLocal.add(new ShibAuthenticationProvider());
        manyNonLocal.add(new GitHubOAuth2AP(null, null));
        manyNonLocal.add(new GoogleOAuth2AP(null, null));
        manyNonLocal.add(new OrcidOAuth2AP(null, null, null));
        assertTrue(AuthUtil.isNonLocalLoginEnabled(manyNonLocal));
        
        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null, null, null));
        // only builtin provider
        assertFalse(AuthUtil.isNonLocalLoginEnabled(onlyBuiltin));
    }

}
