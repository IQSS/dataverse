package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class AuthUtilTest {

    @RunWith(Parameterized.class)
    public static class AuthUtilParamTests {

        @Parameters
        public static Collection<String[]> data() {
            return Arrays.asList(
                    new String[][] {
                        { null, null, null },
                        { "Homer", "Homer", null },
                        { "Simpson", null, "Simpson" },
                        { "Homer Simpson", "Homer", "Simpson" },
                        { "Homer Simpson", " Homer", "Simpson" }
                    }
                );
        }

        @Parameter
        public String expectedDisplayName;

        @Parameter(1)
        public String displayFirst;

        @Parameter(2)
        public String displayLast;

        @Test
        public void testGetDisplayName() {
            assertEquals(expectedDisplayName, AuthUtil.getDisplayName(displayFirst, displayLast));
        }
    }

    public static class AuthUtilNoParamTests {

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
    }

}
