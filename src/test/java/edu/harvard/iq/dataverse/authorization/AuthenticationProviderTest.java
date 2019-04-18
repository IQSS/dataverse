package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthenticationProviderTest {

    private static Map<String, String> bundleTestMap;

    @BeforeAll
    static void setup() {
        /*
         * Create a map used to test known AuthenticationProvider ids
         * @author rmp553
         */
        bundleTestMap = Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>("builtin", "authenticationProvider.name.builtin"),
                new SimpleEntry<>("github", "authenticationProvider.name.github"),
                new SimpleEntry<>("google", "authenticationProvider.name.google"),
                new SimpleEntry<>("orcid", "authenticationProvider.name.orcid"),
                new SimpleEntry<>("orcid-sandbox", "authenticationProvider.name.orcid-sandbox"),
                new SimpleEntry<>("shib", "authenticationProvider.name.shib"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Test
    void testGetFriendlyName_withNullInput() {
        String expResult = BundleUtil.getStringFromBundle("authenticationProvider.name.null");
        assertEquals(expResult, AuthenticationProvider.getFriendlyName(null));
    }

    @Test
    void getFriendlyName_withoutMatchingId() {
        String idNotInBundle = "id-not-in-bundle-so-use-id";
        assertEquals(idNotInBundle, AuthenticationProvider.getFriendlyName(idNotInBundle));
    }

    @ParameterizedTest
    @CsvSource({
            "builtin, authenticationProvider.name.builtin",
            "github, authenticationProvider.name.github",
            "google, authenticationProvider.name.google",
            "orcid, authenticationProvider.name.orcid",
            "orcid-sandbox, authenticationProvider.name.orcid-sandbox",
            "shib, authenticationProvider.name.shib"
    })
    void testGetFriendlyName_withTestMap(String authProviderId, String bundleName) {
        String expectedResult = BundleUtil.getStringFromBundle(bundleName);
        assertEquals(expectedResult, AuthenticationProvider.getFriendlyName(authProviderId));
    }


    
}
