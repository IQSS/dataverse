package edu.harvard.iq.dataverse.api.auth;

import java.util.Optional;

public class AuthUtil {

    private static final String BEARER_AUTH_SCHEME = "Bearer";

    /**
     * Extracts the Bearer token from the provided HTTP Authorization header value.
     * <p>
     * Validates that the header value starts with the "Bearer" scheme as defined in RFC 6750.
     * If the header is null, empty, or does not start with "Bearer ", an empty {@link Optional} is returned.
     *
     * @param headerParamBearerToken the raw HTTP Authorization header value containing the Bearer token
     * @return An {@link Optional} containing the raw Bearer token if present and valid; otherwise, an empty {@link Optional}
     */
    public static Optional<String> extractBearerTokenFromHeaderParam(String headerParamBearerToken) {
        if (headerParamBearerToken != null && headerParamBearerToken.toLowerCase().startsWith(BEARER_AUTH_SCHEME.toLowerCase() + " ")) {
            return Optional.of(headerParamBearerToken);
        }
        return Optional.empty();
    }
}
