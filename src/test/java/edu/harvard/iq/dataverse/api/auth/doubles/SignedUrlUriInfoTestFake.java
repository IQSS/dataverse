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

    private static final String SIGNED_URL_BASE_URL = "http://localhost:8080/api/test1";
    private static final Integer SIGNED_URL_TIMEOUT = 1000;


    public SignedUrlUriInfoTestFake(String signedUrlToken, String signedUrlUserId) {
        this.signedUrlToken = signedUrlToken;
        this.signedUrlUserId = signedUrlUserId;
    }

    @Override
    public URI getRequestUri() {
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
