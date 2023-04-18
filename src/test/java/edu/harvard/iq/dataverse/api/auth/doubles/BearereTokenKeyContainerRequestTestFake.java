package edu.harvard.iq.dataverse.api.auth.doubles;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import static edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism.DATAVERSE_API_KEY_REQUEST_HEADER_NAME;

public class BearereTokenKeyContainerRequestTestFake extends ContainerRequestTestFake {

    private final String apiKey;

    public BearereTokenKeyContainerRequestTestFake(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getHeaderString(String s) {
        if (s.equals(HttpHeaders.AUTHORIZATION)) {
            return this.apiKey;
        }
        return null;
    }
}
