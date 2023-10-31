package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthenticationProviderTest {

    private static final String[] authProviders = {"null", "builtin", "github", "google", "orcid", "orcid-sandbox", "shib"};
    private static Map<String, String> bundleTestMap;

    @BeforeAll
    static void setup() {
        bundleTestMap = Stream.of(authProviders)
                .map(authProviderId -> new SimpleEntry<>(
                        authProviderId,
                        BundleUtil.getStringFromBundle("authenticationProvider.name." + authProviderId)
                    )
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    void testGetFriendlyName_withNullInput() {
        String expResult = bundleTestMap.get("null");
        assertEquals(expResult, AuthenticationProvider.getFriendlyName(null));
    }

    @Test
    void getFriendlyName_withoutMatchingId() {
        String idNotInBundle = "id-not-in-bundle-so-use-id";
        assertEquals(idNotInBundle, AuthenticationProvider.getFriendlyName(idNotInBundle));
    }

    @ParameterizedTest
    @ValueSource(strings = {"builtin", "github", "google", "orcid", "orcid-sandbox", "shib"})
    void testGetFriendlyName_withTestMap(String authProviderId) {
        String expectedResult = bundleTestMap.get(authProviderId);
        assertEquals(expectedResult, AuthenticationProvider.getFriendlyName(authProviderId));
    }
}
