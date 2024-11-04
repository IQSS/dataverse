package edu.harvard.iq.dataverse.api.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.Optional;

public class AuthUtil {

    private static final String BEARER_AUTH_SCHEME = "Bearer";

    /**
     * Retrieve the raw, encoded token value from the Authorization Bearer HTTP header as defined in RFC 6750
     *
     * @return An {@link Optional} either empty if not present or the raw token from the header
     */
    public static Optional<String> getRequestBearerToken(ContainerRequestContext containerRequestContext) {
        String headerParamBearerToken = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (headerParamBearerToken != null && headerParamBearerToken.toLowerCase().startsWith(BEARER_AUTH_SCHEME.toLowerCase() + " ")) {
            return Optional.of(headerParamBearerToken);
        }
        return Optional.empty();
    }
}
