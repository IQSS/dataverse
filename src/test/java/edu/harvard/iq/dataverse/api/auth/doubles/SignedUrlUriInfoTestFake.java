package edu.harvard.iq.dataverse.api.auth.doubles;

import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static edu.harvard.iq.dataverse.util.UrlSignerUtil.SIGNED_URL_TOKEN;
import static edu.harvard.iq.dataverse.util.UrlSignerUtil.SIGNED_URL_USER;
import static jakarta.ws.rs.HttpMethod.GET;

public class SignedUrlUriInfoTestFake extends UriInfoTestFake {

    private final String signedUrlToken;
    private final String signedUrlUserId;
    private final String requestUriOverride;

    private static final String SIGNED_URL_BASE_URL = "http://localhost:8080/api/test1";
    private static final Integer SIGNED_URL_TIMEOUT = 1000;


    public SignedUrlUriInfoTestFake(String signedUrlToken, String signedUrlUserId) {
        this(signedUrlToken, signedUrlUserId, null);
    }

    // Lets a test supply the exact request URI the server would see, to exercise the real
    // URLDecoder.decode + isValidUrl validation path.
    public SignedUrlUriInfoTestFake(String signedUrlToken, String signedUrlUserId, String requestUriOverride) {
        this.signedUrlToken = signedUrlToken;
        this.signedUrlUserId = signedUrlUserId;
        this.requestUriOverride = requestUriOverride;
    }

    @Override
    public URI getRequestUri() {
        if (requestUriOverride != null) {
            return URI.create(requestUriOverride);
        }
        // Sign the way the server does: with the configured signing secret prepended to the token. This
        // keeps the fake consistent with SignedUrlAuthMechanism, which validates with secret + token.
        return URI.create(UrlSignerUtil.signUrlWithApiKey(SIGNED_URL_BASE_URL, SIGNED_URL_TIMEOUT, signedUrlUserId, GET, signedUrlToken));
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        if (requestUriOverride != null) {
            // Reflect the actual request URI (like Jersey would) so tests exercise real extraction of
            // the user/token params from the URL under test, not fabricated values.
            String query = URI.create(requestUriOverride).getRawQuery();
            if (query != null) {
                for (String pair : query.split("&")) {
                    int equals = pair.indexOf('=');
                    String name = (equals < 0) ? pair : pair.substring(0, equals);
                    String value = (equals < 0) ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
                    queryParameters.add(name, value);
                }
            }
            return queryParameters;
        }
        queryParameters.add(SIGNED_URL_TOKEN, signedUrlToken);
        queryParameters.add(SIGNED_URL_USER, signedUrlUserId);
        return queryParameters;
    }
}
