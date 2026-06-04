package edu.harvard.iq.dataverse.api.auth.doubles;

import edu.harvard.iq.dataverse.util.UrlSignerUtil;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.net.URI;

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

    // Lets a test supply the exact request URI the server would see (e.g. a signed URL for a
    // specific base URL, or a reconstructed/escaped presentation), so the real validation path
    // (URLDecoder.decode + UrlSignerUtil.isValidUrl) can be exercised end to end.
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
        return URI.create(UrlSignerUtil.signUrl(SIGNED_URL_BASE_URL, SIGNED_URL_TIMEOUT, signedUrlUserId, GET, signedUrlToken));
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.add(SIGNED_URL_TOKEN, signedUrlToken);
        queryParameters.add(SIGNED_URL_USER, signedUrlUserId);
        return queryParameters;
    }
}
