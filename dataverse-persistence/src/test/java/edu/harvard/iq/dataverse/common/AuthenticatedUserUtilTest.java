package edu.harvard.iq.dataverse.common;

import org.junit.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class AuthenticatedUserUtilTest {

    /**
     * Create a map used to test known AuthenticationProvider ids
     *
     * @return
     */
    private Map<String, String> getBundleTestMap() {
        return Collections.unmodifiableMap(Stream.of(
                        new SimpleEntry<>("builtin", "authenticationProvider.name.builtin"),
                        new SimpleEntry<>("github", "authenticationProvider.name.github"),
                        new SimpleEntry<>("google", "authenticationProvider.name.google"),
                        new SimpleEntry<>("orcid", "authenticationProvider.name.orcid"),
                        new SimpleEntry<>("orcid-sandbox", "authenticationProvider.name.orcid-sandbox"),
                        new SimpleEntry<>("shib", "authenticationProvider.name.shib"))
                .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));
    }

    /**
     * Test of getAuthenticationProviderFriendlyName method, of class AuthenticationProvider.
     */
    @Test
    public void testGetFriendlyName() {
        System.out.println("getAuthenticationProviderFriendlyName");

        Map<String, String> bundleTestMap = this.getBundleTestMap();

        // ------------------------------------------
        // Test a null
        // ------------------------------------------
        String expResult = BundleUtil.getStringFromBundle("authenticationProvider.name.null");
        assertEquals(expResult, AuthenticatedUserUtil.getAuthenticationProviderFriendlyName(null));

        // ------------------------------------------
        // Test an id w/o a bundle entry--should default to id
        // ------------------------------------------
        String idNotInBundle = "id-not-in-bundle-so-use-id";
        String expResult2 = AuthenticatedUserUtil.getAuthenticationProviderFriendlyName(idNotInBundle);
        assertEquals(expResult2, idNotInBundle);

        // ------------------------------------------
        // Iterate through the map and test each item
        // ------------------------------------------
        bundleTestMap.forEach((authProviderId, bundleName) -> {
            String expectedResult = BundleUtil.getStringFromBundle(bundleName);
            assertEquals(expectedResult, AuthenticatedUserUtil.getAuthenticationProviderFriendlyName(authProviderId));
        });

    }
}
