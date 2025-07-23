package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;

import java.util.Map;
import java.util.function.Function;

/**
 * A factory for creating {@link OAuthUserLookupParamsFactory} instances based on an identity provider.
 * This is a non-instantiable utility class.
 */
public final class OAuthUserLookupParamsFactory {

    /**
     * A map linking provider IDs to their corresponding user searcher constructor.
     */
    private static final Map<String, Function<String, OAuthUserLookupParams>> PROVIDER_MAP = Map.of(
            GoogleOAuth2AP.PROVIDER_ID, GoogleUserLookupParams::new,
            GitHubOAuth2AP.PROVIDER_ID, GitHubUserLookupParams::new,
            OrcidOAuth2AP.PROVIDER_ID, ORCIDUserLookupParams::new
    );

    private OAuthUserLookupParamsFactory() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates an instance of an {@link OAuthUserLookupParams} based on the identity provider claim.
     *
     * @param idpClaim The identity provider claim value (e.g., "https://accounts.google.com").
     * @param userId   The user identifier from the OAuth provider.
     * @return A new instance of a concrete {@link OAuthUserLookupParams}.
     * @throws IllegalArgumentException if the identity provider is not supported.
     */
    public static OAuthUserLookupParams getSearcher(String idpClaim, String userId) {
        return PROVIDER_MAP.keySet().stream()
                .filter(idpClaim::contains)
                .findFirst()
                .map(providerId -> PROVIDER_MAP.get(providerId).apply(userId))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + idpClaim));
    }
}
